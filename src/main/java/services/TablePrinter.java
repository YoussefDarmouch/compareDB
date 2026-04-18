
package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

import java.util.TreeSet;


public class TablePrinter {

    // ─── Colors ─────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String GREEN  = "\u001B[32m";
    private static final String BOLD   = "\u001B[1m";

    // ─── 1. TYPE ───────────────────────────
    public static void printTypeComparison(
            Map<String, List<String>> result,
            String db1Name,
            String db2Name) {

        System.out.println("\n" + BOLD + "--- Type Differences ---" + RESET);

        int[] widths = {30, 22, 24, 24, 10, 28};
        String[] headers = ComparisonOutputUtils.comparisonHeaders("COLUMN_NAME", db1Name, db2Name);

        printHorizontalBorder(widths);
        printRow(headers, widths);
        printHorizontalBorder(widths);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            String table = entry.getKey();

            if ("info".equalsIgnoreCase(table)) {
                for (String info : entry.getValue()) {
                    System.out.println(info);
                }
                continue;
            }

            for (String line : entry.getValue()) {

                String[] parts = parseTypeLine(line);
                if (parts != null) {
                    String[] row = ComparisonOutputUtils.typeRow(table, parts[0], parts[1], parts[2]);
                    printGenericRow(row, widths, 1, 2, 3, 4);

                    printHorizontalBorder(widths);
                }
            }
        }
    }

    // ─── 2. COLUMNS ────────────────────────
    public static void printColumnComparison(
            Map<String, List<String>> result,
            String db1Name,
            String db2Name) {

        System.out.println("\n" + BOLD + "--- Column Differences ---" + RESET);

        int[] widths = {30, 22, 14, 14, 10, 28};
        String[] headers = ComparisonOutputUtils.comparisonHeaders("COLUMN_NAME", db1Name, db2Name);

        printHorizontalBorder(widths);
        printRow(headers, widths);
        printHorizontalBorder(widths);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            String table = entry.getKey();

            if ("info".equalsIgnoreCase(table)) {
                for (String info : entry.getValue()) {
                    System.out.println(info);
                }
                continue;
            }

            for (String line : entry.getValue()) {

                String[] parts = parseColumnLine(line, db1Name, db2Name);

                if (parts != null) {
                    String[] row = ComparisonOutputUtils.columnRow(table, parts[0], parts[1], db1Name, db2Name);
                    printGenericRow(row, widths, 1, 2, 3, 4);

                    printHorizontalBorder(widths);
                }
            }
        }
    }


    public static void printDataComparison(
            Map<String, List<String>> result,
            String db1Name,
            String db2Name) {

        System.out.println("\n" + BOLD + "--- Data Differences ---" + RESET);

        int[] widths = {34, 16, 22, 22, 10, 32};
        String[] headers = ComparisonOutputUtils.comparisonHeaders("OBJECT_NAME", db1Name, db2Name);

        printHorizontalBorder(widths);
        printRow(headers, widths);
        printHorizontalBorder(widths);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            String table = entry.getKey();

            if ("info".equalsIgnoreCase(table)) {
                for (String info : entry.getValue()) {
                    System.out.println(info);
                }
                continue;
            }

            for (String line : entry.getValue()) {

                String[] parts = parseDataLine(line);

                if (parts != null) {
                    String[] row = ComparisonOutputUtils.dataRow(table, parts[0], parts[1], parts[2], parts[3]);
                    printGenericRow(row, widths, 1, 2, 3, 4);

                    printHorizontalBorder(widths);
                }
            }
        }

    }


    public static void printSharedTables(List<String> tables) {

        System.out.println("\n" + BOLD + "--- Shared Tables ---" + RESET);

        if (tables == null || tables.isEmpty()) {
            System.out.println("No shared tables.");
            return;
        }

        int max = 10;
        for (String t : tables) max = Math.max(max, t.length());

        int[] widths = {4, max};
        String[] headers = {"#", "Table"};

        printHorizontalBorder(widths);
        printRow(headers, widths);
        printHorizontalBorder(widths);

        for (int i = 0; i < tables.size(); i++) {
            System.out.print("| " + pad(String.valueOf(i + 1), widths[0]) + " | "
                    + GREEN + pad(tables.get(i), widths[1]) + RESET + " |\n");
            printHorizontalBorder(widths);
        }
    }

    private static void printHorizontalBorder(int[] widths) {
        StringBuilder sb = new StringBuilder("+");

        for (int w : widths) {
            sb.append(repeatChar('-', w + 2)).append("+");
        }

        System.out.println(sb.toString());
    }

    private static void printRow(String[] row, int[] widths) {
        StringBuilder sb = new StringBuilder("|");

        for (int i = 0; i < row.length; i++) {
            sb.append(" ").append(pad(row[i], widths[i])).append(" |");
        }

        System.out.println(sb.toString());
    }

    private static String pad(String s, int width) {
        if (s == null) s = "null";

        if (s.length() >= width) {
            return s.substring(0, width);
        }

        return s + repeatChar(' ', width - s.length());
    }

    private static String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    // ─── Parsers ──────────────────────────

    private static String[] parseTypeLine(String line) {
        try {
            String after = line.substring(line.indexOf("Column ") + 7);
            String col = after.substring(0, after.indexOf(" =>")).trim();

            String rest = after.substring(after.indexOf("=>") + 2);
            String[] parts = rest.split("\\|");

            String t1 = parts[0].substring(parts[0].indexOf(":") + 1).trim();
            String t2 = parts[1].substring(parts[1].indexOf(":") + 1).trim();

            return new String[]{col, t1, t2};

        } catch (Exception e) {
            return null;
        }
    }

    private static String[] parseColumnLine(String line, String db1, String db2) {
        try {
            String col = line.substring(line.lastIndexOf(":") + 1).trim();
            String exists = line.contains(db1) ? db1 + " only" : db2 + " only";
            return new String[]{col, exists};
        } catch (Exception e) {
            return null;
        }
    }




    private static String[] parseDataLine(String line) {
        try {
            String row = line.substring(line.indexOf("Row ") + 4, line.indexOf(",")).trim();
            String col = line.substring(line.indexOf("column ") + 7, line.indexOf(" differs")).trim();

            String rest = line.substring(line.indexOf("differs:") + 8);
            String[] parts = rest.split("\\|");

            String v1 = parts[0].substring(parts[0].indexOf("=") + 1).trim();
            String v2 = parts[1].substring(parts[1].indexOf("=") + 1).trim();

            return new String[]{row, col, v1, v2};

        } catch (Exception e) {
            return null;
        }
    }
    public static void printComparisonFunction(Map<String, List<String>> result, String db1Name, String db2Name) {
        printObjectComparison("Function Differences", result, db1Name, db2Name);
    }
    public static void printComparisonProcedures(Map<String, List<String>> result, String db1Name, String db2Name) {
        printObjectComparison("Procedure Differences", result, db1Name, db2Name);
    }
    public static void printComparisonTrigger(Map<String, List<String>> result, String db1Name, String db2Name) {
        printObjectComparison("Trigger Differences", result, db1Name, db2Name);
    }

    // ─── Rich diff display (direct from DbObjectDiff list) ────────────────────

    public static void printDiffsFunction(List<DbObjectDiff> diffs, String db1Name, String db2Name) {
        printObjectDiffs("Function Differences", "FUNCTION", diffs, db1Name, db2Name, false);
    }
    public static void printDiffsProcedure(List<DbObjectDiff> diffs, String db1Name, String db2Name) {
        printObjectDiffs("Procedure Differences", "PROCEDURE", diffs, db1Name, db2Name, false);
    }
    public static void printDiffsTrigger(List<DbObjectDiff> diffs, String db1Name, String db2Name) {
        printObjectDiffs("Trigger Differences", "TRIGGER", diffs, db1Name, db2Name, false);
    }
    public static void printDiffsPackage(List<DbObjectDiff> diffs, String db1Name, String db2Name) {
        printObjectDiffs("Package Differences", "PACKAGE", diffs, db1Name, db2Name, true);
    }

    private static void printObjectDiffs(String title, String objectLabel,
                                         List<DbObjectDiff> diffs,
                                         String db1Name, String db2Name,
                                         boolean packageMode) {
        System.out.println("\n" + BOLD + "--- " + title + " ---" + RESET);

        int[] widths = packageMode
                ? new int[]{22, 14, 16, 16, 8, 70}
                : new int[]{22, 14, 16, 16, 8, 18, 50};
        String[] headers = packageMode
                ? new String[]{objectLabel, "STATUS", db1Name, db2Name, "IMPACT", "DETAILS"}
                : new String[]{objectLabel, "STATUS", db1Name, db2Name, "IMPACT", "EXEC_RESULT", "DETAILS"};

        printHorizontalBorder(widths);
        printRow(headers, widths);
        printHorizontalBorder(widths);

        if (diffs.isEmpty()) {
            printGenericRow(packageMode
                    ? new String[]{"(no differences)", "", "", "", "", ""}
                    : new String[]{"(no differences)", "", "", "", "", "", ""}, widths);
            printHorizontalBorder(widths);
            return;
        }

        for (DbObjectDiff diff : diffs) {
            String[] row = packageMode
                    ? buildPackageRow(diff, db1Name, db2Name)
                    : buildFuncRow(diff, db1Name, db2Name);
            printGenericRow(row, widths, packageMode ? new int[]{1, 2, 3, 4} : new int[]{1, 2, 3, 4, 5});
            printHorizontalBorder(widths);
        }
    }

    // ─── Row builders for rich display ────────────────────────────────────────

    private static String[] buildFuncRow(DbObjectDiff diff, String db1Name, String db2Name) {
        String name       = diff.getObjectName();
        String status     = resolveStatusLabel(diff, db1Name, db2Name);
        String db1Summary = buildObjDbCell(diff, true, false);
        String db2Summary = buildObjDbCell(diff, false, false);
        String impact     = buildObjImpact(diff);
        String execResult = diff.getExecutionResult();
        String details    = buildFuncDetails(diff, db1Name, db2Name);
        return new String[]{name, status, db1Summary, db2Summary, impact, execResult, details};
    }

    private static String[] buildPackageRow(DbObjectDiff diff, String db1Name, String db2Name) {
        String name       = diff.getObjectName();
        String status     = resolveStatusLabel(diff, db1Name, db2Name);
        String db1Summary = buildObjDbCell(diff, true, false);
        String db2Summary = buildObjDbCell(diff, false, false);
        String impact     = buildObjImpact(diff);
        String details    = buildPackageDetails(diff, db1Name, db2Name);
        return new String[]{name, status, db1Summary, db2Summary, impact, details};
    }

    private static String resolveStatusLabel(DbObjectDiff diff, String db1Name, String db2Name) {
        switch (diff.getChangeType()) {
            case ADDED:    return "ONLY IN " + db1Name;
            case REMOVED:  return "ONLY IN " + db2Name;
            case MODIFIED: return "DIFFERENT";
            default:       return diff.getChangeType().name();
        }
    }

    private static String buildObjDbCell(DbObjectDiff diff, boolean leftSide, boolean ignored) {
        if (diff.getChangeType() == DbObjectDiff.ChangeType.ADDED) {
            if (leftSide) {
                int n = diff.getDb1SourceLines();
                return n > 0 ? "Present (" + n + " lines)" : "PRESENT";
            }
            return "MISSING";
        }
        if (diff.getChangeType() == DbObjectDiff.ChangeType.REMOVED) {
            if (!leftSide) {
                int n = diff.getDb2SourceLines();
                return n > 0 ? "Present (" + n + " lines)" : "PRESENT";
            }
            return "MISSING";
        }
        int n = leftSide ? diff.getDb1SourceLines() : diff.getDb2SourceLines();
        return n > 0 ? "Src " + n + " lines" : "UNCHANGED";
    }

    private static String buildObjImpact(DbObjectDiff diff) {
        if (diff.getChangeType() != DbObjectDiff.ChangeType.MODIFIED) return "HIGH";
        if (diff.getSourceChangesCount() >= 5 || diff.getParameterChangesCount() >= 2) return "HIGH";
        if (diff.getSourceChangesCount() > 0  || diff.getParameterChangesCount() > 0)  return "MEDIUM";
        return "LOW";
    }

    /** Details for FUNCTION / PROCEDURE / TRIGGER rows. */
    private static String buildFuncDetails(DbObjectDiff diff, String db1Name, String db2Name) {
        if (diff.getChangeType() == DbObjectDiff.ChangeType.ADDED) {
            return "Object exists only in " + db1Name;
        }
        if (diff.getChangeType() == DbObjectDiff.ChangeType.REMOVED) {
            return "Object exists only in " + db2Name;
        }
        StringBuilder sb = new StringBuilder();

        // Similarity score
        if (diff.getSimilarity() >= 0) {
            sb.append("Similarity: ").append(String.format("%.0f%%", diff.getSimilarity() * 100)).append("\n");
        }

        if (!diff.getSourceDiffs().isEmpty()) {
            sb.append("Changed lines: ");
            collectLineRanges(sb, diff.getSourceDiffs());
            // Show actual value differences with change type (up to 4 lines)
            int shown = 0;
            for (DbObjectDiff.SourceDiffLine dl : diff.getSourceDiffs()) {
                if (shown >= 4) break;
                String tag = dl.getChangeType() != null ? dl.getChangeType() : "DIFF";
                sb.append("\n  L").append(dl.getLineNumber()).append(" [").append(tag).append("] ");
                if ("ADDED".equals(dl.getChangeType())) {
                    sb.append(db2Name).append(": ").append(trimSafe(dl.getNewLine()));
                } else if ("REMOVED".equals(dl.getChangeType())) {
                    sb.append(db1Name).append(": ").append(trimSafe(dl.getOldLine()));
                } else {
                    sb.append(db1Name).append(": ").append(trimSafe(dl.getOldLine()));
                    sb.append("\n         ").append(db2Name).append(": ").append(trimSafe(dl.getNewLine()));
                    if (dl.getCharChangeSummary() != null) {
                        sb.append("\n         changes: ").append(trimSafe(dl.getCharChangeSummary()));
                    }
                }
                shown++;
            }
            if (diff.getSourceDiffs().size() > 4) {
                sb.append("\n  ... and ").append(diff.getSourceDiffs().size() - 4).append(" more");
            }
        }
        if (!diff.getParameterDiffs().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Parameters:");
            for (DbObjectDiff.ParameterDiffLine p : diff.getParameterDiffs()) {
                sb.append("\n- ").append(p.getParameterName());
                String oldSig = compactSig(p.getOldMode(), p.getOldDataType());
                String newSig = compactSig(p.getNewMode(), p.getNewDataType());
                if (!oldSig.isEmpty() || !newSig.isEmpty()) {
                    sb.append(": ").append(oldSig.isEmpty() ? "-" : oldSig)
                      .append(" -> ").append(newSig.isEmpty() ? "-" : newSig);
                }
            }
        }
        return sb.length() == 0 ? "No detailed differences captured" : sb.toString();
    }

    private static String buildPackageDetails(DbObjectDiff diff, String db1Name, String db2Name) {
        StringBuilder sb = new StringBuilder();

        // ===== ADDED / REMOVED =====
        if (diff.getChangeType() == DbObjectDiff.ChangeType.ADDED
                || diff.getChangeType() == DbObjectDiff.ChangeType.REMOVED) {

            String targetDb = diff.getChangeType() == DbObjectDiff.ChangeType.ADDED ? db2Name : db1Name;

            sb.append("Object exists only in ")
                    .append(diff.getChangeType() == DbObjectDiff.ChangeType.ADDED ? db1Name : db2Name);

            if (diff.getSubProgramDiffs().isEmpty()) {
                sb.append("\n(no sub-programs detected)");
            } else {
                sb.append("\nContains:");
                for (DbObjectDiff.SubProgramDiff sp : diff.getSubProgramDiffs()) {
                    sb.append("\n- ").append(sp.getName())
                            .append(" (").append(sp.getType()).append(")");
                }
            }

            sb.append("\nImpact reason: missing in ").append(targetDb);
            return sb.toString();
        }

        // ===== SIMILARITY =====
        if (diff.getSimilarity() >= 0) {
            sb.append("Similarity: ")
                    .append(String.format("%.0f%%", diff.getSimilarity() * 100));
        }

        // ===== MAP LINE -> DIFF =====
        Map<Integer, DbObjectDiff.SourceDiffLine> diffByLine = new LinkedHashMap<>();
        for (DbObjectDiff.SourceDiffLine dl : diff.getSourceDiffs()) {
            diffByLine.put(dl.getLineNumber(), dl);
        }

        List<DbObjectDiff.SubProgramDiff> subs = diff.getSubProgramDiffs();

        // ===== NO SUBPROGRAM =====
        if (subs == null || subs.isEmpty()) {
            sb.append("\nChanged lines: ");
            collectLineRanges(sb, diff.getSourceDiffs());

            int shown = 0;
            for (DbObjectDiff.SourceDiffLine dl : diff.getSourceDiffs()) {

                if (shown >= 5) break;

                sb.append("\n  Line ").append(dl.getLineNumber());

                if (dl.getOldLine() != null) {
                    sb.append("\n    D1 : ").append(trimSafe(dl.getOldLine()));
                }

                if (dl.getNewLine() != null) {
                    sb.append("\n    D2 : ").append(trimSafe(dl.getNewLine()));
                }

                shown++;
            }

            return sb.toString();
        }

        // ===== COUNT =====
        int changedCount = 0;
        int sameCount = 0;

        for (DbObjectDiff.SubProgramDiff sp : subs) {
            if ("DIFFERENT".equals(sp.getStatus())) changedCount++;
            else sameCount++;
        }

        sb.append("\nSub-programs: ")
                .append(changedCount).append(" changed, ")
                .append(sameCount).append(" unchanged");

        // ===== LOOP SUBPROGRAMS =====
        for (DbObjectDiff.SubProgramDiff sp : subs) {

            if (!"DIFFERENT".equals(sp.getStatus())) continue;

            sb.append("\n[").append(sp.getType()).append("] ")
                    .append(sp.getName())
                    .append(" (lines ")
                    .append(sp.getStartLine()).append("-")
                    .append(sp.getEndLine()).append(")");

            if (sp.getChangedLines() != null && !sp.getChangedLines().isEmpty()) {

                sb.append("\n  changed lines: ");
                appendLineList(sb, sp.getChangedLines());

                int shown = 0;

                for (int ln : sp.getChangedLines()) {

                    DbObjectDiff.SourceDiffLine dl = diffByLine.get(ln);

                    if (dl != null ) {

                        sb.append("\n    Line ").append(ln);

                        if (dl.getOldLine() != null) {
                            sb.append("\n      D1 : ")
                                    .append(trimSafe(dl.getOldLine()));
                        }

                        if (dl.getNewLine() != null) {
                            sb.append("\n      D2 : ")
                                    .append(trimSafe(dl.getNewLine()));
                        }

                        if (dl.getCharChangeSummary() != null
                                && !dl.getCharChangeSummary().isEmpty()) {

                            sb.append("\n      Diff : ")
                                    .append(trimSafe(dl.getCharChangeSummary()));
                        }

                        shown++;
                    }
                }


            }
        }

        return sb.toString();
    }

    /** Show a sample of actual source differences with change types (up to maxLines). */
    private static void appendSourceSampleRich(StringBuilder sb,
                                               List<DbObjectDiff.SourceDiffLine> diffs,
                                               String db1Name, String db2Name, int maxLines) {
        if (diffs == null || diffs.isEmpty()) return;
        int count = Math.min(diffs.size(), maxLines);
        for (int i = 0; i < count; i++) {
            DbObjectDiff.SourceDiffLine dl = diffs.get(i);
            String tag = dl.getChangeType() != null ? dl.getChangeType() : "DIFF";
            sb.append("\n  L").append(dl.getLineNumber()).append(" [").append(tag).append("] ");
            if ("ADDED".equals(dl.getChangeType())) {
                sb.append(db2Name).append(": ").append(trimSafe(dl.getNewLine()));
            } else if ("REMOVED".equals(dl.getChangeType())) {
                sb.append(db1Name).append(": ").append(trimSafe(dl.getOldLine()));
            } else {
                sb.append(db1Name).append(": ").append(trimSafe(dl.getOldLine()));
                sb.append("\n         ").append(db2Name).append(": ").append(trimSafe(dl.getNewLine()));
                if (dl.getCharChangeSummary() != null) {
                    sb.append("\n         tokens: ").append(trimSafe(dl.getCharChangeSummary()));
                }
            }
        }
        if (diffs.size() > maxLines) {
            sb.append("\n  ... and ").append(diffs.size() - maxLines).append(" more");
        }
    }

    /** Trim and truncate a source line for display. */
    private static String trimSafe(String line) {
        if (line == null) return "<none>";
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return "<empty>";
        if (trimmed.length() > 60) return trimmed.substring(0, 57) + "...";
        return trimmed;
    }

    private static void collectLineRanges(StringBuilder sb, List<DbObjectDiff.SourceDiffLine> diffs) {
        // Compress consecutive line numbers into ranges (e.g. 12,13,14 → 12-14)
        if (diffs.isEmpty()) return;
        List<Integer> nums = new ArrayList<Integer>();
        for (DbObjectDiff.SourceDiffLine d : diffs) nums.add(d.getLineNumber());
        appendLineList(sb, nums);
    }

    private static void appendLineList(StringBuilder sb, List<Integer> nums) {
        if (nums == null || nums.isEmpty()) return;
        List<Integer> sorted = new ArrayList<Integer>(nums);
        Collections.sort(sorted);
        int start = sorted.get(0), prev = start;
        for (int i = 1; i <= sorted.size(); i++) {
            int cur = i < sorted.size() ? sorted.get(i) : -1;
            if (cur == prev + 1) { prev = cur; continue; }
            if (sb.length() > 0) {
                char last = sb.charAt(sb.length() - 1);
                if (last != ' ' && last != ':') sb.append(", ");
            }
            if (start == prev) sb.append(start);
            else sb.append(start).append("-").append(prev);
            start = cur; prev = cur;
        }
    }

    private static String compactSig(String mode, String type) {
        String m = mode != null ? mode.trim() : "";
        String t = type != null ? type.trim() : "";
        if (m.isEmpty()) return t;
        if (t.isEmpty()) return m;
        return m + " " + t;
    }

    // ─── Existing map-based methods (kept for Excel export) ──────────────────

    private static void printObjectComparison(String title,
                                              Map<String, List<String>> result,
                                              String db1Name,
                                              String db2Name) {
        System.out.println("\n" + BOLD + "--- " + title + " ---" + RESET);

        int[] widths = {28, 24, 24, 24, 10, 38};
        String[] headers = ComparisonOutputUtils.comparisonHeaders(objectHeaderForTitle(title), db1Name, db2Name);

        printHorizontalBorder(widths);
        printRow(headers, widths);
        printHorizontalBorder(widths);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            if ("info".equalsIgnoreCase(entry.getKey())) {
                for (String info : entry.getValue()) {
                    printGenericRow(ComparisonOutputUtils.objectInfoRow(info), widths, 1, 4);
                    printHorizontalBorder(widths);
                }
                continue;
            }

            for (String line : entry.getValue()) {
                String[] parts = parseObjectLine(line);
                if (parts == null) {
                    continue;
                }

                String color = parts[1].startsWith("ONLY IN")
                        ? YELLOW
                        : ("DIFFERENT".equals(parts[1]) ? RED : GREEN);

                printGenericRow(parts, widths, 1, 2, 3, 4);
                printHorizontalBorder(widths);
            }
        }
        
    }




    private static List<String> wrapTextSmart(String text, int width) {
        List<String> lines = new java.util.ArrayList<>();

        if (text == null) {
            lines.add("null");
            return lines;
        }

        if (width <= 0) {
            lines.add("");
            return lines;
        }

        // First split on explicit newlines, then word-wrap each segment
        String[] segments = text.split("\n", -1);
        for (String segment : segments) {
            if (segment.isEmpty()) {
                lines.add("");
                continue;
            }
            String[] words = segment.split(" ");
            StringBuilder current = new StringBuilder();

            for (String word : words) {
                if (word == null) word = "";

                // If the word itself is longer than width, hard-wrap it in chunks.
                if (word.length() > width) {
                    // Flush current line first
                    if (current.length() > 0) {
                        lines.add(current.toString());
                        current.setLength(0);
                    }

                    int idx = 0;
                    while (idx < word.length()) {
                        int end = Math.min(idx + width, word.length());
                        lines.add(word.substring(idx, end));
                        idx = end;
                    }
                    continue;
                }

                int extra = (current.length() > 0 ? 1 : 0) + word.length();
                if (current.length() + extra <= width) {
                    if (current.length() > 0) current.append(" ");
                    current.append(word);
                    continue;
                }

                if (current.length() > 0) lines.add(current.toString());
                current = new StringBuilder(word);
            }

            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }

        return lines;
    }
    private static void printGenericRow(String[] row, int[] widths, int... colorColumns) {

    List<List<String>> wrappedColumns = new java.util.ArrayList<>();
    int maxHeight = 0;

    // wrap all columns
    for (int i = 0; i < widths.length; i++) {
        String cell = i < row.length ? row[i] : "";
        List<String> wrapped = wrapTextSmart(cell, widths[i]);
        wrappedColumns.add(wrapped);
        maxHeight = Math.max(maxHeight, wrapped.size());
    }

    // print each visual line
    for (int line = 0; line < maxHeight; line++) {
        StringBuilder sb = new StringBuilder("|");

        for (int col = 0; col < widths.length; col++) {

            String value = "";
            if (line < wrappedColumns.get(col).size()) {
                value = wrappedColumns.get(col).get(line);
            }

            String color = null;
            for (int c : colorColumns) {
                if (col == c) {
                    color = colorForValue(value);
                    break;
                }
            }

            sb.append(" ");

            if (color != null) {
                sb.append(color).append(pad(value, widths[col])).append(RESET);
            } else {
                sb.append(pad(value, widths[col]));
            }

            sb.append(" |");
        }

        System.out.println(sb.toString());
    }
}
    // private static void printGenericRow(String[] row,
    //                                     int[] widths,
    //                                     int... colorColumns) {
    //     StringBuilder sb = new StringBuilder("|");

    //     for (int i = 0; i < row.length && i < widths.length; i++) {
    //         String color = null;
    //         for (int colorColumn : colorColumns) {
    //             if (i == colorColumn) {
    //                 color = colorForValue(row[i]);
    //                 break;
    //             }
    //         }

    //         sb.append(" ");
    //         if (color != null) {
    //             sb.append(color).append(pad(row[i], widths[i])).append(RESET);
    //         } else {
    //             sb.append(pad(row[i], widths[i]));
    //         }
    //         sb.append(" |");
    //     }

    //     System.out.println(sb.toString());
    // }

    private static String colorForValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.toUpperCase();
        if (normalized.contains("HIGH") || normalized.startsWith("ONLY IN") || normalized.contains("MISSING") || normalized.contains("DIFFERENT")) {
            return RED;
        }
        if (normalized.contains("MEDIUM")) {
            return YELLOW;
        }
        if (normalized.contains("LOW") || normalized.contains("SHARED") || normalized.contains("PRESENT")) {
            return GREEN;
        }
        return null;
    }

    private static String[] parseObjectLine(String line) {
        if (line == null) {
            return null;
        }

        String[] parts = line.split("\\|", 6);
        if (parts.length < 2) {
            return null;
        }

        String db1Summary = parts.length >= 3 ? parts[2].trim() : "-";
        String db2Summary = parts.length >= 4 ? parts[3].trim() : "-";
        String impact = parts.length >= 5 ? parts[4].trim() : "LOW";
        String details = parts.length >= 6 ? parts[5].trim() : "";
        return new String[]{parts[0].trim(), parts[1].trim(), db1Summary, db2Summary, impact, details};
    }

    private static String objectHeaderForTitle(String title) {
        if (title == null) {
            return "Object";
        }

        if (title.startsWith("Function")) return "Function";
        if (title.startsWith("Procedure")) return "Procedure";
        if (title.startsWith("Trigger")) return "Trigger";
        if (title.startsWith("Package")) return "Package";
        return "Object";
    }

    public static void compareTables(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        String db1 = DbLabelUtils.displayName(db1Config);
        String db2 = DbLabelUtils.displayName(db2Config);

        List<String> d1Tables = ShowTablesService.GetNameTable(db1Config);
        List<String> d2Tables = ShowTablesService.GetNameTable(db2Config);

        Set<String> allTables = new TreeSet<>();
        allTables.addAll(d1Tables);
        allTables.addAll(d2Tables);


        int maxTableLength = "Table".length();
        for (String table : allTables) {
            if (table.length() > maxTableLength) {
                maxTableLength = table.length();
            }
        }

        int[] widths = {30, 22, 14, 14, 10, 28};

        System.out.println("\n========== COMPARE TABLES ==========");
        printHorizontalBorder(widths);
        printRow(ComparisonOutputUtils.comparisonHeaders("TABLE_NAME", db1, db2), widths);
        printHorizontalBorder(widths);

        // DATA
        for (String table : allTables) {

            String[] row = ComparisonOutputUtils.tableRow(table, d1Tables.contains(table), d2Tables.contains(table), db1, db2);
            printGenericRow(row, widths, 1, 2, 3, 4);
            printHorizontalBorder(widths);
        }
    }


}