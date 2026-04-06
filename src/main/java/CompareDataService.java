import services.TableColumnsService;
import services.ShowTablesService;
import services.DbConnectionFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class CompareDataService {
    // Returns tables that exist in both databases.
    private static List<String> getSharedTables(String db1, String db2) {
        return getSharedTables(db1, db2, null);
    }

    private static List<String> getSharedTables(DbConnectionFactory.DbConfig db1Config,
                                                 DbConnectionFactory.DbConfig db2Config) {
        return getSharedTables(db1Config, db2Config, null);
    }

    // Returns shared tables and applies an optional user selection filter.
    private static List<String> getSharedTables(String db1, String db2, Set<String> selectedTables) {
        return getSharedTables(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2),
                selectedTables
        );
    }

    // Returns shared tables and applies an optional user selection filter.
    private static List<String> getSharedTables(DbConnectionFactory.DbConfig db1Config,
                                                DbConnectionFactory.DbConfig db2Config,
                                                Set<String> selectedTables) {
        List<String> tablesInDb1 = ShowTablesService.GetNameTable(db1Config);
        List<String> tablesInDb2 = ShowTablesService.GetNameTable(db2Config);

        Set<String> tablesInDb2Set = new HashSet<>(tablesInDb2);
        List<String> sharedTables = new ArrayList<>();

        for (String table : tablesInDb1) {
            if (tablesInDb2Set.contains(table)) {
                sharedTables.add(table);
            }
        }

        // Keep only tables selected by the user (when provided).
        if (selectedTables != null && !selectedTables.isEmpty()) {
            sharedTables.retainAll(selectedTables);
        }

        // Sort for stable and readable output.
        Collections.sort(sharedTables);
        return sharedTables;
    }

    // Used by the GUI to show common tables before running comparison.
    public static List<String> getSharedTablesBetweenDbs(String db1, String db2) {
        return getSharedTables(db1, db2);
    }

    public static List<String> getSharedTablesBetweenDbs(DbConnectionFactory.DbConfig db1Config,
                                                          DbConnectionFactory.DbConfig db2Config) {
        return getSharedTables(db1Config, db2Config);
    }

    // Prints tables that exist only in one side.
    private static void printMissingTables(String db1, String db2) {
        printMissingTables(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2)
        );
    }

    // Prints tables that exist only in one side.
    private static void printMissingTables(DbConnectionFactory.DbConfig db1Config,
                                           DbConnectionFactory.DbConfig db2Config) {
        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        List<String> tablesInDb1 = ShowTablesService.GetNameTable(db1Config);
        List<String> tablesInDb2 = ShowTablesService.GetNameTable(db2Config);

        Set<String> tablesInDb1Set = new HashSet<>(tablesInDb1);
        Set<String> tablesInDb2Set = new HashSet<>(tablesInDb2);

        for (String table : tablesInDb1) {
            if (!tablesInDb2Set.contains(table)) {
                System.out.println("Table exists only in " + db1 + ": " + table);
            }
        }

        for (String table : tablesInDb2) {
            if (!tablesInDb1Set.contains(table)) {
                System.out.println("Table exists only in " + db2 + ": " + table);
            }
        }
    }

    // Extracts the column name from format: "column_name (column_type)".
    private static String getColumnName(String columnDefinition) {
        int openParenIndex = columnDefinition.indexOf('(');
        if (openParenIndex < 0) {
            return columnDefinition.trim();
        }
        return columnDefinition.substring(0, openParenIndex).trim();
    }

    // Extracts the type from format: "column_name (column_type)".
    private static String getColumnType(String columnDefinition) {
        int openParenIndex = columnDefinition.indexOf('(');
        int closeParenIndex = columnDefinition.lastIndexOf(')');
        if (openParenIndex < 0 || closeParenIndex <= openParenIndex) {
            return "";
        }
        return columnDefinition.substring(openParenIndex + 1, closeParenIndex).trim();
    }

    // Converts column definitions into a map of name -> type.
    private static Map<String, String> toTypeMap(List<String> columns) {
        Map<String, String> types = new HashMap<>();
        for (String columnDefinition : columns) {
            types.put(getColumnName(columnDefinition), getColumnType(columnDefinition));
        }
        return types;
    }

    // Converts column definitions into a set of names only.
    private static Set<String> toColumnNameSet(List<String> columns) {
        Set<String> names = new HashSet<>();
        for (String columnDefinition : columns) {
            names.add(getColumnName(columnDefinition));
        }
        return names;
    }

    public static void compareTypes(String db1, String db2) {
        compareTypes(db1, db2, null);
    }

    public static void compareTypes(String db1, String db2, Set<String> selectedTables) {
        compareTypes(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2),
                selectedTables
        );
    }

    public static void compareTypes(DbConnectionFactory.DbConfig db1Config,
                                    DbConnectionFactory.DbConfig db2Config,
                                    Set<String> selectedTables) {
        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        System.out.println("\n--- Type Differences ---");
        printMissingTables(db1Config, db2Config);

        List<String> sharedTables = getSharedTables(db1Config, db2Config, selectedTables);
        if (sharedTables.isEmpty()) {
            System.out.println("No common tables to compare types.");
            return;
        }

        boolean hasDifference = false;
        for (String table : sharedTables) {
            // Read type metadata for the same table from both databases.
            Map<String, String> typesInDb1 = toTypeMap(TableColumnsService.GetColumns(db1Config, table));
            Map<String, String> typesInDb2 = toTypeMap(TableColumnsService.GetColumns(db2Config, table));

            // Compare only columns that exist in both maps.
            for (String columnName : typesInDb1.keySet()) {
                if (!typesInDb2.containsKey(columnName)) {
                    continue;
                }

                String typeInDb1 = typesInDb1.get(columnName);
                String typeInDb2 = typesInDb2.get(columnName);

                if (!typeInDb1.equalsIgnoreCase(typeInDb2)) {
                    System.out.println("Table " + table + " => type difference in column " + columnName +
                            " => " + db1 + ": " + typeInDb1 + " | " + db2 + ": " + typeInDb2);
                    hasDifference = true;
                }
            }
        }

        if (!hasDifference) {
            System.out.println("No type differences found");
        }
    }

    public static void compareColumns(String db1, String db2) {
        compareColumns(db1, db2, null);
    }

    public static void compareColumns(String db1, String db2, Set<String> selectedTables) {
        compareColumns(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2),
                selectedTables
        );
    }

    public static void compareColumns(DbConnectionFactory.DbConfig db1Config,
                                      DbConnectionFactory.DbConfig db2Config,
                                      Set<String> selectedTables) {
        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        System.out.println("\n--- Column Differences (NAME ONLY) ---");
        printMissingTables(db1Config, db2Config);

        List<String> sharedTables = getSharedTables(db1Config, db2Config, selectedTables);
        if (sharedTables.isEmpty()) {
            System.out.println("No common tables to compare columns.");
            return;
        }

        boolean hasDifference = false;
        for (String table : sharedTables) {
            // Read column names for the same table from both databases.
            Set<String> columnNamesInDb1 = toColumnNameSet(TableColumnsService.GetColumns(db1Config, table));
            Set<String> columnNamesInDb2 = toColumnNameSet(TableColumnsService.GetColumns(db2Config, table));

            // Show columns that exist only in db1.
            for (String columnName : columnNamesInDb1) {
                if (!columnNamesInDb2.contains(columnName)) {
                    System.out.println("Table " + table + " => column exists only in " + db1 + ": " + columnName);
                    hasDifference = true;
                }
            }

            // Show columns that exist only in db2.
            for (String columnName : columnNamesInDb2) {
                if (!columnNamesInDb1.contains(columnName)) {
                    System.out.println("Table " + table + " => column exists only in " + db2 + ": " + columnName);
                    hasDifference = true;
                }
            }
        }

        if (!hasDifference) {
            System.out.println("No column differences found");
        }
    }

    public static void compareData(String db1Name, String db2Name) {
        compareData(db1Name, db2Name, null);
    }

    public static void compareData(String db1Name, String db2Name, Set<String> selectedTables) {
        compareData(
                DbConnectionFactory.getDefaultConfig(db1Name),
                DbConnectionFactory.getDefaultConfig(db2Name),
                selectedTables
        );
    }

    public static void compareData(DbConnectionFactory.DbConfig db1Config,
                                   DbConnectionFactory.DbConfig db2Config,
                                   Set<String> selectedTables) {
        String db1Name = db1Config.getDatabaseName();
        String db2Name = db2Config.getDatabaseName();

        System.out.println("\n--- Data Differences ---");
        printMissingTables(db1Config, db2Config);

        List<String> sharedTables = getSharedTables(db1Config, db2Config, selectedTables);
        if (sharedTables.isEmpty()) {
            System.out.println("No common tables to compare data.");
            return;
        }

        boolean hasDifference = false;
        for (String table : sharedTables) {
            // Read table rows as map: id -> (column -> value).
            Map<Integer, Map<String, String>> dataInDb1 = TableColumnsService.GetData(db1Config, table);
            Map<Integer, Map<String, String>> dataInDb2 = TableColumnsService.GetData(db2Config, table);

            // Compare each row from db1 against db2.
            for (Integer id : dataInDb1.keySet()) {

                if (!dataInDb2.containsKey(id)) {
                    System.out.println("Table " + table + " => row id exists only in " + db1Name + ": " + id);
                    hasDifference = true;
                    continue;
                }

                Map<String, String> rowInDb1 = dataInDb1.get(id);
                Map<String, String> rowInDb2 = dataInDb2.get(id);

                // Compare each column value for the same row id.
                for (String columnName : rowInDb1.keySet()) {

                    if (!rowInDb2.containsKey(columnName)) {
                        System.out.println("Table " + table + " => column " + columnName + " missing in " + db2Name + " for id " + id);
                        hasDifference = true;
                    } else if (!Objects.equals(rowInDb1.get(columnName), rowInDb2.get(columnName))) {
                        System.out.println("Table " + table + " => id " + id + ", column " + columnName + " differs");
                        System.out.println("   " + db1Name + ": " + rowInDb1.get(columnName));
                        System.out.println("   " + db2Name + ": " + rowInDb2.get(columnName));
                        hasDifference = true;
                    }
                }

                // Show extra columns that exist only in db2 for this row.
                for (String columnName : rowInDb2.keySet()) {
                    if (!rowInDb1.containsKey(columnName)) {
                        System.out.println("Table " + table + " => column " + columnName + " exists only in " + db2Name + " for id " + id);
                        hasDifference = true;
                    }
                }
            }

            // Show row ids that exist only in db2.
            for (Integer id : dataInDb2.keySet()) {
                if (!dataInDb1.containsKey(id)) {
                    System.out.println("Table " + table + " => row id exists only in " + db2Name + ": " + id);
                    hasDifference = true;
                }
            }
        }

        if (!hasDifference) {
            System.out.println("No data differences found");
        }
    }
}