package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PackageComparisonService {

    private PackageComparisonService() {}

    // =========================================================================
    // SUB-PROGRAM DESCRIPTOR  (NEW)
    // =========================================================================

    /**
     * Lightweight descriptor that carries both the kind (PROCEDURE / FUNCTION)
     * and the normalised name of a sub-program.
     */
    public static final class SubProgramDescriptor {
        /** "PROCEDURE" or "FUNCTION" — always uppercase. */
        public final String type;
        /** Identifier, always uppercase, quotes stripped. */
        public final String name;

        public SubProgramDescriptor(String type, String name) {
            this.type = type.toUpperCase();
            this.name = name.toUpperCase().replace("\"", "").trim();
        }

        /** Canonical key used for map look-ups: e.g. "FUNCTION:FN_SALARY". */
        public String key() { return type + ":" + name; }

        @Override public String toString() { return type + " " + name; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SubProgramDescriptor)) return false;
            SubProgramDescriptor d = (SubProgramDescriptor) o;
            return type.equals(d.type) && name.equals(d.name);
        }

        @Override public int hashCode() { return key().hashCode(); }
    }

    // =========================================================================
    // PUBLIC API: extract sub-programs from a package body string  (NEW / ENHANCED)
    // =========================================================================

    /**
     * Regex that matches both PROCEDURE and FUNCTION declarations.
     *
     * Handles:
     *   - Optional MEMBER prefix (object types)
     *   - Quoted identifiers ("MY PROC")
     *   - Unquoted identifiers
     *   - All valid terminators: '(' | IS | AS | end-of-line
     *
     * Group 1 → type  ("PROCEDURE" | "FUNCTION")
     * Group 2 → name  (quoted or unquoted)
     */
    private static final Pattern SUB_PROGRAM_DESCRIPTOR_PATTERN = Pattern.compile(
            "^\\s*(?:MEMBER\\s+)?(PROCEDURE|FUNCTION)" +
            "\\s+(\"[^\"]+\"|[A-Za-z][A-Za-z0-9_$#]*)" +
            "\\s*(?:\\(|\\bIS\\b|\\bAS\\b|$)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    /**
     * Extract every PROCEDURE and FUNCTION declared in {@code packageBody}.
     *
     * <ul>
     *   <li>Strips all PL/SQL comments before scanning.</li>
     *   <li>Returns descriptors in alphabetical order, deduplicated.</li>
     *   <li>Names and types are normalised to uppercase.</li>
     * </ul>
     *
     * @param packageBody full PACKAGE BODY source (may be null / empty)
     * @return ordered, deduplicated set of {@link SubProgramDescriptor}
     */
    public static Set<SubProgramDescriptor> extractSubProgramDescriptors(String packageBody) {
        // TreeSet with natural key ordering for deterministic output
        Set<SubProgramDescriptor> result = new TreeSet<>(
                (a, b) -> a.key().compareTo(b.key()));

        if (packageBody == null || packageBody.trim().isEmpty()) return result;

        String clean = stripPlSqlComments(packageBody);
        Matcher m = SUB_PROGRAM_DESCRIPTOR_PATTERN.matcher(clean);
        while (m.find()) {
            result.add(new SubProgramDescriptor(m.group(1), m.group(2)));
        }
        return result;
    }

    /**
     * Convenience overload — returns only names (uppercase), for callers that
     * do not need the type distinction.
     *
     * @param packageBody full PACKAGE BODY source
     * @return ordered set of normalised sub-program names
     */
    public static Set<String> extractSubPrograms(String packageBody) {
        Set<String> names = new TreeSet<>();
        for (SubProgramDescriptor d : extractSubProgramDescriptors(packageBody)) {
            names.add(d.name);
        }
        return names;
    }

    // =========================================================================
    // MISSING SUB-PROGRAM COMPARISON  (NEW)
    // =========================================================================

    /**
     * Compares the sub-programs present in two package body sources and
     * injects {@link DbObjectDiff.SubProgramDiff} entries into {@code diff}
     * for every sub-program that is absent from one side.
     *
     * Status values used:
     * <ul>
     *   <li>{@code MISSING_IN_D2} — sub-program exists in d1 but not in d2</li>
     *   <li>{@code MISSING_IN_D1} — sub-program exists in d2 but not in d1</li>
     * </ul>
     *
     * @param diff   the diff object to enrich (sub-program diffs are appended)
     * @param body1  full PACKAGE BODY source from database 1 (d1)
     * @param body2  full PACKAGE BODY source from database 2 (d2)
     */
    public static void detectMissingSubPrograms(DbObjectDiff diff,
                                                String body1,
                                                String body2) {
        // Build maps keyed by "TYPE:NAME"
        Map<String, SubProgramDescriptor> map1 = buildDescriptorMap(body1);
        Map<String, SubProgramDescriptor> map2 = buildDescriptorMap(body2);

        // Union of all keys, sorted for deterministic output
        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(map1.keySet());
        allKeys.addAll(map2.keySet());

        for (String key : allKeys) {
            boolean inD1 = map1.containsKey(key);
            boolean inD2 = map2.containsKey(key);

            if (inD1 && !inD2) {
                SubProgramDescriptor d = map1.get(key);
                // Add to SubProgramDiffs
                diff.getSubProgramDiffs().add(new DbObjectDiff.SubProgramDiff(
                        d.name, d.type, "MISSING_IN_D2",
                        0, 0, null));
                // Also surface in SourceDiffs so it appears in every tabular view
                diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(
                        0,
                        d.type + " " + d.name,   // old side — exists in d1
                        null,                     // new side — absent in d2
                        "REMOVED",
                        "Missing in D2"));

            } else if (!inD1 && inD2) {
                SubProgramDescriptor d = map2.get(key);
                diff.getSubProgramDiffs().add(new DbObjectDiff.SubProgramDiff(
                        d.name, d.type, "MISSING_IN_D1",
                        0, 0, null));
                diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(
                        0,
                        null,                     // old side — absent in d1
                        d.type + " " + d.name,   // new side — exists in d2
                        "ADDED",
                        "Missing in D1"));
            }
            // Both sides present → handled by buildSubProgramDiffs / buildModified
        }
    }

    /**
     * Returns a human-readable summary of missing sub-programs, suitable for
     * a "DETAILS" column or log output.
     *
     * Example output:
     * <pre>
     * Missing sub-programs:
     *   FUNCTION FN_SALARY  → Missing in D2
     *   PROCEDURE XYZ       → Missing in D1
     * </pre>
     *
     * @param diff the diff object (must have been populated by
     *             {@link #detectMissingSubPrograms} or equivalent)
     * @return formatted string, or empty string if nothing is missing
     */
    public static String formatMissingSubProgramsSummary(DbObjectDiff diff) {
        List<DbObjectDiff.SubProgramDiff> missing = new ArrayList<>();
        for (DbObjectDiff.SubProgramDiff sp : diff.getSubProgramDiffs()) {
            if ("MISSING_IN_D1".equals(sp.getStatus()) ||
                "MISSING_IN_D2".equals(sp.getStatus())) {
                missing.add(sp);
            }
        }

        if (missing.isEmpty()) return "";

        // Sort: FUNCTION before PROCEDURE, then alphabetically by name
        missing.sort((a, b) -> {
            int typeCmp = a.getType().compareTo(b.getType());
            return typeCmp != 0 ? typeCmp : a.getName().compareTo(b.getName());
        });

        StringBuilder sb = new StringBuilder("Missing sub-programs:\n");
        int maxNameLen = missing.stream()
                .mapToInt(sp -> (sp.getType() + " " + sp.getName()).length())
                .max().orElse(0);

        for (DbObjectDiff.SubProgramDiff sp : missing) {
            String label = sp.getType() + " " + sp.getName();
            String direction = "MISSING_IN_D2".equals(sp.getStatus())
                    ? "Missing in D2"
                    : "Missing in D1";
            // Pad label for columnar alignment
            sb.append("  ")
              .append(String.format("%-" + maxNameLen + "s", label))
              .append("  →  ")
              .append(direction)
              .append("\n");
        }
        return sb.toString().replaceAll("\\s+$", "");
    }

    // =========================================================================
    // PRIVATE HELPERS  (NEW)
    // =========================================================================

    /** Build a "TYPE:NAME" → descriptor map from a package body source. */
    private static Map<String, SubProgramDescriptor> buildDescriptorMap(String packageBody) {
        // TreeMap for consistent iteration order
        Map<String, SubProgramDescriptor> map = new TreeMap<>();
        for (SubProgramDescriptor d : extractSubProgramDescriptors(packageBody)) {
            map.put(d.key(), d);
        }
        return map;
    }

    // =========================================================================
    // PREPROCESSING: Comment & Whitespace Removal  (UNCHANGED)
    // =========================================================================

    private static class PreprocessedSource {
        List<String> processedLines;
        List<Integer> originalLineNumbers;
    }

    private static PreprocessedSource preprocessSource(String source) {
        if (source == null || source.isEmpty()) {
            return new PreprocessedSource() {{
                processedLines = new ArrayList<>();
                originalLineNumbers = new ArrayList<>();
            }};
        }

        List<String> originalLines = ComparisonHelper.split(source);
        PreprocessedSource result = new PreprocessedSource();
        result.processedLines = new ArrayList<>();
        result.originalLineNumbers = new ArrayList<>();

        boolean inMultiLineComment = false;

        for (int lineIdx = 0; lineIdx < originalLines.size(); lineIdx++) {
            String line = originalLines.get(lineIdx);
            int originalLineNum = lineIdx + 1;

            if (inMultiLineComment) {
                if (line.contains("*/")) {
                    inMultiLineComment = false;
                    int endCommentIdx = line.indexOf("*/");
                    line = line.substring(endCommentIdx + 2);
                } else {
                    continue;
                }
            }

            if (line.contains("/*")) {
                int startIdx = line.indexOf("/*");
                String before = line.substring(0, startIdx);
                int endIdx = line.indexOf("*/", startIdx);
                if (endIdx > 0) {
                    String after = line.substring(endIdx + 2);
                    line = before + after;
                } else {
                    inMultiLineComment = true;
                    line = before;
                }
            }

            if (line.contains("--")) {
                int commentIdx = line.indexOf("--");
                line = line.substring(0, commentIdx);
            }

            line = line.replaceAll("\\s+", " ").trim();
            if (line.isEmpty()) continue;
            line = line.toUpperCase();

            result.processedLines.add(line);
            result.originalLineNumbers.add(originalLineNum);
        }

        return result;
    }

    // =========================================================================
    // COMPARISON ENGINE  (MODIFIED — detectMissingSubPrograms called in buildModified)
    // =========================================================================

    public static List<DbObjectDiff> compare(
            DbConnectionFactory.DbConfig db1,
            DbConnectionFactory.DbConfig db2) {

        Set<String> names1 = DatabaseFunctionService.getObjectNames(db1, DbObjectDiff.ObjectType.PACKAGE);
        Set<String> names2 = DatabaseFunctionService.getObjectNames(db2, DbObjectDiff.ObjectType.PACKAGE);

        Set<String> all = new TreeSet<>(names1);
        all.addAll(names2);

        List<DbObjectDiff> result = new ArrayList<>();

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

            DbObjectDiff modified = buildModified(name, db1, db2);
            if (modified != null) result.add(modified);
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // ADDED
    // -------------------------------------------------------------------------

    private static DbObjectDiff buildAdded(String name, DbConnectionFactory.DbConfig db1) {
        String src = resolvePackageBodyForCompare(db1, name);
        List<String> lines = ComparisonHelper.split(src);

        DbObjectDiff diff = new DbObjectDiff(name, DbObjectDiff.ObjectType.PACKAGE, DbObjectDiff.ChangeType.ADDED);
        diff.setDb1SourceLines(lines.size());

        for (int i = 0; i < lines.size(); i++) {
            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(i + 1, null, lines.get(i)));
        }

        for (SubProgramEntry e : extractSubProgramEntries(lines)) {
            diff.getSubProgramDiffs().add(
                    new DbObjectDiff.SubProgramDiff(e.name, e.type, "PRESENT",
                            e.startLine, e.endLine, null));
        }
        return diff;
    }

    // -------------------------------------------------------------------------
    // REMOVED
    // -------------------------------------------------------------------------

    private static DbObjectDiff buildRemoved(String name, DbConnectionFactory.DbConfig db2) {
        String src = resolvePackageBodyForCompare(db2, name);
        List<String> lines = ComparisonHelper.split(src);

        DbObjectDiff diff = new DbObjectDiff(name, DbObjectDiff.ObjectType.PACKAGE, DbObjectDiff.ChangeType.REMOVED);
        diff.setDb2SourceLines(lines.size());

        for (int i = 0; i < lines.size(); i++) {
            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(i + 1, lines.get(i), null));
        }

        for (SubProgramEntry e : extractSubProgramEntries(lines)) {
            diff.getSubProgramDiffs().add(
                    new DbObjectDiff.SubProgramDiff(e.name, e.type, "PRESENT",
                            e.startLine, e.endLine, null));
        }
        return diff;
    }

    // -------------------------------------------------------------------------
    // MODIFIED  — now calls detectMissingSubPrograms  (CHANGED)
    // -------------------------------------------------------------------------

    private static DbObjectDiff buildModified(String name,
                                              DbConnectionFactory.DbConfig db1,
                                              DbConnectionFactory.DbConfig db2) {
        String body1 = resolvePackageBodyForCompare(db1, name);
        String body2 = resolvePackageBodyForCompare(db2, name);

        PreprocessedSource preprocessed1 = preprocessSource(body1);
        PreprocessedSource preprocessed2 = preprocessSource(body2);

        SourceDiffEngine.DiffResult diffResult = SourceDiffEngine.diff(
                preprocessed1.processedLines,
                preprocessed2.processedLines);

        // ── STEP 1: check for any missing sub-programs even when source is identical ──
        DbObjectDiff tempDiff = new DbObjectDiff(name, DbObjectDiff.ObjectType.PACKAGE,
                DbObjectDiff.ChangeType.MODIFIED);
        detectMissingSubPrograms(tempDiff, body1, body2);
        boolean hasMissing = !tempDiff.getSubProgramDiffs().isEmpty();

        if (diffResult.isIdentical() && !hasMissing) {
            return null;  // truly identical — nothing to report
        }

        // ── STEP 2: build the real diff object ──
        DbObjectDiff diff = new DbObjectDiff(name, DbObjectDiff.ObjectType.PACKAGE,
                DbObjectDiff.ChangeType.MODIFIED);
        diff.setSimilarity(diffResult.getSimilarity());

        for (SourceDiffEngine.DiffEntry entry : diffResult.getEntries()) {
            if (entry.getType() == SourceDiffEngine.DiffType.EQUAL) continue;

            int originalLineNum = mapToOriginalLineNumber(entry, preprocessed1, preprocessed2);
            String oldLine    = entry.getOldLine() != null ? normalizeForDisplay(entry.getOldLine()) : null;
            String newLine    = entry.getNewLine() != null ? normalizeForDisplay(entry.getNewLine()) : null;
            String charSummary = formatCharSummary(entry.charChangeSummary());

            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(
                    originalLineNum, oldLine, newLine,
                    entry.getType().name(),
                    charSummary.isEmpty() ? null : charSummary));
        }

        List<String> raw1 = ComparisonHelper.split(body1);
        List<String> raw2 = ComparisonHelper.split(body2);
        diff.setDb1SourceLines(raw1.size());
        diff.setDb2SourceLines(raw2.size());

        // ── STEP 3: line-level sub-program diffs (handles MISSING_IN_D1 / MISSING_IN_D2 internally) ──
        buildSubProgramDiffs(diff, raw1, raw2);

        // ── STEP 4: semantic analysis (unchanged) ──
        performSemanticAnalysis(diff, raw1, raw2);

        return diff;
    }

    private static int mapToOriginalLineNumber(SourceDiffEngine.DiffEntry entry,
                                               PreprocessedSource preprocessed1,
                                               PreprocessedSource preprocessed2) {
        if (entry.getOldLineNum() > 0) {
            int idx = entry.getOldLineNum() - 1;
            if (idx >= 0 && idx < preprocessed1.originalLineNumbers.size()) {
                return preprocessed1.originalLineNumbers.get(idx);
            }
        }
        int idx = entry.getNewLineNum() - 1;
        if (idx >= 0 && idx < preprocessed2.originalLineNumbers.size()) {
            return preprocessed2.originalLineNumbers.get(idx);
        }
        return Math.max(entry.getOldLineNum(), entry.getNewLineNum());
    }

    // =========================================================================
    // TEXT DISPLAY HELPERS  (UNCHANGED)
    // =========================================================================

    private static String normalizeForDisplay(String line) {
        if (line == null) return null;
        return line.replaceAll("\\s+", " ").trim().toUpperCase();
    }

    private static String truncateForDisplay(String line, int maxLen) {
        if (line == null) return null;
        String normalized = normalizeForDisplay(line);
        if (normalized.length() <= maxLen) return normalized;
        return normalized.substring(0, maxLen - 3) + "...";
    }

    private static String formatCharSummary(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.replaceAll("\\s{2,}", "  ")
                  .replaceAll("(\\[[-+][^\\]]{0,80}\\])", "$1 ")
                  .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                  .trim();
    }

    // =========================================================================
    // SEMANTIC ANALYSIS  (UNCHANGED)
    // =========================================================================

    private static void performSemanticAnalysis(DbObjectDiff diff, List<String> raw1, List<String> raw2) {
        List<VariableEntry> vars1 = extractDeclaredVariables(raw1);
        List<VariableEntry> vars2 = extractDeclaredVariables(raw2);
        compareVariables(diff, vars1, vars2);

        List<SqlQueryEntry> queries1 = extractSqlQueries(raw1);
        List<SqlQueryEntry> queries2 = extractSqlQueries(raw2);
        compareQueries(diff, queries1, queries2);

        List<ControlFlowEntry> flow1 = extractControlFlow(raw1);
        List<ControlFlowEntry> flow2 = extractControlFlow(raw2);
        compareControlFlow(diff, flow1, flow2);

        List<OutputEntry> output1 = extractDbmsOutput(raw1);
        List<OutputEntry> output2 = extractDbmsOutput(raw2);
        compareOutputs(diff, output1, output2);

        List<ExceptionEntry> exc1 = extractExceptionHandlers(raw1);
        List<ExceptionEntry> exc2 = extractExceptionHandlers(raw2);
        compareExceptions(diff, exc1, exc2);

        analyzeLogicFlow(diff, raw1, raw2);
    }

    private static void compareVariables(DbObjectDiff diff, List<VariableEntry> vars1, List<VariableEntry> vars2) {
        Map<String, VariableEntry> map2 = new LinkedHashMap<>();
        for (VariableEntry v : vars2) map2.put(v.name.toUpperCase(), v);

        for (VariableEntry v1 : vars1) {
            VariableEntry v2 = map2.get(v1.name.toUpperCase());
            if (v2 == null) {
                diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(v1.lineNumber,
                        "VARIABLE: " + v1.name + " " + v1.dataType, null, "REMOVED", null));
            } else if (!normalizedForComparison(v1.dataType).equals(normalizedForComparison(v2.dataType)) ||
                       !compareDefaultValues(v1.defaultValue, v2.defaultValue)) {
                diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(v1.lineNumber,
                        "VARIABLE: " + v1.name + " " + v1.dataType + (v1.defaultValue != null ? " := " + v1.defaultValue : ""),
                        "VARIABLE: " + v2.name + " " + v2.dataType + (v2.defaultValue != null ? " := " + v2.defaultValue : ""),
                        "MODIFIED", null));
            }
            map2.remove(v1.name.toUpperCase());
        }

        for (VariableEntry v2 : map2.values()) {
            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(v2.lineNumber, null,
                    "VARIABLE: " + v2.name + " " + v2.dataType, "ADDED", null));
        }
    }

    private static boolean compareDefaultValues(String val1, String val2) {
        if (val1 == null && val2 == null) return true;
        if (val1 == null || val2 == null) return false;
        return normalizedForComparison(val1).equals(normalizedForComparison(val2));
    }

    private static void compareQueries(DbObjectDiff diff,
                                       List<SqlQueryEntry> queries1,
                                       List<SqlQueryEntry> queries2) {
        Map<String, SqlQueryEntry> map2 = new LinkedHashMap<>();
        for (SqlQueryEntry q : queries2) map2.put(normalizedForComparison(q.query), q);

        Set<String> matched = new HashSet<>();

        for (SqlQueryEntry q1 : queries1) {
            String norm1 = normalizedForComparison(q1.query);
            SqlQueryEntry q2 = map2.get(norm1);

            if (q2 == null) {
                SqlQueryEntry similar = findSimilarQuery(q1, queries2);
                if (similar != null) {
                    diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(q1.lineNumber,
                            "SQL: " + truncateForDisplay(q1.query, 80) + (q1.intoVariable != null ? " INTO " + q1.intoVariable : ""),
                            "SQL: " + truncateForDisplay(similar.query, 80) + (similar.intoVariable != null ? " INTO " + similar.intoVariable : ""),
                            "MODIFIED", detectQueryMeaning(q1.query, similar.query)));
                    matched.add(normalizedForComparison(similar.query));
                } else {
                    diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(q1.lineNumber,
                            "SQL: " + truncateForDisplay(q1.query, 80) + (q1.intoVariable != null ? " INTO " + q1.intoVariable : ""),
                            null, "REMOVED", null));
                }
            } else if (!compareIntoVariables(q1.intoVariable, q2.intoVariable)) {
                diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(q1.lineNumber,
                        "SQL: " + truncateForDisplay(q1.query, 80) + (q1.intoVariable != null ? " INTO " + q1.intoVariable : ""),
                        "SQL: " + truncateForDisplay(q2.query, 80) + (q2.intoVariable != null ? " INTO " + q2.intoVariable : ""),
                        "MODIFIED", "INTO variable changed"));
                matched.add(norm1);
            } else {
                matched.add(norm1);
            }
        }

        for (SqlQueryEntry q2 : queries2) {
            if (!matched.contains(normalizedForComparison(q2.query))) {
                diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(q2.lineNumber, null,
                        "SQL: " + truncateForDisplay(q2.query, 80) + (q2.intoVariable != null ? " INTO " + q2.intoVariable : ""),
                        "ADDED", null));
            }
        }
    }

    private static SqlQueryEntry findSimilarQuery(SqlQueryEntry q1, List<SqlQueryEntry> candidates) {
        String table1 = extractTableName(q1.query);
        if (table1 == null) return null;
        for (SqlQueryEntry q : candidates) {
            if (table1.equalsIgnoreCase(extractTableName(q.query))) return q;
        }
        return null;
    }

    private static String extractTableName(String query) {
        Pattern p = Pattern.compile("FROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(query);
        return m.find() ? m.group(1) : null;
    }

    private static String detectQueryMeaning(String q1, String q2) {
        String u1 = q1.toUpperCase(), u2 = q2.toUpperCase();
        if (u1.contains("COUNT(") != u2.contains("COUNT("))
            return u2.contains("COUNT(") ? "COUNT added" : "COUNT removed";
        if (u1.contains("DISTINCT") != u2.contains("DISTINCT"))
            return "DISTINCT " + (u2.contains("DISTINCT") ? "added" : "removed");
        if (u1.contains(" JOIN ") != u2.contains(" JOIN "))
            return "JOIN " + (u2.contains(" JOIN ") ? "added" : "removed");
        if (u1.contains("GROUP BY") != u2.contains("GROUP BY"))
            return "GROUP BY " + (u2.contains("GROUP BY") ? "added" : "removed");
        if (u1.contains("WHERE") != u2.contains("WHERE"))
            return "WHERE clause " + (u2.contains("WHERE") ? "added" : "removed");
        return "SQL query modified";
    }

    private static boolean compareIntoVariables(String var1, String var2) {
        if (var1 == null && var2 == null) return true;
        if (var1 == null || var2 == null) return false;
        return var1.equalsIgnoreCase(var2);
    }

    private static void compareControlFlow(DbObjectDiff diff,
                                           List<ControlFlowEntry> flow1,
                                           List<ControlFlowEntry> flow2) {
        Map<String, ControlFlowEntry> map2 = new LinkedHashMap<>();
        for (ControlFlowEntry f : flow2)
            map2.put(f.flowType + ":" + normalizedForComparison(f.statement), f);

        Set<String> matchedKeys = new HashSet<>();
        for (ControlFlowEntry f1 : flow1) {
            String key1 = f1.flowType + ":" + normalizedForComparison(f1.statement);
            if (map2.containsKey(key1)) {
                matchedKeys.add(key1);
            } else {
                diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(f1.lineNumber,
                        "CONTROL [" + f1.flowType + "]: " + f1.statement, null, "REMOVED", null));
            }
        }

        for (Map.Entry<String, ControlFlowEntry> entry : map2.entrySet()) {
            if (!matchedKeys.contains(entry.getKey())) {
                ControlFlowEntry f2 = entry.getValue();
                diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(f2.lineNumber, null,
                        "CONTROL [" + f2.flowType + "]: " + f2.statement, "ADDED", null));
            }
        }
    }

    private static void compareOutputs(DbObjectDiff diff,
                                       List<OutputEntry> output1,
                                       List<OutputEntry> output2) {
        Map<String, OutputEntry> map2 = new LinkedHashMap<>();
        for (OutputEntry o : output2) map2.put(o.message, o);

        for (OutputEntry o1 : output1) {
            if (!map2.containsKey(o1.message)) {
                diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(o1.lineNumber,
                        "OUTPUT: " + o1.message, null, "REMOVED", null));
            }
            map2.remove(o1.message);
        }

        for (OutputEntry o2 : map2.values()) {
            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(o2.lineNumber, null,
                    "OUTPUT: " + o2.message, "ADDED", null));
        }
    }

    private static void compareExceptions(DbObjectDiff diff,
                                          List<ExceptionEntry> exc1,
                                          List<ExceptionEntry> exc2) {
        Map<String, ExceptionEntry> map2 = new LinkedHashMap<>();
        for (ExceptionEntry e : exc2) map2.put(e.exceptionType, e);

        for (ExceptionEntry e1 : exc1) {
            if (!map2.containsKey(e1.exceptionType)) {
                diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(e1.lineNumber,
                        "EXCEPTION: " + e1.exceptionType, null, "REMOVED", null));
            }
            map2.remove(e1.exceptionType);
        }

        for (ExceptionEntry e2 : map2.values()) {
            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(e2.lineNumber, null,
                    "EXCEPTION: " + e2.exceptionType, "ADDED", null));
        }
    }

    // -------------------------------------------------------------------------
    // Sub-program diff (line-level — UNCHANGED logic, renamed helper)
    // -------------------------------------------------------------------------

    private static void buildSubProgramDiffs(DbObjectDiff diff,
                                             List<String> oldLines,
                                             List<String> newLines) {

        List<SubProgramEntry> entries1 = extractSubProgramEntries(oldLines);
        List<SubProgramEntry> entries2 = extractSubProgramEntries(newLines);
        Set<String> typedInD1 = extractTypedSubPrograms(joinLines(oldLines, 1, oldLines.size()));
        Set<String> missingInD2Keys = new HashSet<>(typedInD1);
        Set<String> typedInD2 = extractTypedSubPrograms(joinLines(newLines, 1, newLines.size()));
        missingInD2Keys.removeAll(typedInD2);
        Set<String> missingInD1Keys = new HashSet<>(typedInD2);
        missingInD1Keys.removeAll(typedInD1);

        Map<String, SubProgramEntry> map2 = new LinkedHashMap<>();
        for (SubProgramEntry e : entries2) map2.put(e.type + ":" + e.name, e);

        Set<Integer> changedLines1 = new HashSet<>();
        for (DbObjectDiff.SourceDiffLine dl : diff.getSourceDiffs()) changedLines1.add(dl.getLineNumber());

        for (SubProgramEntry e1 : entries1) {
            SubProgramEntry e2 = map2.get(e1.type + ":" + e1.name);

            List<Integer> localChanged = new ArrayList<>();
            for (int ln : changedLines1) {
                if (ln >= e1.startLine && ln <= e1.endLine) localChanged.add(ln);
            }
            Collections.sort(localChanged);

            boolean changed = !localChanged.isEmpty();
            String key = e1.type + ":" + e1.name;

            if (e2 != null && !changed) {
                String block1 = joinLines(oldLines, e1.startLine, e1.endLine);
                String block2 = joinLines(newLines, e2.startLine, e2.endLine);

                if (!normalizedForComparison(block1).equals(normalizedForComparison(block2))) {
                    changed = true;
                }
            }

            String status = (e2 == null || missingInD2Keys.contains(key))
                    ? "MISSING_IN_D2"
                    : (changed ? "DIFFERENT" : "SAME");

            diff.getSubProgramDiffs().add(new DbObjectDiff.SubProgramDiff(
                    e1.name, e1.type, status,
                    e1.startLine, e1.endLine, localChanged));
        }

        for (SubProgramEntry e2 : entries2) {
            boolean foundInDb1 = entries1.stream()
                    .anyMatch(e1 -> e1.name.equals(e2.name) && e1.type.equals(e2.type));
            String key = e2.type + ":" + e2.name;
            if (!foundInDb1 || missingInD1Keys.contains(key)) {
                diff.getSubProgramDiffs().add(new DbObjectDiff.SubProgramDiff(
                        e2.name, e2.type, "MISSING_IN_D1",
                        e2.startLine, e2.endLine, null));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Sub-program entry extraction from List<String>
    //  (renamed from extractSubPrograms(List) to avoid collision with public API)
    // -------------------------------------------------------------------------

    private static final Pattern SUB_PROGRAM_PATTERN = Pattern.compile(
            "^\\s*(?:MEMBER\\s+)?(PROCEDURE|FUNCTION)\\s+(\\w+)\\s*(?:\\(|\\bIS\\b|\\bAS\\b|$)",
            Pattern.CASE_INSENSITIVE);

    /** Pattern used by the new public API — operates on the full source string. */
    private static final Pattern SUB_PROGRAM_NAME_PATTERN = Pattern.compile(
            "^\\s*(?:MEMBER\\s+)?(PROCEDURE|FUNCTION)" +
            "\\s+(\"[^\"]+\"|[A-Za-z][A-Za-z0-9_$#]*)" +
            "\\s*(?:\\(|\\bIS\\b|\\bAS\\b|$)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    /** Internal helper — extracts line-level SubProgramEntry list from split lines. */
    private static List<SubProgramEntry> extractSubProgramEntries(List<String> lines) {
        List<SubProgramEntry> all = new ArrayList<>();

        int bodyStart = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equalsIgnoreCase("-- PACKAGE BODY --")) {
                bodyStart = i + 1;
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

        for (int i = 0; i < all.size() - 1; i++) {
            all.get(i).endLine = all.get(i + 1).startLine - 1;
        }
        if (!all.isEmpty()) all.get(all.size() - 1).endLine = lines.size();

        Map<String, SubProgramEntry> seen = new LinkedHashMap<>();
        for (SubProgramEntry e : all) {
            String key = e.type + ":" + e.name;
            SubProgramEntry existing = seen.get(key);
            if (existing == null || (e.inBody && !existing.inBody)) {
                seen.put(key, e);
            }
        }

        return new ArrayList<>(seen.values());
    }

    private static Set<String> extractTypedSubPrograms(String packageBody) {
        Set<String> typed = new TreeSet<>();
        if (packageBody == null || packageBody.trim().isEmpty()) return typed;

        String bodyWithoutComments = stripPlSqlComments(packageBody);
        Matcher matcher = SUB_PROGRAM_NAME_PATTERN.matcher(bodyWithoutComments);
        while (matcher.find()) {
            String type = matcher.group(1).toUpperCase();
            String nm   = normalizeIdentifier(matcher.group(2));
            typed.add(type + ":" + nm);
        }
        return typed;
    }

    // =========================================================================
    // PL/SQL comment stripping & identifier helpers  (SHARED)
    // =========================================================================

    private static String stripPlSqlComments(String text) {
        if (text == null || text.isEmpty()) return "";
        String noBlock = text.replaceAll("(?s)/\\*.*?\\*/", " ");
        return noBlock.replaceAll("(?m)--.*$", " ");
    }

    private static String normalizeIdentifier(String rawName) {
        if (rawName == null) return "";
        return rawName.replace("\"", "").trim().toUpperCase();
    }

    // =========================================================================
    // SEMANTIC ANALYSIS: Logic Flow  (UNCHANGED)
    // =========================================================================

    private static void analyzeLogicFlow(DbObjectDiff diff,
                                         List<String> oldLines,
                                         List<String> newLines) {
        List<LogicFlowEntry> flow1 = extractLogicFlow(oldLines);
        List<LogicFlowEntry> flow2 = extractLogicFlow(newLines);
        detectLogicChanges(diff, flow1, flow2);
        detectExecutionOrderChanges(diff, flow1, flow2);
    }

    private static void detectLogicChanges(DbObjectDiff diff,
                                           List<LogicFlowEntry> flow1,
                                           List<LogicFlowEntry> flow2) {
        Map<String, LogicFlowEntry> countQ1 = new HashMap<>(), dataQ1 = new HashMap<>();
        for (LogicFlowEntry e : flow1) {
            if (e.type.equals("COUNT_CHECK")) countQ1.put(e.type, e);
            else if (e.type.equals("DATA_FETCH")) dataQ1.put(e.type, e);
        }

        Map<String, LogicFlowEntry> countQ2 = new HashMap<>(), dataQ2 = new HashMap<>();
        for (LogicFlowEntry e : flow2) {
            if (e.type.equals("COUNT_CHECK")) countQ2.put(e.type, e);
            else if (e.type.equals("DATA_FETCH")) dataQ2.put(e.type, e);
        }

        if (countQ1.containsKey("COUNT_CHECK") && !countQ2.containsKey("COUNT_CHECK")) {
            LogicFlowEntry cq = countQ1.get("COUNT_CHECK");
            String dq = dataQ2.containsKey("DATA_FETCH") ? dataQ2.get("DATA_FETCH").content.trim() : "";
            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(cq.lineNumber,
                    "SQL: " + formatQuery(cq.content),
                    dq.isEmpty() ? null : "SQL: " + formatQuery(dq),
                    "MODIFIED", "Validation removed"));

        } else if (!countQ1.containsKey("COUNT_CHECK") && countQ2.containsKey("COUNT_CHECK")) {
            LogicFlowEntry cq = countQ2.get("COUNT_CHECK");
            String dq = dataQ1.containsKey("DATA_FETCH") ? dataQ1.get("DATA_FETCH").content.trim() : "";
            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(cq.lineNumber,
                    dq.isEmpty() ? null : "SQL: " + formatQuery(dq),
                    "SQL: " + formatQuery(cq.content),
                    "MODIFIED", "Validation added"));
        }
    }

    private static String formatQuery(String query) {
        if (query == null) return "";
        String q = query.replaceAll("\\s+", " ").trim();
        return q.length() > 100 ? q.substring(0, 97) + "..." : q;
    }

    private static void detectExecutionOrderChanges(DbObjectDiff diff,
                                                    List<LogicFlowEntry> flow1,
                                                    List<LogicFlowEntry> flow2) {
        List<String> keys1 = new ArrayList<>(), keys2 = new ArrayList<>();
        for (LogicFlowEntry e : flow1) keys1.add(e.type);
        for (LogicFlowEntry e : flow2) keys2.add(e.type);

        if (!keys1.equals(keys2) && flow1.size() > 1 && flow2.size() > 1) {
            String q1 = extractMeaningfulQuery(flow1);
            String q2 = extractMeaningfulQuery(flow2);
            String desc = generateFlowDiffDescription(keys1, keys2);

            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(
                    Math.max(flow1.get(0).lineNumber, flow2.get(0).lineNumber),
                    q1.isEmpty() ? "EXECUTION ORDER: " + String.join(" → ", keys1) : "SQL: " + q1,
                    q2.isEmpty() ? "EXECUTION ORDER: " + String.join(" → ", keys2) : "SQL: " + q2,
                    "MODIFIED", desc));
        }
    }

    private static String extractMeaningfulQuery(List<LogicFlowEntry> flow) {
        for (LogicFlowEntry e : flow) {
            if (e.type.equals("COUNT_CHECK") || e.type.equals("DATA_FETCH") || e.type.equals("DML")) {
                String q = e.content.replaceAll("\\s+", " ").trim();
                return q.length() > 80 ? q.substring(0, 77) + "..." : q;
            }
        }
        return "";
    }

    private static String generateFlowDiffDescription(List<String> keys1, List<String> keys2) {
        boolean hasCount1 = keys1.contains("COUNT_CHECK");
        boolean hasCount2 = keys2.contains("COUNT_CHECK");
        if (!hasCount1 && hasCount2) return "Validation added";
        if (hasCount1 && !hasCount2) return "Validation removed";
        boolean hasSel1 = keys1.contains("DATA_FETCH") || keys1.contains("COUNT_CHECK");
        boolean hasSel2 = keys2.contains("DATA_FETCH") || keys2.contains("COUNT_CHECK");
        if (hasSel1 != hasSel2) return "Query changed";
        return "Logic flow reordered";
    }

    private static List<LogicFlowEntry> extractLogicFlow(List<String> lines) {
        List<LogicFlowEntry> flow = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim().toUpperCase();
            if      (line.startsWith("SELECT") && line.contains("COUNT("))  flow.add(new LogicFlowEntry("COUNT_CHECK", lines.get(i), i + 1));
            else if (line.startsWith("SELECT"))                              flow.add(new LogicFlowEntry("DATA_FETCH",  lines.get(i), i + 1));
            else if (line.startsWith("IF ") || line.startsWith("ELSIF "))   flow.add(new LogicFlowEntry("CONDITION",   lines.get(i), i + 1));
            else if (line.startsWith("INSERT ") || line.startsWith("UPDATE ") || line.startsWith("DELETE "))
                                                                             flow.add(new LogicFlowEntry("DML",         lines.get(i), i + 1));
            else if (line.startsWith("DBMS_OUTPUT"))                        flow.add(new LogicFlowEntry("OUTPUT",      lines.get(i), i + 1));
            else if (line.startsWith("EXCEPTION") || line.startsWith("WHEN "))
                                                                             flow.add(new LogicFlowEntry("EXCEPTION_HANDLER", lines.get(i), i + 1));
        }
        return flow;
    }

    // =========================================================================
    // SEMANTIC ANALYSIS: PL/SQL Variable, Query, Control Flow  (UNCHANGED)
    // =========================================================================

    private static List<VariableEntry> extractDeclaredVariables(List<String> lines) {
        List<VariableEntry> variables = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return variables;
        for (SubProgramEntry sp : extractSubProgramEntries(lines)) {
            variables.addAll(extractVariablesFromSubProgram(lines, sp.startLine, sp.endLine));
        }
        return variables;
    }

    private static List<VariableEntry> extractVariablesFromSubProgram(
            List<String> lines, int startLine, int endLine) {

        List<VariableEntry> variables = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return variables;

        int startIdx = Math.max(0, startLine - 1);
        int endIdx   = Math.min(lines.size(), endLine);

        int declareIdx = -1;
        for (int i = startIdx; i < endIdx; i++) {
            String line = lines.get(i).trim().toUpperCase();
            if (line.equals("DECLARE") || line.startsWith("DECLARE ")) { declareIdx = i; break; }
        }

        int beginIdx = endIdx;
        int searchFrom = declareIdx >= 0 ? declareIdx + 1 : startIdx;
        for (int i = searchFrom; i < endIdx; i++) {
            String line = lines.get(i).trim().toUpperCase();
            if (line.equals("BEGIN") || line.startsWith("BEGIN ")) { beginIdx = i; break; }
        }

        int varStartIdx = declareIdx >= 0 ? declareIdx + 1 : startIdx;
        for (int i = varStartIdx; i < beginIdx; i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*")) continue;
            variables.addAll(parseVariableDeclarations(line, i + 1));
        }
        return variables;
    }

    private static List<VariableEntry> parseVariableDeclarations(String line, int lineNumber) {
        List<VariableEntry> result = new ArrayList<>();
        if (line == null || line.isEmpty()) return result;

        String cleanLine = line.replaceAll("[;,]\\s*$", "").trim();
        String upperLine = cleanLine.toUpperCase();

        if (upperLine.matches("^(IF|THEN|ELSE|ELSIF|END|WHEN|RETURN|FOR|WHILE|LOOP|EXIT|EXCEPTION|CURSOR|BEGIN|PROCEDURE|FUNCTION)\\b.*")) {
            return result;
        }

        String[] dataTypes = {
            "NUMBER","VARCHAR2","VARCHAR","CHAR","NCHAR","NVARCHAR2",
            "INT","INTEGER","PLS_INTEGER","BINARY_INTEGER",
            "DATE","TIMESTAMP","TIME","INTERVAL",
            "BOOLEAN","FLOAT","REAL","DOUBLE",
            "CLOB","BLOB","NCLOB","BFILE",
            "RAW","LONG","LONG RAW","ROWID","UROWID",
            "REF","RECORD","TABLE","VARRAY"
        };

        for (String dataType : dataTypes) {
            Pattern p = Pattern.compile(
                    "^(\\w+)\\s+" + Pattern.quote(dataType) + "(?:\\s*\\([^)]*\\))?(?:\\s+.*)?$",
                    Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(cleanLine);
            if (m.find()) {
                String varName = m.group(1);
                if (!varName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) continue;
                result.add(new VariableEntry(varName,
                        extractDataType(cleanLine, dataType),
                        lineNumber,
                        extractDefaultValue(cleanLine)));
                break;
            }
        }
        return result;
    }

    private static String extractDefaultValue(String line) {
        int idx = line.indexOf(":=");
        if (idx > 0) return line.substring(idx + 2).replaceAll("[;,]\\s*$", "").trim();
        return null;
    }

    private static String extractDataType(String line, String baseType) {
        int typeStart = line.toUpperCase().indexOf(baseType.toUpperCase());
        if (typeStart < 0) return baseType;

        int idx = typeStart + baseType.length();
        StringBuilder tb = new StringBuilder(baseType);
        while (idx < line.length() && Character.isWhitespace(line.charAt(idx))) idx++;

        if (idx < line.length() && line.charAt(idx) == '(') {
            int parenCount = 0, startParen = idx;
            while (idx < line.length()) {
                char c = line.charAt(idx);
                if (c == '(') parenCount++;
                else if (c == ')') { parenCount--; if (parenCount == 0) { tb.append(line, startParen, idx + 1); break; } }
                idx++;
            }
        }
        return tb.toString();
    }

    private static List<SqlQueryEntry> extractSqlQueries(List<String> lines) {
        List<SqlQueryEntry> queries = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return queries;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim().toUpperCase();
            if (line.startsWith("SELECT") || line.startsWith("INSERT") ||
                line.startsWith("UPDATE") || line.startsWith("DELETE")) {

                StringBuilder query = new StringBuilder(lines.get(i));
                int j = i;
                while (j < lines.size() && !lines.get(j).trim().endsWith(";")) {
                    j++;
                    if (j < lines.size()) query.append(" ").append(lines.get(j));
                }
                queries.add(new SqlQueryEntry(query.toString().trim(), i + 1,
                        extractIntoVariable(query.toString())));
            }
        }
        return queries;
    }

    private static String extractIntoVariable(String query) {
        Pattern p = Pattern.compile("\\bINTO\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(query);
        return m.find() ? m.group(1) : null;
    }

    private static List<ControlFlowEntry> extractControlFlow(List<String> lines) {
        List<ControlFlowEntry> flows = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return flows;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim().toUpperCase();
            if (line.startsWith("IF ") || line.startsWith("ELSE") ||
                line.startsWith("ELSIF ") || line.startsWith("RETURN")) {
                flows.add(new ControlFlowEntry(extractFlowType(line), lines.get(i).trim(), i + 1));
            }
        }
        return flows;
    }

    private static String extractFlowType(String line) {
        if (line.startsWith("IF "))     return "IF";
        if (line.startsWith("ELSIF ")) return "ELSIF";
        if (line.startsWith("ELSE"))   return "ELSE";
        if (line.startsWith("RETURN")) return "RETURN";
        return "UNKNOWN";
    }

    private static List<OutputEntry> extractDbmsOutput(List<String> lines) {
        List<OutputEntry> outputs = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return outputs;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).toUpperCase();
            if (line.contains("DBMS_OUTPUT.PUT_LINE") || line.contains("DBMS_OUTPUT.PUT")) {
                Pattern p = Pattern.compile("DBMS_OUTPUT\\.PUT(?:_LINE)?\\s*\\(([^)]*)\\)",
                        Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(line);
                if (m.find()) outputs.add(new OutputEntry(m.group(1).trim(), i + 1));
            }
        }
        return outputs;
    }

    private static List<ExceptionEntry> extractExceptionHandlers(List<String> lines) {
        List<ExceptionEntry> exceptions = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return exceptions;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim().toUpperCase();
            if (line.equals("EXCEPTION") || line.startsWith("EXCEPTION ")) {
                exceptions.add(new ExceptionEntry("EXCEPTION_BLOCK", i + 1));
            } else if (line.startsWith("WHEN ")) {
                exceptions.add(new ExceptionEntry(extractExceptionType(lines.get(i)), i + 1));
            }
        }
        return exceptions;
    }

    private static String extractExceptionType(String line) {
        Pattern p = Pattern.compile("\\bWHEN\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(line);
        return m.find() ? m.group(1) : "UNKNOWN_EXCEPTION";
    }

    // =========================================================================
    // TEXT / SOURCE HELPERS  (UNCHANGED)
    // =========================================================================

    private static String resolvePackageBodyForCompare(DbConnectionFactory.DbConfig db, String name) {
        String direct = DatabaseFunctionService.getPackageBodySource(db, name);
        if (direct != null && !direct.trim().isEmpty()) return direct;
        String merged = DatabaseFunctionService.getObjectSource(db, DbObjectDiff.ObjectType.PACKAGE, name);
        return extractPackageBodySource(merged != null ? merged : "");
    }

    private static String extractPackageBodySource(String mergedSource) {
        if (mergedSource == null || mergedSource.isEmpty()) return "";
        List<String> lines = ComparisonHelper.split(mergedSource);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equalsIgnoreCase("-- PACKAGE BODY --")) {
                StringBuilder sb = new StringBuilder();
                for (int j = i + 1; j < lines.size(); j++) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(lines.get(j));
                }
                return sb.toString();
            }
        }
        return mergedSource;
    }

    private static String normalizedForComparison(String v) {
        if (v == null) return "";
        return v.replaceAll("\\s+", "").toUpperCase();
    }

    private static String joinLines(List<String> lines, int start, int end) {
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, start - 1);
        int to   = Math.min(lines.size(), end);
        for (int i = from; i < to; i++) sb.append(lines.get(i)).append("\n");
        return sb.toString();
    }

    // =========================================================================
    // INNER DATA CLASSES  (UNCHANGED)
    // =========================================================================

    private static final class SubProgramEntry {
        final String name, type;
        final int startLine;
        int endLine;
        boolean inBody;

        SubProgramEntry(String name, String type, int startLine) {
            this.name = name; this.type = type;
            this.startLine = startLine; this.endLine = startLine;
            this.inBody = false;
        }
    }

    private static final class LogicFlowEntry {
        final String type, content;
        final int lineNumber;
        LogicFlowEntry(String type, String content, int lineNumber) {
            this.type = type; this.content = content;
            this.lineNumber = lineNumber;
        }
    }

    private static final class VariableEntry {
        final String name, dataType;
        final int lineNumber;
        final String defaultValue;

        VariableEntry(String name, String dataType, int lineNumber, String defaultValue) {
            this.name = name; this.dataType = dataType;
            this.lineNumber = lineNumber; this.defaultValue = defaultValue;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VariableEntry)) return false;
            VariableEntry that = (VariableEntry) o;
            return name.equals(that.name) && dataType.equals(that.dataType);
        }

        @Override public int hashCode() { return name.hashCode() * 31 + dataType.hashCode(); }

        @Override public String toString() {
            return "VariableEntry{name='" + name + "', dataType='" + dataType + "', line=" + lineNumber + "}";
        }
    }

    private static final class SqlQueryEntry {
        final String query, intoVariable;
        final int lineNumber;
        SqlQueryEntry(String query, int lineNumber, String intoVariable) {
            this.query = query; this.lineNumber = lineNumber; this.intoVariable = intoVariable;
        }
    }

    private static final class ControlFlowEntry {
        final String flowType, statement;
        final int lineNumber;
        ControlFlowEntry(String flowType, String statement, int lineNumber) {
            this.flowType = flowType; this.statement = statement; this.lineNumber = lineNumber;
        }
    }

    private static final class OutputEntry {
        final String message;
        final int lineNumber;
        OutputEntry(String message, int lineNumber) {
            this.message = message; this.lineNumber = lineNumber;
        }
    }

    private static final class ExceptionEntry {
        final String exceptionType;
        final int lineNumber;
        ExceptionEntry(String exceptionType, int lineNumber) {
            this.exceptionType = exceptionType; this.lineNumber = lineNumber;
        }
    }
}