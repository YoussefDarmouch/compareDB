
package services;
import Package.ExportExcel;

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

        int[] widths = {10, 20, 16, 16};

        String[] headers = {"Table", "Column", db1Name, db2Name};

        printHorizontalBorder(widths);
        printRow(headers, widths);
        printHorizontalBorder(widths);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            String table = entry.getKey();

            for (String line : entry.getValue()) {

                String[] parts = parseTypeLine(line);
                if (parts != null) {

                    System.out.print("| " + pad(table, widths[0]) + " | "
                            + pad(parts[0], widths[1]) + " | "
                            + RED + pad(parts[1], widths[2]) + RESET + " | "
                            + RED + pad(parts[2], widths[3]) + RESET + " |\n");

                    printHorizontalBorder(widths);
                }
            }
        }
ExportExcel.exportTypeComparison(result, db1Name, db2Name);
    }

    // ─── 2. COLUMNS ────────────────────────
    public static void printColumnComparison(
            Map<String, List<String>> result,
            String db1Name,
            String db2Name) {

        System.out.println("\n" + BOLD + "--- Column Differences ---" + RESET);

        int[] widths = {12, 25, 25};

        String[] headers = {"Table", "Column", "Exists in"};

        printHorizontalBorder(widths);
        printRow(headers, widths);
        printHorizontalBorder(widths);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            String table = entry.getKey();

            for (String line : entry.getValue()) {

                String[] parts = parseColumnLine(line, db1Name, db2Name);

                if (parts != null) {

                    String color = parts[1].contains(db1Name) ? YELLOW : CYAN;

                    System.out.print("| " + pad(table, widths[0]) + " | "
                            + pad(parts[0], widths[1]) + " | "
                            + color + pad(parts[1], widths[2]) + RESET + " |\n");

                    printHorizontalBorder(widths);
                }
            }
        }
      ExportExcel.exportColumnComparison(result,db1Name,db2Name);
    }


    public static void printDataComparison(
            Map<String, List<String>> result,
            String db1Name,
            String db2Name) {

        System.out.println("\n" + BOLD + "--- Data Differences ---" + RESET);

        int[] widths = {12, 10, 20, 20, 20};

        String[] headers = {"Table", "Row", "Column", db1Name, db2Name};

        printHorizontalBorder(widths);
        printRow(headers, widths);
        printHorizontalBorder(widths);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            String table = entry.getKey();

            for (String line : entry.getValue()) {

                String[] parts = parseDataLine(line);

                if (parts != null) {

                    System.out.print("| "
                            + pad(table, widths[0]) + " | "
                            + pad(parts[0], widths[1]) + " | "
                            + pad(parts[1], widths[2]) + " | "
                            + RED + pad(parts[2], widths[3]) + RESET + " | "
                            + RED + pad(parts[3], widths[4]) + RESET + " |\n");

                    printHorizontalBorder(widths);
                }
            }
        }
        ExportExcel.exportDataComparison(result, db1Name, db2Name);

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
    public static void printComparisonFunction(Map<String, List<String>> result) {

        String separator = "--------------------------------------------------------------------------------";
        System.out.println("\n--- Function Differences ---");

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            System.out.println("Category: " + entry.getKey());

            for (String line : entry.getValue()) {
                System.out.println("   " + line);

            }

        }




        System.out.println(separator);

    }
    public static void printComparisonProcedures(Map<String, List<String>> result) {



        String separator = "--------------------------------------------------------------------------------";
        System.out.println("\n--- Procedure Differences ---");

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            System.out.println("Category: " + entry.getKey());

            for (String line : entry.getValue()) {
                System.out.println("   " + line);
            }
        }




        System.out.println(separator);
    }
    public static void printComparisonTrigger(Map<String, List<String>> result) {

        String separator = "--------------------------------------------------------------------------------";
        System.out.println("\n--- Trigger Differences ---");

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            System.out.println("Category: " + entry.getKey());

            for (String line : entry.getValue()) {
                System.out.println("   " + line);
            }
        }

        System.out.println(separator);

    }

    public static void compareTables(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        List<String> d1Tables = ShowTablesService.GetNameTable(db1Config);
        List<String> d2Tables = ShowTablesService.GetNameTable(db2Config);

        // 🔥 نجمعو جميع tables
        Set<String> allTables = new TreeSet<>();
        allTables.addAll(d1Tables);
        allTables.addAll(d2Tables);

        // 🔥 نحسبو max length
        int maxTableLength = "Table".length();
        for (String table : allTables) {
            if (table.length() > maxTableLength) {
                maxTableLength = table.length();
            }
        }

        int statusLength = "Exists in".length();

        String format = "| %-" + maxTableLength + "s | %-" + statusLength + "s |%n";

        String line = "+"
                + repeatChar('-', maxTableLength + 2)
                + "+"
                + repeatChar('-', statusLength + 2)
                + "+";

        // HEADER
        System.out.println("\n========== COMPARE TABLES ==========");
        System.out.println(line);
        System.out.printf(format, "Table", "Exists in");
        System.out.println(line);

        // DATA
        for (String table : allTables) {

            String status;

            if (d1Tables.contains(table) && d2Tables.contains(table)) {
                status = db1 + " & " + db2;
            } else if (d1Tables.contains(table)) {
                status = db1 + " only";
            } else {
                status = db2 + " only";
            }

            System.out.printf(format, table, status);
            System.out.println(line);
        }
    }

}