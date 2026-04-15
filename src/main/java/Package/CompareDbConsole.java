package Package;

import java.io.File;
import java.util.*;
import services.DbConnectionFactory;
import services.DbLabelUtils;
import services.TablePrinter;

public class CompareDbConsole {

    private static final Scanner scanner = new Scanner(System.in);

    public static void run() {

        System.out.println("==================================");
        System.out.println("     DATABASE COMPARATOR ");
        System.out.println("==================================\n");

        DbConnectionFactory.DbEngine sharedEngine = readSharedEngine();

        System.out.println("Enter Source Database (DB1):");
        DbConnectionFactory.DbConfig db1 = readDbConfig(sharedEngine);

        System.out.println("\nEnter Target Database (DB2):");
        DbConnectionFactory.DbConfig db2 = readDbConfig(sharedEngine);


        Set<String> options = readOptions();

        Set<String> tables = new HashSet<>();
        if (options.contains("columns") || options.contains("data") || options.contains("types")) {
            tables = selectTables(db1, db2);
            if (tables == null) {
                System.out.println("❌ Operation canceled.");
                return;
            }
        }


        System.out.println("\n🚀 Running comparison...\n");

        String reportPath = runComparisons(db1, db2, tables, options);
        if (reportPath != null) {
            System.out.println("\n📄 Excel report: " + reportPath);
        }

        System.out.println("\n✅ Done.");
    }



    private static DbConnectionFactory.DbEngine readSharedEngine() {
        System.out.print("Database engine for both DBs (mysql/oracle, default: mysql): ");
        String engineInput = scanner.nextLine();
        return DbConnectionFactory.DbEngine.from(engineInput);
    }


    private static DbConnectionFactory.DbConfig readDbConfig(DbConnectionFactory.DbEngine engine) {

        System.out.print("Database name: ");
        String name = scanner.nextLine();

        System.out.print("Host (default: localhost): ");
        String host = scanner.nextLine();

        String defaultPort = engine == DbConnectionFactory.DbEngine.ORACLE ? "1521" : "3306";
        System.out.print("Port (default: " + defaultPort + "): ");
        String port = scanner.nextLine();

        System.out.print("User: ");
        String user = scanner.nextLine();

        System.out.print("Password: ");
        String pass = scanner.nextLine();

        DbConnectionFactory.DbConfig def = DbConnectionFactory.getDefaultConfig(name, engine);

        return new DbConnectionFactory.DbConfig(
            engine,
                name,
                host.isEmpty() ? def.getHost() : host,
                port.isEmpty() ? def.getPort() : port,
                user,
                pass,
                def.getParams()
        );
    }



    private static Set<String> readOptions() {
        System.out.println("\nSelect comparison options (comma separated):");
        System.out.println("1. tables");
        System.out.println("2. columns");
        System.out.println("3. data");
        System.out.println("4. types");
        System.out.println("5. functions");
        System.out.println("6. procedures");
        System.out.println("7. triggers");
        System.out.println("8. packages");
        System.out.println("9. all");

        System.out.print("Your choice: ");
        String input = scanner.nextLine().toLowerCase();

        Set<String> set = new HashSet<>();

        if (input.contains("all") || input.contains("9")) {
            return new HashSet<>(Arrays.asList(
                    "tables", "columns", "data", "types",
                    "functions", "procedures", "triggers", "packages"
            ));
        }

        for (String s : input.split(",")) {
            String normalized = normalizeOptionToken(s);
            if (!normalized.isEmpty()) {
                set.add(normalized);
            }
        }

        return set;
    }

    private static String normalizeOptionToken(String token) {
        String t = token == null ? "" : token.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return "";

        if ("1".equals(t)) return "tables";
        if ("2".equals(t)) return "columns";
        if ("3".equals(t)) return "data";
        if ("4".equals(t)) return "types";
        if ("5".equals(t)) return "functions";
        if ("6".equals(t)) return "procedures";
        if ("7".equals(t)) return "triggers";
        if ("8".equals(t)) return "packages";
        if ("9".equals(t)) return "all";

        return t;
    }



    private static Set<String> selectTables(DbConnectionFactory.DbConfig c1,
                                            DbConnectionFactory.DbConfig c2) {

        List<String> shared = CompareDataService.getSharedTablesBetweenDbs(c1, c2);

        if (shared.isEmpty()) {
            System.out.println("❌ No common tables found.");
            return null;
        }

        System.out.println("\nAvailable tables:");
        for (int i = 0; i < shared.size(); i++) {
            System.out.println((i + 1) + ". " + shared.get(i));
        }

        System.out.println("\nSelect tables (e.g: 1,2,3 or 'all'): ");
        String input = scanner.nextLine();

        if (input.equalsIgnoreCase("all")) {
            return new HashSet<>(shared);
        }

        Set<String> selected = new HashSet<>();

        for (String s : input.split(",")) {
            int idx = Integer.parseInt(s.trim()) - 1;
            if (idx >= 0 && idx < shared.size()) {
                selected.add(shared.get(idx));
            }
        }

        return selected;
    }



    private static String runComparisons(DbConnectionFactory.DbConfig c1,
                                         DbConnectionFactory.DbConfig c2,
                                         Set<String> tables,
                                         Set<String> options) {
        String db1Label = DbLabelUtils.displayName(c1);
        String db2Label = DbLabelUtils.displayName(c2);

        Map<String, List<String>> typeResult = null;
        Map<String, List<String>> columnResult = null;
        Map<String, List<String>> dataResult = null;
        Map<String, List<String>> functionResult = null;
        Map<String, List<String>> procedureResult = null;
        Map<String, List<String>> triggerResult = null;
        Map<String, List<String>> packageResult = null;

        if (options.contains("tables")) {
            System.out.println("\n========== COMPARE TABLES ==========");
            CompareTablesDb.compareTables(c1, c2);
        }

        if (options.contains("columns")) {
            System.out.println("\n========== COMPARE COLUMNS ==========");
            columnResult = CompareDataService.compareColumnsApi(c1, c2, tables);
            TablePrinter.printColumnComparison(columnResult, db1Label, db2Label);
        }

        if (options.contains("data")) {
            System.out.println("\n========== COMPARE DATA ==========");
            dataResult = CompareDataService.compareDataApi(c1, c2, tables);
            TablePrinter.printDataComparison(dataResult, db1Label, db2Label);
        }

        if (options.contains("types")) {
            System.out.println("\n========== COMPARE TYPES ==========");
            typeResult = CompareDataService.compareTypesApi(c1, c2, tables);
            TablePrinter.printTypeComparison(typeResult, db1Label, db2Label);
        }

        if (options.contains("functions")) {
            System.out.println("\n========== COMPARE FUNCTIONS ==========");
            functionResult = CompareFuncDB.compareFunctionsApi(c1, c2);
            TablePrinter.printComparisonFunction(functionResult, db1Label, db2Label);
        }

        if (options.contains("procedures")) {
            System.out.println("\n========== COMPARE PROCEDURES ==========");
            procedureResult = CompareFuncDB.compareProceduresApi(c1, c2);
            TablePrinter.printComparisonProcedures(procedureResult, db1Label, db2Label);
        }

        if (options.contains("triggers")) {
            System.out.println("\n========== COMPARE TRIGGERS ==========");
            triggerResult = CompareFuncDB.compareTriggersApi(c1, c2);
            TablePrinter.printComparisonTrigger(triggerResult, db1Label, db2Label);
        }

        if (options.contains("packages")) {
            System.out.println("\n========== COMPARE PACKAGES ==========");
            packageResult = CompareFuncDB.comparePackagesApi(c1, c2);
            TablePrinter.printComparisonPackages(packageResult, db1Label, db2Label);
        }

        String reportPath = buildExportPath(c1, c2);
        ExportExcel.exportAll(
                reportPath,
                c1,
                c2,
                typeResult,
                columnResult,
                dataResult,
                tables == null ? Collections.<String>emptyList() : new ArrayList<String>(tables),
                functionResult,
                procedureResult,
                triggerResult,
                packageResult);

        return reportPath;
    }

    private static String buildExportPath(DbConnectionFactory.DbConfig db1Config,
                                          DbConnectionFactory.DbConfig db2Config) {
        File dir = new File("output");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        return new File(dir,
                "comparison_console_"
                        + sanitizeFileName(db1Config.getDatabaseName())
                        + "_vs_"
                        + sanitizeFileName(db2Config.getDatabaseName())
                        + "_"
                        + timestamp
                        + ".xlsx").getPath();
    }

    private static String sanitizeFileName(String value) {
        return value == null ? "db" : value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

