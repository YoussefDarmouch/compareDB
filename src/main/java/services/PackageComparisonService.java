package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares Oracle packages between two databases.
 *
 * On Oracle, package comparison uses {@code USER_SOURCE} with {@code TYPE = 'PACKAGE BODY'}
 * only (see {@link DatabaseFunctionService#getPackageBodySource}). If the body object is
 * missing, it falls back to merged spec+body text after the {@code -- PACKAGE BODY --}
 * marker when present. Line counts and diffs never include the package spec when the
 * body is available from the catalog.
 *
 * Each modified body is compared at two levels:
 *   1. Line-by-line diffs (whitespace / blank-line insensitive after preprocessing)
 *   2. Sub-programs – each PROCEDURE/FUNCTION block is tracked individually
 */
public final class PackageComparisonService {

    private PackageComparisonService() {}

    public static List<DbObjectDiff> compare(
            DbConnectionFactory.DbConfig db1,
            DbConnectionFactory.DbConfig db2) {

        Set<String> names1 = DatabaseFunctionService.getObjectNames(db1, DbObjectDiff.ObjectType.PACKAGE);
        Set<String> names2 = DatabaseFunctionService.getObjectNames(db2, DbObjectDiff.ObjectType.PACKAGE);

        Set<String> all = new TreeSet<String>(names1);
        all.addAll(names2);

        List<DbObjectDiff> result = new ArrayList<DbObjectDiff>();

        for (String name : all) {
            boolean in1 = names1.contains(name);
            boolean in2 = names2.contains(name);

            if (in1 && !in2) {
                result.add(buildAdded(name, db1));
                continue;
            }

            if (!in1 && in2) {
                result.add(buildRemoved(name, db2));
                continue;
            }

            // present in both – compare
            DbObjectDiff modified = buildModified(name, db1, db2);
            if (modified != null) result.add(modified);
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // ADDED  (only in DB1)
    // -------------------------------------------------------------------------

    private static DbObjectDiff buildAdded(String name, DbConnectionFactory.DbConfig db1) {
        String src = resolvePackageBodyForCompare(db1, name);
        List<String> lines = ComparisonHelper.split(src);

        DbObjectDiff diff = new DbObjectDiff(name, DbObjectDiff.ObjectType.PACKAGE, DbObjectDiff.ChangeType.ADDED);
        diff.setDb1SourceLines(lines.size());

        // record source lines so ExportExcel can use them
        for (int i = 0; i < lines.size(); i++) {
            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(i + 1, null, lines.get(i)));
        }

        // list every procedure/function in the package
        for (SubProgramEntry e : extractSubPrograms(lines)) {
            diff.getSubProgramDiffs().add(
                    new DbObjectDiff.SubProgramDiff(e.name, e.type, "PRESENT",
                            e.startLine, e.endLine, null));
        }
        return diff;
    }

    // -------------------------------------------------------------------------
    // REMOVED  (only in DB2)
    // -------------------------------------------------------------------------

    private static DbObjectDiff buildRemoved(String name, DbConnectionFactory.DbConfig db2) {
        String src = resolvePackageBodyForCompare(db2, name);
        List<String> lines = ComparisonHelper.split(src);

        DbObjectDiff diff = new DbObjectDiff(name, DbObjectDiff.ObjectType.PACKAGE, DbObjectDiff.ChangeType.REMOVED);
        diff.setDb2SourceLines(lines.size());

        for (int i = 0; i < lines.size(); i++) {
            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(i + 1, lines.get(i), null));
        }

        for (SubProgramEntry e : extractSubPrograms(lines)) {
            diff.getSubProgramDiffs().add(
                    new DbObjectDiff.SubProgramDiff(e.name, e.type, "PRESENT",
                            e.startLine, e.endLine, null));
        }
        return diff;
    }

    // -------------------------------------------------------------------------
    // MODIFIED  (present in both, source differs)
    // -------------------------------------------------------------------------

    private static DbObjectDiff buildModified(String name,
                                              DbConnectionFactory.DbConfig db1,
                                              DbConnectionFactory.DbConfig db2) {
        String body1 = resolvePackageBodyForCompare(db1, name);
        String body2 = resolvePackageBodyForCompare(db2, name);

        SourcePreprocessor.ProcessedWithRawLines pw1 = SourcePreprocessor.processWithRawLines(body1);
        SourcePreprocessor.ProcessedWithRawLines pw2 = SourcePreprocessor.processWithRawLines(body2);

        SourceDiffEngine.DiffResult diffResult = SourceDiffEngine.diff(pw1.lines, pw2.lines);

        if (diffResult.isIdentical()) {
            return null; // identical body after preprocessing
        }

        DbObjectDiff diff = new DbObjectDiff(name, DbObjectDiff.ObjectType.PACKAGE, DbObjectDiff.ChangeType.MODIFIED);
        diff.setSimilarity(diffResult.getSimilarity());

        for (SourceDiffEngine.DiffEntry entry : diffResult.getEntries()) {
            if (entry.getType() == SourceDiffEngine.DiffType.EQUAL) continue;

            int lineNum = mapToRawLineNumber(entry, pw1, pw2);
            String charSummary = entry.charChangeSummary();

            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(
                    lineNum,
                    entry.getOldLine(),
                    entry.getNewLine(),
                    entry.getType().name(),
                    charSummary.isEmpty() ? null : charSummary));
        }

        List<String> raw1 = ComparisonHelper.split(body1);
        List<String> raw2 = ComparisonHelper.split(body2);
        diff.setDb1SourceLines(raw1.size());
        diff.setDb2SourceLines(raw2.size());

        // Sub-program ranges and changed lines are body-relative (same strings as diff input)
        buildSubProgramDiffs(diff, raw1, raw2);

        return diff;
    }

    /** Map LCS line index to 1-based raw source line (DB1 for old / MODIFIED, DB2 for insert-only). */
    private static int mapToRawLineNumber(SourceDiffEngine.DiffEntry entry,
                                          SourcePreprocessor.ProcessedWithRawLines pw1,
                                          SourcePreprocessor.ProcessedWithRawLines pw2) {
        if (entry.getOldLineNum() > 0) {
            return pw1.rawLineNumbers1Based.get(entry.getOldLineNum() - 1);
        }
        return pw2.rawLineNumbers1Based.get(entry.getNewLineNum() - 1);
    }

    // -------------------------------------------------------------------------
    // Sub-program diff
    // -------------------------------------------------------------------------

    private static void buildSubProgramDiffs(DbObjectDiff diff,
                                             List<String> oldLines,
                                             List<String> newLines) {

        List<SubProgramEntry> entries1 = extractSubPrograms(oldLines);
        List<SubProgramEntry> entries2 = extractSubPrograms(newLines);

        // Lookup by type:name for DB2
        Map<String, SubProgramEntry> map2 = new LinkedHashMap<String, SubProgramEntry>();
        for (SubProgramEntry e : entries2) {
            map2.put(e.type + ":" + e.name, e);
        }

        // Global set of changed line numbers in DB1
        Set<Integer> changedLines1 = new HashSet<Integer>();
        for (DbObjectDiff.SourceDiffLine dl : diff.getSourceDiffs()) {
            changedLines1.add(dl.getLineNumber());
        }

        for (SubProgramEntry e1 : entries1) {
            SubProgramEntry e2 = map2.get(e1.type + ":" + e1.name);

            // Collect changed lines that fall within this sub-program's range in DB1
            List<Integer> localChanged = new ArrayList<Integer>();
            for (int ln : changedLines1) {
                if (ln >= e1.startLine && ln <= e1.endLine) {
                    localChanged.add(ln);
                }
            }
            Collections.sort(localChanged);

            boolean changed = !localChanged.isEmpty();

            // Cross-check by comparing the raw blocks when both sides exist
            if (e2 != null && !changed) {
                String block1 = joinLines(oldLines, e1.startLine, e1.endLine);
                String block2 = joinLines(newLines, e2.startLine, e2.endLine);
                changed = !normalizedPackageLine(block1).equals(normalizedPackageLine(block2));
            }

            diff.getSubProgramDiffs().add(new DbObjectDiff.SubProgramDiff(
                    e1.name, e1.type,
                    changed ? "DIFFERENT" : "SAME",
                    e1.startLine, e1.endLine,
                    localChanged));
        }

        // Sub-programs that exist only in DB2
        for (SubProgramEntry e2 : entries2) {
            boolean foundInDb1 = false;
            for (SubProgramEntry e1 : entries1) {
                if (e1.name.equals(e2.name) && e1.type.equals(e2.type)) {
                    foundInDb1 = true;
                    break;
                }
            }
            if (!foundInDb1) {
                diff.getSubProgramDiffs().add(new DbObjectDiff.SubProgramDiff(
                        e2.name, e2.type, "DIFFERENT",
                        e2.startLine, e2.endLine, null));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Sub-program entry extraction
    // -------------------------------------------------------------------------

    private static final Pattern SUB_PROGRAM_PATTERN = Pattern.compile(
            "^\\s*(?:MEMBER\\s+)?(PROCEDURE|FUNCTION)\\s+(\\w+)\\s*(?:\\(|\\bIS\\b|\\bAS\\b|$)",
            Pattern.CASE_INSENSITIVE);

    private static List<SubProgramEntry> extractSubPrograms(List<String> lines) {
        List<SubProgramEntry> all = new ArrayList<SubProgramEntry>();

        // Find where the PACKAGE BODY starts (spec and body are merged with a marker)
        int bodyStart = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equalsIgnoreCase("-- PACKAGE BODY --")) {
                bodyStart = i + 1; // 0-based index of first body line
                break;
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            Matcher m = SUB_PROGRAM_PATTERN.matcher(lines.get(i));
            if (m.find()) {
                SubProgramEntry entry = new SubProgramEntry(
                        m.group(2).toUpperCase(),
                        m.group(1).toUpperCase(),
                        i + 1);
                entry.inBody = (bodyStart >= 0 && i >= bodyStart);
                all.add(entry);
            }
        }

        // Fill end-line boundary for each entry
        for (int i = 0; i < all.size() - 1; i++) {
            all.get(i).endLine = all.get(i + 1).startLine - 1;
        }
        if (!all.isEmpty()) {
            all.get(all.size() - 1).endLine = lines.size();
        }

        // Deduplicate: if a sub-program appears in both spec and body, keep only the body version
        Map<String, SubProgramEntry> seen = new LinkedHashMap<String, SubProgramEntry>();
        for (SubProgramEntry e : all) {
            String key = e.type + ":" + e.name;
            SubProgramEntry existing = seen.get(key);
            if (existing == null) {
                seen.put(key, e);
            } else if (e.inBody && !existing.inBody) {
                // Prefer body over spec
                seen.put(key, e);
            }
            // else keep existing (body already stored, or both in same section)
        }

        return new ArrayList<SubProgramEntry>(seen.values());
    }

    private static final class SubProgramEntry {
        final String name;
        final String type; // "PROCEDURE" or "FUNCTION"
        final int startLine;
        int endLine;
        boolean inBody;

        SubProgramEntry(String name, String type, int startLine) {
            this.name = name;
            this.type = type;
            this.startLine = startLine;
            this.endLine = startLine;
            this.inBody = false;
        }
    }

    // -------------------------------------------------------------------------
    // Text helpers
    // -------------------------------------------------------------------------

    /**
     * Body text used for all package comparisons: direct {@code PACKAGE BODY} from
     * Oracle when available, otherwise the body slice of merged catalog source.
     */
    private static String resolvePackageBodyForCompare(DbConnectionFactory.DbConfig db, String name) {
        String direct = DatabaseFunctionService.getPackageBodySource(db, name);
        if (direct != null && !direct.trim().isEmpty()) {
            return direct;
        }
        String merged = DatabaseFunctionService.getObjectSource(db, DbObjectDiff.ObjectType.PACKAGE, name);
        return extractPackageBodySource(merged != null ? merged : "");
    }

    /**
     * Keeps only {@code PACKAGE BODY} source. Merged Oracle text uses a line
     * {@code -- PACKAGE BODY --} between spec and body; everything after that
     * line is returned (body line numbers then start at 1). If the marker is
     * missing, returns {@code mergedSource} unchanged (e.g. body-only retrieval).
     */
    private static String extractPackageBodySource(String mergedSource) {
        if (mergedSource == null || mergedSource.isEmpty()) {
            return "";
        }
        List<String> lines = ComparisonHelper.split(mergedSource);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equalsIgnoreCase("-- PACKAGE BODY --")) {
                StringBuilder sb = new StringBuilder();
                for (int j = i + 1; j < lines.size(); j++) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(lines.get(j));
                }
                return sb.toString();
            }
        }
        return mergedSource;
    }

    /** Strips all whitespace and upper-cases – used for package source comparison. */
    private static String normalizedPackageLine(String v) {
        if (v == null) return "";
        return v.replaceAll("\\s+", "").toUpperCase();
    }

    private static String joinLines(List<String> lines, int start, int end) {
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, start - 1);
        int to   = Math.min(lines.size(), end);
        for (int i = from; i < to; i++) {
            sb.append(lines.get(i)).append("\n");
        }
        return sb.toString();
    }
}