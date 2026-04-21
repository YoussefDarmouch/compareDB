package Package;

import java.io.File;
import java.util.*;
import java.util.Scanner;

import services.DbConnectionFactory;
import services.DbLabelUtils;
import services.DbObjectDiff;
import services.TablePrinter;

public class CompareDbConsole {

    private static final Scanner scanner = new Scanner(System.in);

    public static void run() {

        System.out.println("==================================");
        System.out.println("     DATABASE COMPARATOR ");
        System.out.println("==================================\n");

        DbConnectionFactory.DbEngine engine = readSharedEngine();

        System.out.println("Enter Source Database (DB1):");
        DbConnectionFactory.DbConfig db1 = readDbConfig(engine);

        System.out.println("\nEnter Target Database (DB2):");
        DbConnectionFactory.DbConfig db2 = readDbConfig(engine);

        Set<String> options = readOptions(engine);

        Set<String> tables = new HashSet<>();

        boolean needsTables =
                options.contains("columns") ||
                        options.contains("data") ||
                        options.contains("types");

        if (needsTables) {
            tables = selectTables(db1, db2);
        }

        System.out.println("\n🚀 Running comparison...\n");

        String reportPath = runComparisons(db1, db2, tables, options);

        System.out.println("\n📄 Excel report: " + reportPath);
        System.out.println("\n✅ Done.");
    }

    private static DbConnectionFactory.DbEngine readSharedEngine() {
        System.out.print("Database engine (mysql/oracle): ");
        return DbConnectionFactory.DbEngine.from(scanner.nextLine());
    }

    private static DbConnectionFactory.DbConfig readDbConfig(DbConnectionFactory.DbEngine engine) {

        String defaultPort = engine == DbConnectionFactory.DbEngine.ORACLE ? "1521" : "3306";

        System.out.print("Database name: ");
        String name = scanner.nextLine();

        System.out.print("Host (localhost): ");
        String host = scanner.nextLine();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Port (" + defaultPort + "): ");
        String port = scanner.nextLine();
        if (port.isEmpty()) port = defaultPort;

        System.out.print("User: ");
        String user = scanner.nextLine();

        System.out.print("Password: ");
        String pass = scanner.nextLine();

        DbConnectionFactory.DbConfig def = DbConnectionFactory.getDefaultConfig(name, engine);

        return new DbConnectionFactory.DbConfig(
                engine, name, host, port, user, pass, def.getParams()
        );
    }

    // ================= OPTIONS =================
    private static Set<String> readOptions(DbConnectionFactory.DbEngine engine) {

        System.out.println("\nOptions:");
        System.out.println("1.tables  2.columns  3.data  4.types");
        System.out.println("5.functions 6.procedures 7.triggers");

        if (engine == DbConnectionFactory.DbEngine.ORACLE) {
            System.out.println("8.packages");
        }

        System.out.println("9.all");

        String input = scanner.nextLine().toLowerCase();
        Set<String> set = new HashSet<>();

        if (input.contains("9") || input.contains("all")) {

            set.addAll(Arrays.asList(
                    "tables", "columns", "data", "types",
                    "functions", "procedures", "triggers"
            ));

            if (engine == DbConnectionFactory.DbEngine.ORACLE) {
                set.add("packages");
            }

            return set;
        }

        for (String s : input.split(",")) {

            String opt = normalize(s);

            if ("packages".equals(opt) &&
                    engine != DbConnectionFactory.DbEngine.ORACLE) continue;

            if (!opt.isEmpty()) set.add(opt);
        }

        return set;
    }

    private static String normalize(String t) {
        t = t.trim();
        switch (t) {
            case "1": return "tables";
            case "2": return "columns";
            case "3": return "data";
            case "4": return "types";
            case "5": return "functions";
            case "6": return "procedures";
            case "7": return "triggers";
            case "8": return "packages";
            case "9": return "all";
        }
        return t;
    }

    // ================= TABLE SELECTION (FIXED) =================
    private static Set<String> selectTables(DbConnectionFactory.DbConfig c1,
                                            DbConnectionFactory.DbConfig c2) {

        List<String> shared = CompareDataService.getSharedTablesBetweenDbs(c1, c2);

        if (shared.isEmpty()) {
            System.out.println("No tables.");
            return new HashSet<>();
        }

        System.out.println("\n--- Shared Tables ---");

        for (int i = 0; i < shared.size(); i++) {
            System.out.println((i + 1) + ". " + shared.get(i));
        }

        int allOption = shared.size() + 1;
        System.out.println(allOption + ". all");

        System.out.print("\nSelect tables (e.g. 1,3 or " + allOption + " for all): ");

        String input = scanner.nextLine().trim().toLowerCase();

        if (input.equals("all") || input.equals(String.valueOf(allOption))) {
            return new HashSet<>(shared);
        }

        Set<String> selected = new HashSet<>();

        for (String s : input.split(",")) {
            try {
                int i = Integer.parseInt(s.trim());

                if (i == allOption) {
                    return new HashSet<>(shared);
                }

                i--;

                if (i >= 0 && i < shared.size()) {
                    selected.add(shared.get(i));
                }

            } catch (Exception ignored) {}
        }

        return selected;
    }

    // ================= COMPARISON =================
    private static String runComparisons(DbConnectionFactory.DbConfig c1,
                                         DbConnectionFactory.DbConfig c2,
                                         Set<String> tables,
                                         Set<String> options) {

        String db1 = DbLabelUtils.displayName(c1);
        String db2 = DbLabelUtils.displayName(c2);

        Map<String,List<String>> typeR=null, colR=null, dataR=null;
        Map<String,List<String>> funR=null, procR=null, trigR=null, packR=null;

        if (options.contains("tables"))
            CompareTablesDb.compareTables(c1,c2);

        if (options.contains("columns")) {
            colR = CompareDataService.compareColumnsApi(c1,c2,tables);
            TablePrinter.printColumnComparison(colR, db1, db2);
        }

        if (options.contains("data")) {
            dataR = CompareDataService.compareDataApi(c1,c2,tables);
            TablePrinter.printDataComparison(dataR, db1, db2);
        }

        if (options.contains("types")) {
            typeR = CompareDataService.compareTypesApi(c1,c2,tables);
            TablePrinter.printTypeComparison(typeR, db1, db2);
        }

        if (options.contains("functions")) {
            List<DbObjectDiff> d = CompareFuncDB.compareFunctionsDiffs(c1,c2);
            TablePrinter.printDiffsFunction(d, db1, db2);
            funR = CompareFuncDB.diffsToMap("functions", d, db1, db2);
        }

        if (options.contains("procedures")) {
            List<DbObjectDiff> d = CompareFuncDB.compareProceduresDiffs(c1,c2);
            TablePrinter.printDiffsProcedure(d, db1, db2);
            procR = CompareFuncDB.diffsToMap("procedures", d, db1, db2);
        }

        if (options.contains("triggers")) {
            List<DbObjectDiff> d = CompareFuncDB.compareTriggersDiffs(c1,c2);
            TablePrinter.printDiffsTrigger(d, db1, db2);
            trigR = CompareFuncDB.diffsToMap("triggers", d, db1, db2);
        }

        if (options.contains("packages") &&
                c1.getEngine() == DbConnectionFactory.DbEngine.ORACLE) {

            List<DbObjectDiff> d = CompareFuncDB.comparePackagesDiffs(c1,c2);
            TablePrinter.printDiffsPackage(d, db1, db2);
            packR = CompareFuncDB.diffsToMap("packages", d, db1, db2);
        }

        String path = buildPath(c1,c2);

        ExportExcel.exportAll(
                path,c1,c2,
                typeR,colR,dataR,
                tables == null ? Collections.emptyList() : new ArrayList<>(tables),
                funR,procR,trigR,packR
        );

        return path;
    }

    private static String buildPath(DbConnectionFactory.DbConfig d1,
                                    DbConnectionFactory.DbConfig d2) {

        File f = new File("output");
        if (!f.exists()) f.mkdirs();

        String t = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new java.util.Date());

        return "output/compare_" + d1.getDatabaseName() +
                "_vs_" + d2.getDatabaseName() + "_" + t + ".xlsx";
    }
}