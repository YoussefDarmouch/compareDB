
package services;

import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public static void printComparisonPackages(Map<String, List<String>> result, String db1Name, String db2Name) {
        printObjectComparison("Package Differences", result, db1Name, db2Name);

    }

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

    private static void printGenericRow(String[] row,
                                        int[] widths,
                                        int... colorColumns) {
        StringBuilder sb = new StringBuilder("|");

        for (int i = 0; i < row.length && i < widths.length; i++) {
            String color = null;
            for (int colorColumn : colorColumns) {
                if (i == colorColumn) {
                    color = colorForValue(row[i]);
                    break;
                }
            }

            sb.append(" ");
            if (color != null) {
                sb.append(color).append(pad(row[i], widths[i])).append(RESET);
            } else {
                sb.append(pad(row[i], widths[i]));
            }
            sb.append(" |");
        }

        System.out.println(sb.toString());
    }

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