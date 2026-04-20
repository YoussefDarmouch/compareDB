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
            int idx = entry.getOldLineNum() - 1;
            if (idx >= 0 && idx < pw1.rawLineNumbers1Based.size()) {
                return pw1.rawLineNumbers1Based.get(idx);
            }
        }
        int idx = entry.getNewLineNum() - 1;
        if (idx >= 0 && idx < pw2.rawLineNumbers1Based.size()) {
            return pw2.rawLineNumbers1Based.get(idx);
        }
        // Fallback for malformed/edge diff entries to keep comparison resilient.
        return Math.max(entry.getOldLineNum(), entry.getNewLineNum());
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
            String status;

            // Cross-check by comparing the raw blocks when both sides exist
            if (e2 != null && !changed) {
                String block1 = joinLines(oldLines, e1.startLine, e1.endLine);
                String block2 = joinLines(newLines, e2.startLine, e2.endLine);
                changed = !normalizedPackageLine(block1).equals(normalizedPackageLine(block2));
            }

            if (e2 == null) {
                status = "REMOVED";
            } else {
                status = changed ? "DIFFERENT" : "SAME";
            }

            diff.getSubProgramDiffs().add(new DbObjectDiff.SubProgramDiff(
                    e1.name, e1.type,
                    status,
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
                        e2.name, e2.type, "ADDED",
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

    // =========================================================================
    // NEW FEATURE: PL/SQL Variable Declaration Detection
    // =========================================================================
    // Added methods below detect variable declarations in PROCEDURE/FUNCTION
    // blocks without modifying any existing code or logic.
    // =========================================================================

    /**
     * Extracts PL/SQL variable declarations from package lines.
     * Detects variables declared within PROCEDURE and FUNCTION blocks only.
     * 
     * Supported variable patterns:
     *   - v_count NUMBER;
     *   - v_name VARCHAR2(100);
     *   - l_total INT;
     *   - p_amount NUMBER := 0;
     *
     * @param lines the list of source code lines
     * @return a list of detected variable declarations with name, type, and line number
     */
    private static List<VariableEntry> extractDeclaredVariables(List<String> lines) {
        List<VariableEntry> variables = new ArrayList<VariableEntry>();

        if (lines == null || lines.isEmpty()) {
            return variables;
        }

        // Get all procedures and functions in the package
        List<SubProgramEntry> subPrograms = extractSubPrograms(lines);

        // For each procedure/function, extract variables from its DECLARE section
        for (SubProgramEntry subProg : subPrograms) {
            List<VariableEntry> subProgVars = extractVariablesFromSubProgram(
                    lines, subProg.startLine, subProg.endLine);
            variables.addAll(subProgVars);
        }

        return variables;
    }

    /**
     * Extracts variable declarations from a specific PROCEDURE or FUNCTION block.
     * Looks for variables between DECLARE and BEGIN keywords.
     *
     * @param lines all source code lines
     * @param startLine starting line of the subprogram (1-based)
     * @param endLine ending line of the subprogram (1-based)
     * @return list of variables found in this subprogram
     */
    private static List<VariableEntry> extractVariablesFromSubProgram(
            List<String> lines, int startLine, int endLine) {

        List<VariableEntry> variables = new ArrayList<VariableEntry>();

        if (lines == null || lines.isEmpty()) {
            return variables;
        }

        // Convert to 0-based indices
        int startIdx = Math.max(0, startLine - 1);
        int endIdx = Math.min(lines.size(), endLine);

        // Find DECLARE keyword within this subprogram
        int declareIdx = -1;
        for (int i = startIdx; i < endIdx; i++) {
            String line = lines.get(i).trim().toUpperCase();
            if (line.equals("DECLARE")) {
                declareIdx = i;
                break;
            }
        }

        // If no DECLARE found, no variables to extract
        if (declareIdx < 0) {
            return variables;
        }

        // Find BEGIN keyword after DECLARE
        int beginIdx = -1;
        for (int i = declareIdx + 1; i < endIdx; i++) {
            String line = lines.get(i).trim().toUpperCase();
            if (line.equals("BEGIN")) {
                beginIdx = i;
                break;
            }
        }

        // If no BEGIN found, search up to end of subprogram
        if (beginIdx < 0) {
            beginIdx = endIdx;
        }

        // Extract variables between DECLARE and BEGIN
        for (int i = declareIdx + 1; i < beginIdx; i++) {
            String line = lines.get(i).trim();
            int lineNumber = i + 1; // Convert back to 1-based

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*")) {
                continue;
            }

            // Try to parse as variable declaration
            VariableEntry var = parseVariableDeclaration(line, lineNumber);
            if (var != null) {
                variables.add(var);
            }
        }

        return variables;
    }

    /**
     * Attempts to parse a single line as a PL/SQL variable declaration.
     * Recognizes patterns like: variable_name DataType [options];
     *
     * @param line the line of source code to parse
     * @param lineNumber the 1-based line number (for tracking)
     * @return a VariableEntry if successfully parsed, null otherwise
     */
    private static VariableEntry parseVariableDeclaration(String line, int lineNumber) {
        if (line == null || line.isEmpty()) {
            return null;
        }

        // Remove trailing semicolon or comma
        String cleanLine = line.replaceAll("[;,]\\s*$", "").trim();

        // Skip SQL keywords that look like variable declarations
        String upperLine = cleanLine.toUpperCase();
        if (upperLine.matches("^(IF|THEN|ELSE|ELSIF|END|WHEN|RETURN|FOR|WHILE|LOOP|EXIT|BEGIN|EXCEPTION|CURSOR)\\b.*")) {
            return null;
        }

        // List of supported PL/SQL data types
        String[] dataTypes = {
            "NUMBER", "VARCHAR2", "VARCHAR", "CHAR", "NCHAR", "NVARCHAR2",
            "INT", "INTEGER", "PLS_INTEGER", "BINARY_INTEGER",
            "DATE", "TIMESTAMP", "TIME", "INTERVAL",
            "BOOLEAN", "FLOAT", "REAL", "DOUBLE",
            "CLOB", "BLOB", "NCLOB", "BFILE",
            "RAW", "LONG", "LONG RAW", "ROWID", "UROWID",
            "REF", "RECORD", "TABLE", "VARRAY"
        };

        // Try to match pattern: variable_name DataType ...
        for (String dataType : dataTypes) {
            // Build regex to match: word_boundary + datatype + (optional params)
            String pattern = "^(\\w+)\\s+" + Pattern.quote(dataType) +
                           "(?:\\s*\\([^)]*\\))?(?:\\s+.*)?$";
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(cleanLine);

            if (m.find()) {
                String varName = m.group(1);

                // Validate variable name (should be valid identifier)
                if (!varName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                    continue;
                }

                // Extract full data type (including parameters like (100) or (10,2))
                String fullDataType = extractDataType(cleanLine, dataType);

                return new VariableEntry(varName, fullDataType, lineNumber);
            }
        }

        return null;
    }

    /**
     * Extracts the full data type specification from a line,
     * including any parameters in parentheses.
     *
     * @param line the variable declaration line
     * @param baseType the base data type found
     * @return the full data type string (e.g., "VARCHAR2(100)")
     */
    private static String extractDataType(String line, String baseType) {
        // Find where the base type starts
        int typeStartIdx = line.toUpperCase().indexOf(baseType.toUpperCase());
        if (typeStartIdx < 0) {
            return baseType;
        }

        // Find the end of the type specification (including parameters)
        int idx = typeStartIdx + baseType.length();
        StringBuilder typeBuilder = new StringBuilder(baseType);

        // Skip whitespace
        while (idx < line.length() && Character.isWhitespace(line.charAt(idx))) {
            idx++;
        }

        // Check for parentheses (like VARCHAR2(100))
        if (idx < line.length() && line.charAt(idx) == '(') {
            int parenCount = 0;
            int startParen = idx;
            while (idx < line.length()) {
                char c = line.charAt(idx);
                if (c == '(') {
                    parenCount++;
                } else if (c == ')') {
                    parenCount--;
                    if (parenCount == 0) {
                        // Include the parentheses in the type
                        typeBuilder.append(line.substring(startParen, idx + 1));
                        break;
                    }
                }
                idx++;
            }
        }

        return typeBuilder.toString();
    }

    /**
     * Helper class to represent a detected variable declaration.
     * Contains variable name, data type, and source line number.
     */
    private static final class VariableEntry {
        final String name;
        final String dataType;
        final int lineNumber;

        /**
         * Creates a new variable entry.
         *
         * @param name the variable name (e.g., "v_count")
         * @param dataType the data type (e.g., "NUMBER" or "VARCHAR2(100)")
         * @param lineNumber the 1-based line number in source code
         */
        VariableEntry(String name, String dataType, int lineNumber) {
            this.name = name;
            this.dataType = dataType;
            this.lineNumber = lineNumber;
        }

        @Override
        public String toString() {
            return "VariableEntry{" +
                   "name='" + name + '\'' +
                   ", dataType='" + dataType + '\'' +
                   ", lineNumber=" + lineNumber +
                   '}';
        }

        /**
         * Gets the variable name.
         * @return the name (e.g., "v_count")
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the data type specification.
         * @return the type (e.g., "NUMBER" or "VARCHAR2(100)")
         */
        public String getDataType() {
            return dataType;
        }

        /**
         * Gets the source line number where this variable is declared.
         * @return 1-based line number
         */
        public int getLineNumber() {
            return lineNumber;
        }
    }
}
