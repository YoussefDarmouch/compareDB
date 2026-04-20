package Package;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.IndexedColorMap;

import services.DbConnectionFactory;
import services.ComparisonOutputUtils;
import services.DbLabelUtils;
import services.ShowTablesService;

public class ExportExcel {

    // ─── Color Hex Codes (matching TablePrinter ANSI colors) ───────────────
    private static final String RED_HEX    = "FF4444";
    private static final String YELLOW_HEX = "FFC000";
    private static final String CYAN_HEX   = "00B0C0";
    private static final String GREEN_HEX  = "00AA44";
    private static final String HEADER_BG  = "2F4F8F";
    private static final String HEADER_FG  = "FFFFFF";
    private static final String ALT_ROW_BG = "F2F2F2";

    // ─── Reusable style factory ─────────────────────────────────────────────
    private static CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(HEADER_BG), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(new XSSFColor(hexToBytes(HEADER_FG), null));
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle normalStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle coloredStyle(XSSFWorkbook wb, String hexColor) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(hexToBytes(hexColor), null));
        f.setBold(true);
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle altRowStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(ALT_ROW_BG), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle sectionTitleStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontName("Arial");
        f.setFontHeightInPoints((short) 13);
        s.setFont(f);
        return s;
    }

    // ─── Helper to set a cell value + style ────────────────────────────────
    private static void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    // ─── Auto-size all used columns in a sheet ──────────────────────────────
    private static void autoSize(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
            // add a small padding
            int width = sheet.getColumnWidth(i) + 1024;
            sheet.setColumnWidth(i, Math.min(width, 25600));
        }
    }

    // ─── Add a blank section title row ──────────────────────────────────────
    private static int addSectionTitle(Sheet sheet, XSSFWorkbook wb, int rowIdx, String title, int numCols) {
        Row titleRow = sheet.createRow(rowIdx++);
        titleRow.setHeightInPoints(20);
        Cell c = titleRow.createCell(0);
        c.setCellValue(title);
        c.setCellStyle(sectionTitleStyle(wb));
        if (numCols > 1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, numCols - 1));
        }
        return rowIdx;
    }

    // ─── Add a header row ───────────────────────────────────────────────────
    private static int addHeaderRow(Sheet sheet, XSSFWorkbook wb, int rowIdx, String[] headers) {
        Row headerRow = sheet.createRow(rowIdx++);
        headerRow.setHeightInPoints(16);
        CellStyle hs = headerStyle(wb);
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], hs);
        }
        return rowIdx;
    }

    // ─── 1. Type Differences Sheet ──────────────────────────────────────────
    private static void writeTypeSheet(XSSFWorkbook wb, Map<String, List<String>> result,
                                       String db1Name, String db2Name) {
        Sheet sheet = wb.createSheet("Type Differences");
        int rowIdx = 0;

        rowIdx = addSectionTitle(sheet, wb, rowIdx, "Type Differences", 6);
        rowIdx = addHeaderRow(sheet, wb, rowIdx, ComparisonOutputUtils.comparisonHeaders("COLUMN_NAME", db1Name, db2Name));

        CellStyle redStyle   = coloredStyle(wb, RED_HEX);
        CellStyle yellowStyle = coloredStyle(wb, YELLOW_HEX);
        CellStyle greenStyle = coloredStyle(wb, GREEN_HEX);
        CellStyle normalSt   = normalStyle(wb);
        CellStyle altSt      = altRowStyle(wb);

        int dataRowCount = 0;
        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            String table = entry.getKey();
            if ("info".equalsIgnoreCase(table)) {
                for (String info : entry.getValue()) {
                    Row row = sheet.createRow(rowIdx++);
                    setCell(row, 0, "-", normalSt);
                    setCell(row, 1, "INFO", yellowStyle);
                    setCell(row, 2, "-", normalSt);
                    setCell(row, 3, "-", normalSt);
                    setCell(row, 4, "LOW", greenStyle);
                    setCell(row, 5, info, normalSt);
                }
                continue;
            }
            for (String line : entry.getValue()) {
                String[] parts = parseTypeLine(line);
                if (parts != null) {
                    String[] displayRow = ComparisonOutputUtils.typeRow(table, parts[0], parts[1], parts[2]);
                    Row row = sheet.createRow(rowIdx++);
                    CellStyle bg = (dataRowCount % 2 == 0) ? normalSt : altSt;
                    setCell(row, 0, displayRow[0], bg);
                    setCell(row, 1, displayRow[1], redStyle);
                    setCell(row, 2, displayRow[2], bg);
                    setCell(row, 3, displayRow[3], bg);
                    setCell(row, 4, displayRow[4], yellowStyle);
                    setCell(row, 5, displayRow[5], bg);
                    dataRowCount++;
                }
            }
        }
        autoSize(sheet, 6);
    }

    // ─── Standalone: export type comparison directly to output/ folder ──────
    public static void exportTypeComparison(
            Map<String, List<String>> result,
            String db1Name,
            String db2Name) {

        java.io.File dir = new java.io.File("output");
        if (!dir.exists()) dir.mkdirs();

        String outputPath = "output/type_differences.xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            writeTypeSheet(wb, result, db1Name, db2Name);
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
            System.out.println("✅ Type differences exported to: " + outputPath);
        } catch (IOException e) {
            System.err.println("❌ Failed to export type differences: " + e.getMessage());
        }
    }

    // ─── Standalone: export data comparison directly to output/ folder ───────
    public static void exportDataComparison(
            Map<String, List<String>> result,
            String db1Name,
            String db2Name) {

        java.io.File dir = new java.io.File("output");
        if (!dir.exists()) dir.mkdirs();

        String outputPath = "output/data_differences.xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            writeDataSheet(wb, result, db1Name, db2Name);
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
            System.out.println("✅ Data differences exported to: " + outputPath);
        } catch (IOException e) {
            System.err.println("❌ Failed to export data differences: " + e.getMessage());
        }
    }

    // ─── Standalone: export column comparison directly to output/ folder ────
    /**
     * Call this directly from printColumnComparison() or wherever you run the column compare.
     * Saves  output/column_differences.xlsx  automatically.
     */
    public static void exportColumnComparison(
            Map<String, List<String>> result,
            String db1Name,
            String db2Name) {

        java.io.File dir = new java.io.File("output");
        if (!dir.exists()) dir.mkdirs();

        String outputPath = "output/column_differences.xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            writeColumnSheet(wb, result, db1Name, db2Name);
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
            System.out.println("✅ Column differences exported to: " + outputPath);
        } catch (IOException e) {
            System.err.println("❌ Failed to export column differences: " + e.getMessage());
        }
    }
    public static void exportTableComparison(
            List<String> d1Tables,
            List<String> d2Tables,
            String db1Name,
            String db2Name) {

        java.io.File dir = new java.io.File("output");
        if (!dir.exists()) dir.mkdirs();

        String outputPath = "output/table_differences.xlsx";

        // Build the sorted union of both table lists
        Set<String> allTables = new TreeSet<>();
        allTables.addAll(d1Tables);
        allTables.addAll(d2Tables);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            writeTableDiffSheet(wb, allTables, d1Tables, d2Tables, db1Name, db2Name);
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
            System.out.println("✅ Table differences exported to: " + outputPath);
        } catch (IOException e) {
            System.err.println("❌ Failed to export table differences: " + e.getMessage());
        }
    }

    // ─── 2. Column Differences Sheet ────────────────────────────────────────
    private static void writeColumnSheet(XSSFWorkbook wb, Map<String, List<String>> result,
                                         String db1Name, String db2Name) {
        Sheet sheet = wb.createSheet("Column Differences");
        int rowIdx = 0;

        rowIdx = addSectionTitle(sheet, wb, rowIdx, "Column Differences", 6);
        rowIdx = addHeaderRow(sheet, wb, rowIdx, ComparisonOutputUtils.comparisonHeaders("COLUMN_NAME", db1Name, db2Name));

        CellStyle yellowStyle = coloredStyle(wb, YELLOW_HEX);
        CellStyle cyanStyle   = coloredStyle(wb, CYAN_HEX);
        CellStyle redStyle    = coloredStyle(wb, RED_HEX);
        CellStyle greenStyle  = coloredStyle(wb, GREEN_HEX);
        CellStyle normalSt    = normalStyle(wb);
        CellStyle altSt       = altRowStyle(wb);

        int dataRowCount = 0;
        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            String table = entry.getKey();
            if ("info".equalsIgnoreCase(table)) {
                for (String info : entry.getValue()) {
                    Row row = sheet.createRow(rowIdx++);
                    setCell(row, 0, "-", normalSt);
                    setCell(row, 1, "INFO", yellowStyle);
                    setCell(row, 2, "-", normalSt);
                    setCell(row, 3, "-", normalSt);
                    setCell(row, 4, "LOW", greenStyle);
                    setCell(row, 5, info, normalSt);
                }
                continue;
            }
            for (String line : entry.getValue()) {
                String[] parts = parseColumnLine(line, db1Name, db2Name);
                if (parts != null) {
                    String[] displayRow = ComparisonOutputUtils.columnRow(table, parts[0], parts[1], db1Name, db2Name);
                    Row row = sheet.createRow(rowIdx++);
                    CellStyle bg = (dataRowCount % 2 == 0) ? normalSt : altSt;
                    setCell(row, 0, displayRow[0], bg);
                    setCell(row, 1, displayRow[1], redStyle);
                    setCell(row, 2, displayRow[2], "PRESENT".equals(displayRow[2]) ? yellowStyle : bg);
                    setCell(row, 3, displayRow[3], "PRESENT".equals(displayRow[3]) ? cyanStyle : bg);
                    setCell(row, 4, displayRow[4], redStyle);
                    setCell(row, 5, displayRow[5], bg);
                    dataRowCount++;
                }
            }
        }
        autoSize(sheet, 6);
    }

    // ─── 3. Data Differences Sheet ──────────────────────────────────────────
    private static void writeDataSheet(XSSFWorkbook wb, Map<String, List<String>> result,
                                       String db1Name, String db2Name) {
        Sheet sheet = wb.createSheet("Data Differences");
        int rowIdx = 0;

        rowIdx = addSectionTitle(sheet, wb, rowIdx, "Data Differences", 6);
        rowIdx = addHeaderRow(sheet, wb, rowIdx, ComparisonOutputUtils.comparisonHeaders("OBJECT_NAME", db1Name, db2Name));

        CellStyle redStyle = coloredStyle(wb, RED_HEX);
        CellStyle yellowStyle = coloredStyle(wb, YELLOW_HEX);
        CellStyle normalSt = normalStyle(wb);
        CellStyle altSt    = altRowStyle(wb);

        int dataRowCount = 0;
        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            String table = entry.getKey();
            if ("info".equalsIgnoreCase(table)) {
                for (String info : entry.getValue()) {
                    Row row = sheet.createRow(rowIdx++);
                    setCell(row, 0, "-", normalSt);
                    setCell(row, 1, "INFO", yellowStyle);
                    setCell(row, 2, "-", normalSt);
                    setCell(row, 3, "-", normalSt);
                    setCell(row, 4, "LOW", yellowStyle);
                    setCell(row, 5, info, normalSt);
                }
                continue;
            }
            for (String line : entry.getValue()) {
                String[] parts = parseDataLine(line);
                if (parts != null) {
                    String[] displayRow = ComparisonOutputUtils.dataRow(table, parts[0], parts[1], parts[2], parts[3]);
                    Row row = sheet.createRow(rowIdx++);
                    CellStyle bg = (dataRowCount % 2 == 0) ? normalSt : altSt;
                    setCell(row, 0, displayRow[0], bg);
                    setCell(row, 1, displayRow[1], redStyle);
                    setCell(row, 2, displayRow[2], bg);
                    setCell(row, 3, displayRow[3], bg);
                    setCell(row, 4, displayRow[4], yellowStyle);
                    setCell(row, 5, displayRow[5], bg);
                    dataRowCount++;
                }
            }
        }
        autoSize(sheet, 6);
    }

    // ─── 4. Table Differences Sheet ─────────────────────────────────────────
    // Receives allTables (sorted set), d1Tables, d2Tables — computes status cleanly
    private static void writeTableDiffSheet(XSSFWorkbook wb,
                                            java.util.Collection<String> allTables,
                                            List<String> d1Tables,
                                            List<String> d2Tables,
                                            String db1Name,
                                            String db2Name) {
        Sheet sheet = wb.createSheet("Table Differences");
        int rowIdx = 0;

        rowIdx = addSectionTitle(sheet, wb, rowIdx, "Table Differences: " + db1Name + " vs " + db2Name, 6);
        rowIdx = addHeaderRow(sheet, wb, rowIdx, ComparisonOutputUtils.comparisonHeaders("TABLE_NAME", db1Name, db2Name));

        CellStyle greenStyle  = coloredStyle(wb, GREEN_HEX);
        CellStyle yellowStyle = coloredStyle(wb, YELLOW_HEX);
        CellStyle cyanStyle   = coloredStyle(wb, CYAN_HEX);
        CellStyle redStyle    = coloredStyle(wb, RED_HEX);
        CellStyle normalSt    = normalStyle(wb);
        CellStyle altSt       = altRowStyle(wb);

        int i = 0;
        for (String table : allTables) {
            boolean inDb1 = d1Tables.contains(table);
            boolean inDb2 = d2Tables.contains(table);

            String[] displayRow = ComparisonOutputUtils.tableRow(table, inDb1, inDb2, db1Name, db2Name);
            CellStyle statusStyle = "HIGH".equals(displayRow[4]) ? redStyle : greenStyle;

            Row row = sheet.createRow(rowIdx++);
            CellStyle bg = (i % 2 == 0) ? normalSt : altSt;
            setCell(row, 0, displayRow[0], bg);
            setCell(row, 1, displayRow[1], statusStyle);
            setCell(row, 2, displayRow[2], inDb1 ? greenStyle : redStyle);
            setCell(row, 3, displayRow[3], inDb2 ? greenStyle : redStyle);
            setCell(row, 4, displayRow[4], "HIGH".equals(displayRow[4]) ? yellowStyle : greenStyle);
            setCell(row, 5, displayRow[5], bg);
            i++;
        }
        autoSize(sheet, 6);
    }

    // Keep old writeSharedTablesSheet for exportAll backward compat
    private static void writeSharedTablesSheet(XSSFWorkbook wb, List<String> tables,
                                               String db1Name, String db2Name) {
        writeTableDiffSheet(wb, tables, tables, new java.util.ArrayList<>(), db1Name, db2Name);
    }

    // ─── 5. Functions Sheet ──────────────────────────────────────────────────
    private static void writeFunctionSheet(XSSFWorkbook wb, Map<String, List<String>> result, String db1Name, String db2Name) {
        Sheet sheet = wb.createSheet("Function Differences");
        int rowIdx = 0;

        rowIdx = addSectionTitle(sheet, wb, rowIdx, "Function Differences", 6);
        rowIdx = addHeaderRow(sheet, wb, rowIdx, ComparisonOutputUtils.comparisonHeaders("FUNCTION_NAME", db1Name, db2Name));

        CellStyle normalSt = normalStyle(wb);
        CellStyle greenSt  = coloredStyle(wb, GREEN_HEX);
        CellStyle redSt    = coloredStyle(wb, RED_HEX);
        CellStyle yellowSt = coloredStyle(wb, YELLOW_HEX);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            if ("info".equalsIgnoreCase(entry.getKey())) {
                for (String info : entry.getValue()) {
                    Row row = sheet.createRow(rowIdx++);
                    String[] infoRow = ComparisonOutputUtils.objectInfoRow(info);
                    for (int i = 0; i < infoRow.length; i++) {
                        setCell(row, i, infoRow[i], i == 1 ? yellowSt : normalSt);
                    }
                }
                continue;
            }

            for (String line : entry.getValue()) {

                String[] parts = parseObjectLine(line);
                if (parts == null) continue;

                Row row = sheet.createRow(rowIdx++);

                CellStyle statusStyle = parts[1].equals("SAME")
                        ? greenSt
                        : (parts[1].startsWith("ONLY") ? yellowSt : redSt);

                setCell(row, 0, parts[0], normalSt);
                setCell(row, 1, parts[1], statusStyle);
                setCell(row, 2, parts[2], normalSt);
                setCell(row, 3, parts[3], normalSt);
                setCell(row, 4, parts[4], "HIGH".equals(parts[4]) ? redSt : yellowSt);
                setCell(row, 5, parts[5], normalSt);
            }
        }

        autoSize(sheet, 6);
    }

    public static void exportFunctionComparison(Map<String, List<String>> result) {

        File dir = new File("output");
        if (!dir.exists()) dir.mkdirs();

        String outputPath = "output/function_differences.xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            writeFunctionSheet(wb, result, "DB1", "DB2");

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }

            System.out.println("✅ Function differences exported to: " + outputPath);

        } catch (IOException e) {
            System.err.println("❌ Failed to export: " + e.getMessage());
        }
    }
    // ─── 6. Procedures Sheet ─────────────────────────────────────────────────
    private static void writeProcedureSheet(XSSFWorkbook wb, Map<String, List<String>> result, String db1Name, String db2Name) {
        Sheet sheet = wb.createSheet("Procedure Differences");
        int rowIdx = 0;

        rowIdx = addSectionTitle(sheet, wb, rowIdx, "Procedure Differences", 6);
        rowIdx = addHeaderRow(sheet, wb, rowIdx, ComparisonOutputUtils.comparisonHeaders("PROCEDURE_NAME", db1Name, db2Name));

        CellStyle normalSt = normalStyle(wb);
        CellStyle greenSt  = coloredStyle(wb, GREEN_HEX);
        CellStyle redSt    = coloredStyle(wb, RED_HEX);
        CellStyle yellowSt = coloredStyle(wb, YELLOW_HEX);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            if ("info".equalsIgnoreCase(entry.getKey())) {
                for (String info : entry.getValue()) {
                    Row row = sheet.createRow(rowIdx++);
                    String[] infoRow = ComparisonOutputUtils.objectInfoRow(info);
                    for (int i = 0; i < infoRow.length; i++) {
                        setCell(row, i, infoRow[i], i == 1 ? yellowSt : normalSt);
                    }
                }
                continue;
            }

            for (String line : entry.getValue()) {

                String[] parts = parseObjectLine(line);
                if (parts == null) continue;

                Row row = sheet.createRow(rowIdx++);

                CellStyle statusStyle = parts[1].equals("SAME")
                        ? greenSt
                        : (parts[1].startsWith("ONLY") ? yellowSt : redSt);

                setCell(row, 0, parts[0], normalSt);
                setCell(row, 1, parts[1], statusStyle);
                setCell(row, 2, parts[2], normalSt);
                setCell(row, 3, parts[3], normalSt);
                setCell(row, 4, parts[4], "HIGH".equals(parts[4]) ? redSt : yellowSt);
                setCell(row, 5, parts[5], normalSt);
            }
        }

        autoSize(sheet, 6);
    }
    public static void exportProcedureComparison(Map<String, List<String>> procedureResult) {

        File dir = new File("output");
        if (!dir.exists()) dir.mkdirs();

        String outputPath = "output/procedure_differences.xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            writeProcedureSheet(wb, procedureResult, "DB1", "DB2");

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }

            System.out.println("✅ Procedure Excel created: " + outputPath);

        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    // ─── 7. Triggers Sheet ───────────────────────────────────────────────────
    private static void writeTriggerSheet(XSSFWorkbook wb, Map<String, List<String>> result, String db1Name, String db2Name) {
        Sheet sheet = wb.createSheet("Trigger Differences");
        int rowIdx = 0;

        rowIdx = addSectionTitle(sheet, wb, rowIdx, "Trigger Differences", 6);
        rowIdx = addHeaderRow(sheet, wb, rowIdx, ComparisonOutputUtils.comparisonHeaders("TRIGGER_NAME", db1Name, db2Name));

        CellStyle normalSt = normalStyle(wb);
        CellStyle greenSt  = coloredStyle(wb, GREEN_HEX);
        CellStyle redSt    = coloredStyle(wb, RED_HEX);
        CellStyle yellowSt = coloredStyle(wb, YELLOW_HEX);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            if ("info".equalsIgnoreCase(entry.getKey())) {
                for (String info : entry.getValue()) {
                    Row row = sheet.createRow(rowIdx++);
                    String[] infoRow = ComparisonOutputUtils.objectInfoRow(info);
                    for (int i = 0; i < infoRow.length; i++) {
                        setCell(row, i, infoRow[i], i == 1 ? yellowSt : normalSt);
                    }
                }
                continue;
            }

            for (String line : entry.getValue()) {

                String[] parts = parseObjectLine(line);
                if (parts == null) continue;

                Row row = sheet.createRow(rowIdx++);

                CellStyle statusStyle = parts[1].equals("SAME")
                        ? greenSt
                        : (parts[1].startsWith("ONLY") ? yellowSt : redSt);

                setCell(row, 0, parts[0], normalSt);
                setCell(row, 1, parts[1], statusStyle);
                setCell(row, 2, parts[2], normalSt);
                setCell(row, 3, parts[3], normalSt);
                setCell(row, 4, parts[4], "HIGH".equals(parts[4]) ? redSt : yellowSt);
                setCell(row, 5, parts[5], normalSt);
            }
        }

        autoSize(sheet, 6);
    }
    public static void exportTriggerComparison(Map<String, List<String>> triggerResult) {

        File dir = new File("output");
        if (!dir.exists()) dir.mkdirs();

        String outputPath = "output/trigger_differences.xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            writeTriggerSheet(wb, triggerResult, "DB1", "DB2");

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }

            System.out.println("✅ Trigger Excel created: " + outputPath);

        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    // ─── 7b. Packages Sheet ──────────────────────────────────────────────────
    private static void writePackageSheet(XSSFWorkbook wb, Map<String, List<String>> result, String db1Name, String db2Name) {
        Sheet sheet = wb.createSheet("Package Differences");
        int rowIdx = 0;

        rowIdx = addSectionTitle(sheet, wb, rowIdx, "Package Differences", 6);
        rowIdx = addHeaderRow(sheet, wb, rowIdx, ComparisonOutputUtils.comparisonHeaders("PACKAGE_NAME", db1Name, db2Name));

        CellStyle normalSt = normalStyle(wb);
        CellStyle greenSt  = coloredStyle(wb, GREEN_HEX);
        CellStyle redSt    = coloredStyle(wb, RED_HEX);
        CellStyle yellowSt = coloredStyle(wb, YELLOW_HEX);

        for (Map.Entry<String, List<String>> entry : result.entrySet()) {
            if ("info".equalsIgnoreCase(entry.getKey())) {
                for (String info : entry.getValue()) {
                    Row row = sheet.createRow(rowIdx++);
                    String[] infoRow = ComparisonOutputUtils.objectInfoRow(info);
                    for (int i = 0; i < infoRow.length; i++) {
                        setCell(row, i, infoRow[i], i == 1 ? yellowSt : normalSt);
                    }
                }
                continue;
            }

            for (String line : entry.getValue()) {

                String[] parts = parseObjectLine(line);
                if (parts == null) continue;

                Row row = sheet.createRow(rowIdx++);

                CellStyle statusStyle = parts[1].equals("SAME")
                        ? greenSt
                        : (parts[1].startsWith("ONLY") ? yellowSt : redSt);

                setCell(row, 0, parts[0], normalSt);
                setCell(row, 1, parts[1], statusStyle);
                setCell(row, 2, parts[2], normalSt);
                setCell(row, 3, parts[3], normalSt);
                setCell(row, 4, parts[4], "HIGH".equals(parts[4]) ? redSt : yellowSt);
                setCell(row, 5, parts[5], normalSt);
            }
        }

        autoSize(sheet, 6);
    }
    public static void exportPackageComparison(Map<String, List<String>> packageResult) {

        File dir = new File("output");
        if (!dir.exists()) dir.mkdirs();

        String outputPath = "output/package_differences.xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            writePackageSheet(wb, packageResult, "DB1", "DB2");

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }

            System.out.println("✅ Package Excel created: " + outputPath);

        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    // ─── 8. Compare Tables Sheet ─────────────────────────────────────────────
    private static void writeCompareTablesSheet(XSSFWorkbook wb,
                                                 DbConnectionFactory.DbConfig db1Config,
                                                 DbConnectionFactory.DbConfig db2Config) {
        String db1 = DbLabelUtils.displayName(db1Config);
        String db2 = DbLabelUtils.displayName(db2Config);

        List<String> d1Tables = ShowTablesService.GetNameTable(db1Config);
        List<String> d2Tables = ShowTablesService.GetNameTable(db2Config);

        Set<String> allTables = new TreeSet<>();
        allTables.addAll(d1Tables);
        allTables.addAll(d2Tables);

        Sheet sheet = wb.createSheet("Compare Tables");
        int rowIdx = 0;

        rowIdx = addSectionTitle(sheet, wb, rowIdx, "Compare Tables: " + db1 + " vs " + db2, 2);
        rowIdx = addHeaderRow(sheet, wb, rowIdx, new String[]{"Table", "Exists in"});

        CellStyle greenStyle  = coloredStyle(wb, GREEN_HEX);
        CellStyle yellowStyle = coloredStyle(wb, YELLOW_HEX);
        CellStyle cyanStyle   = coloredStyle(wb, CYAN_HEX);
        CellStyle normalSt    = normalStyle(wb);
        CellStyle altSt       = altRowStyle(wb);

        int dataRowCount = 0;
        for (String table : allTables) {
            boolean inDb1 = d1Tables.contains(table);
            boolean inDb2 = d2Tables.contains(table);

            String status;
            CellStyle statusStyle;

            if (inDb1 && inDb2) {
                status = db1 + " & " + db2;
                statusStyle = greenStyle;
            } else if (inDb1) {
                status = db1 + " only";
                statusStyle = yellowStyle;
            } else {
                status = db2 + " only";
                statusStyle = cyanStyle;
            }

            Row row = sheet.createRow(rowIdx++);
            CellStyle bg = (dataRowCount % 2 == 0) ? normalSt : altSt;
            setCell(row, 0, table, bg);
            setCell(row, 1, status, statusStyle);
            dataRowCount++;
        }
        autoSize(sheet, 2);
    }

    // ─── Standalone: export table differences to output/ folder ─────────────
    // ─── Standalone: export table differences to output/ folder ─────────────
    // Pass the two original lists — status is computed here, not embedded in strings
    public static void exportTableDifferences(
            java.util.Collection<String> allTables,
            List<String> d1Tables,
            List<String> d2Tables,
            String db1Name,
            String db2Name) {

        java.io.File dir = new java.io.File("output");
        if (!dir.exists()) dir.mkdirs();

        String outputPath = "output/table_differences.xlsx";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            writeTableDiffSheet(wb, allTables, d1Tables, d2Tables, db1Name, db2Name);
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
            System.out.println("✅ Table differences exported to: " + outputPath);
        } catch (IOException e) {
            System.err.println("❌ Failed to export table differences: " + e.getMessage());
        }
    }

    // ─── Master Export Method ─────────────────────────────────────────────────
    /**
     * Exports ALL comparison results into a single Excel workbook,
     * one sheet per comparison type — mirroring every method in TablePrinter.
     *
     * @param outputPath      path of the .xlsx file to write (e.g. "comparison_report.xlsx")
     * @param db1Config       first database config
     * @param db2Config       second database config
     * @param typeResult      result from type comparison
     * @param columnResult    result from column comparison
     * @param dataResult      result from data comparison
     * @param sharedTables    list of shared tables
     * @param functionResult  result from function comparison
     * @param procedureResult result from procedure comparison
     * @param triggerResult   result from trigger comparison
     */
    public static void exportAll(
            String outputPath,
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Map<String, List<String>> typeResult,
            Map<String, List<String>> columnResult,
            Map<String, List<String>> dataResult,
            List<String> sharedTables,
            Map<String, List<String>> functionResult,
            Map<String, List<String>> procedureResult,
            Map<String, List<String>> triggerResult,
            Map<String, List<String>> packageResult) {

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            String db1Name = DbLabelUtils.displayName(db1Config);
            String db2Name = DbLabelUtils.displayName(db2Config);

            writeCompareTablesSheet(wb, db1Config, db2Config);
            if (sharedTables != null && !sharedTables.isEmpty()) {
                writeSharedTablesSheet(wb, sharedTables, db1Name, db2Name);
            }
            if (typeResult != null && !typeResult.isEmpty()) {
                writeTypeSheet(wb, typeResult, db1Name, db2Name);
            }
            if (columnResult != null && !columnResult.isEmpty()) {
                writeColumnSheet(wb, columnResult, db1Name, db2Name);
            }
            if (dataResult != null && !dataResult.isEmpty()) {
                writeDataSheet(wb, dataResult, db1Name, db2Name);
            }
            if (functionResult != null && !functionResult.isEmpty()) {
                writeFunctionSheet(wb, functionResult, db1Name, db2Name);
            }
            if (procedureResult != null && !procedureResult.isEmpty()) {
                writeProcedureSheet(wb, procedureResult, db1Name, db2Name);
            }
            if (triggerResult != null && !triggerResult.isEmpty()) {
                writeTriggerSheet(wb, triggerResult, db1Name, db2Name);
            }
            if (packageResult != null) {
                writePackageSheet(wb, packageResult, db1Name, db2Name);
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }

            System.out.println("✅ Excel report exported to: " + outputPath);

        } catch (IOException e) {
            System.err.println("❌ Failed to export Excel: " + e.getMessage());
        }
    }

    // Backward-compatible overload without packageResult
    public static void exportAll(
            String outputPath,
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Map<String, List<String>> typeResult,
            Map<String, List<String>> columnResult,
            Map<String, List<String>> dataResult,
            List<String> sharedTables,
            Map<String, List<String>> functionResult,
            Map<String, List<String>> procedureResult,
            Map<String, List<String>> triggerResult) {

        exportAll(outputPath, db1Config, db2Config, typeResult, columnResult, dataResult,
                sharedTables, functionResult, procedureResult, triggerResult, null);
    }

    // ─── Parsers (mirrored from TablePrinter) ────────────────────────────────

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

    // ─── Utility ──────────────────────────────────────────────────────────────

    private static String[] parseObjectLine(String line) {
        if (line == null) return null;
        String[] parts = line.split("\\|", 6);
        if (parts.length < 2) return null;

        String name = parts[0].trim();
        String statusAndDetails = parts.length >= 2 ? parts[1].trim() : "";
        String db1Summary = parts.length >= 3 ? parts[2].trim() : "-";
        String db2Summary = parts.length >= 4 ? parts[3].trim() : "-";
        String impact = parts.length >= 5 ? parts[4].trim() : "LOW";
        String details = parts.length >= 6 ? parts[5].trim() : "";

        String status;
        if (statusAndDetails.startsWith("SAME")) {
            status = "SAME";
        } else if (statusAndDetails.startsWith("DIFFERENT")) {
            status = "DIFFERENT";
        } else if (statusAndDetails.startsWith("ONLY_IN_") || statusAndDetails.startsWith("ONLY IN ")) {
            status = statusAndDetails;
        } else if (statusAndDetails.startsWith("ERROR")) {
            status = "ERROR";
        } else {
            status = statusAndDetails;
        }

        return new String[]{name, status, db1Summary, db2Summary, impact, details};
    }

    private static byte[] hexToBytes(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new byte[]{(byte) r, (byte) g, (byte) b};
    }
}