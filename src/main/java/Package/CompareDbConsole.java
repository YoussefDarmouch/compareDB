package Package;

import java.util.*;
import services.DbConnectionFactory;

public class CompareDbConsole {

    private static final Scanner scanner = new Scanner(System.in);

    public static void run() {

        System.out.println("==================================");
        System.out.println("     DATABASE COMPARATOR ");
        System.out.println("==================================\n");


        System.out.println("Enter Source Database (DB1):");
        DbConnectionFactory.DbConfig db1 = readDbConfig();

        System.out.println("\nEnter Target Database (DB2):");
        DbConnectionFactory.DbConfig db2 = readDbConfig();


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

        runComparisons(db1, db2, tables, options);

        System.out.println("\n✅ Done.");
    }



    private static DbConnectionFactory.DbConfig readDbConfig() {
        System.out.print("Database name: ");
        String name = scanner.nextLine();

        System.out.print("Host (default: localhost): ");
        String host = scanner.nextLine();

        System.out.print("Port (default: 3306): ");
        String port = scanner.nextLine();

        System.out.print("User: ");
        String user = scanner.nextLine();

        System.out.print("Password: ");
        String pass = scanner.nextLine();

        DbConnectionFactory.DbConfig def = DbConnectionFactory.getDefaultConfig(name);

        return new DbConnectionFactory.DbConfig(
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
        System.out.println("8. all");

        System.out.print("Your choice: ");
        String input = scanner.nextLine().toLowerCase();

        Set<String> set = new HashSet<>();

        if (input.contains("all")) {
            return new HashSet<>(Arrays.asList(
                    "tables", "columns", "data", "types",
                    "functions", "procedures", "triggers"
            ));
        }

        for (String s : input.split(",")) {
            set.add(s.trim());
        }

        return set;
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



    private static void runComparisons(DbConnectionFactory.DbConfig c1,
                                       DbConnectionFactory.DbConfig c2,
                                       Set<String> tables,
                                       Set<String> options) {

        if (options.contains("tables")) {
            System.out.println("\n========== COMPARE TABLES ==========");
            CompareTablesDb.compareTables(c1, c2);
        }

        if (options.contains("columns")) {
            System.out.println("\n========== COMPARE COLUMNS ==========");
            CompareDataService.compareColumns(c1, c2, tables);
        }

        if (options.contains("data")) {
            System.out.println("\n========== COMPARE DATA ==========");
            CompareDataService.compareData(c1, c2, tables);
        }

        if (options.contains("types")) {
            System.out.println("\n========== COMPARE TYPES ==========");
            CompareDataService.compareTypes(c1, c2, tables);
        }

        if (options.contains("functions")) {
            System.out.println("\n========== COMPARE FUNCTIONS ==========");
            CompareFuncDB.compareFunctions(c1, c2);
        }

        if (options.contains("procedures")) {
            System.out.println("\n========== COMPARE PROCEDURES ==========");
            CompareFuncDB.compareProcedures(c1, c2);
        }

        if (options.contains("triggers")) {
            System.out.println("\n========== COMPARE TRIGGERS ==========");
            CompareFuncDB.compareTriggers(c1, c2);
        }
    }
}

