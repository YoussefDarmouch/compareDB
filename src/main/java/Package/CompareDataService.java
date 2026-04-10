package Package;

import services.TableColumnsService;
import services.ShowTablesService;
import services.DbConnectionFactory;
import services.TablePrinter;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class CompareDataService {


    private static List<String> getSharedTables(String db1, String db2) {
        return getSharedTables(db1, db2, null);
    }

    private static List<String> getSharedTables(DbConnectionFactory.DbConfig db1Config,
                                                DbConnectionFactory.DbConfig db2Config) {
        return getSharedTables(db1Config, db2Config, null);
    }

    private static List<String> getSharedTables(String db1, String db2, Set<String> selectedTables) {
        return getSharedTables(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2),
                selectedTables
        );
    }

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

        if (selectedTables != null && !selectedTables.isEmpty()) {
            sharedTables.retainAll(selectedTables);
        }

        Collections.sort(sharedTables);
        return sharedTables;
    }


    public static List<String> getSharedTablesBetweenDbs(String db1, String db2) {
        List<String> shared = getSharedTables(db1, db2);
        TablePrinter.printSharedTables(shared);   // ← table output
        return shared;
    }

    public static List<String> getSharedTablesBetweenDbs(DbConnectionFactory.DbConfig db1Config,
                                                         DbConnectionFactory.DbConfig db2Config) {
        List<String> shared = getSharedTables(db1Config, db2Config);
        TablePrinter.printSharedTables(shared);   // ← table output
        return shared;
    }


    private static void printMissingTables(String db1, String db2) {
        printMissingTables(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2)
        );
    }

    private static void printMissingTables(DbConnectionFactory.DbConfig db1Config,
                                           DbConnectionFactory.DbConfig db2Config) {
        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        List<String> tablesInDb1 = ShowTablesService.GetNameTable(db1Config);
        List<String> tablesInDb2 = ShowTablesService.GetNameTable(db2Config);

        Set<String> tablesInDb1Set = new HashSet<>(tablesInDb1);
        Set<String> tablesInDb2Set = new HashSet<>(tablesInDb2);

        // Build a result map in the same shape TablePrinter expects:
        // key = table name, value = ["exists only in <dbName>"]
        Map<String, List<String>> missing = new java.util.LinkedHashMap<>();

        for (String table : tablesInDb1) {
            if (!tablesInDb2Set.contains(table)) {
                missing.put(table, Collections.singletonList("exists only in " + db1));
            }
        }
        for (String table : tablesInDb2) {
            if (!tablesInDb1Set.contains(table)) {
                missing.put(table, Collections.singletonList("exists only in " + db2));
            }
        }


    }


    private static String getColumnName(String columnDefinition) {
        int openParenIndex = columnDefinition.indexOf('(');
        if (openParenIndex < 0) return columnDefinition.trim();
        return columnDefinition.substring(0, openParenIndex).trim();
    }

    private static String getColumnType(String columnDefinition) {
        int openParenIndex  = columnDefinition.indexOf('(');
        int closeParenIndex = columnDefinition.lastIndexOf(')');
        if (openParenIndex < 0 || closeParenIndex <= openParenIndex) return "";
        return columnDefinition.substring(openParenIndex + 1, closeParenIndex).trim();
    }

    private static Map<String, String> toTypeMap(List<String> columns) {
        Map<String, String> types = new HashMap<>();
        for (String col : columns) types.put(getColumnName(col), getColumnType(col));
        return types;
    }

    private static Set<String> toColumnNameSet(List<String> columns) {
        Set<String> names = new HashSet<>();
        for (String col : columns) names.add(getColumnName(col));
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

    private static Map<String, List<String>> compareTypesLogic(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> selectedTables) {

        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        Map<String, List<String>> result = new HashMap<>();
        List<String> sharedTables = getSharedTables(db1Config, db2Config, selectedTables);

        if (sharedTables.isEmpty()) {
            result.put("info", Collections.singletonList("No common tables to compare types."));
            return result;
        }

        boolean hasDifference = false;

        for (String table : sharedTables) {
            Map<String, String> typesInDb1 = toTypeMap(TableColumnsService.GetColumns(db1Config, table));
            Map<String, String> typesInDb2 = toTypeMap(TableColumnsService.GetColumns(db2Config, table));

            List<String> diff = new ArrayList<>();

            for (String columnName : typesInDb1.keySet()) {
                if (!typesInDb2.containsKey(columnName)) continue;

                String type1 = typesInDb1.get(columnName);
                String type2 = typesInDb2.get(columnName);

                if (!type1.equalsIgnoreCase(type2)) {
                    diff.add("Column " + columnName +
                            " => " + db1 + ": " + type1 +
                            " | " + db2 + ": " + type2);
                    hasDifference = true;
                }
            }

            if (!diff.isEmpty()) result.put(table, diff);
        }

        if (!hasDifference) {
            result.put("info", Collections.singletonList("No type differences found"));
        }

        return result;
    }

    // API
    public static Map<String, List<String>> compareTypesApi(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> selectedTables) {
        return compareTypesLogic(db1Config, db2Config, selectedTables);
    }

    // CONSOLE — now prints a formatted table
    public static void compareTypes(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> selectedTables) {

        Map<String, List<String>> result = compareTypesLogic(db1Config, db2Config, selectedTables);
        TablePrinter.printTypeComparison(result,                // ← table output
                db1Config.getDatabaseName(),
                db2Config.getDatabaseName());
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

    private static Map<String, List<String>> compareColumnsLogic(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> selectedTables) {

        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        Map<String, List<String>> result = new HashMap<>();
        List<String> sharedTables = getSharedTables(db1Config, db2Config, selectedTables);

        if (sharedTables.isEmpty()) {
            result.put("info", Collections.singletonList("No common tables to compare columns."));
            return result;
        }

        boolean hasDifference = false;

        for (String table : sharedTables) {
            Set<String> columnNamesInDb1 = toColumnNameSet(TableColumnsService.GetColumns(db1Config, table));
            Set<String> columnNamesInDb2 = toColumnNameSet(TableColumnsService.GetColumns(db2Config, table));

            List<String> diff = new ArrayList<>();

            for (String columnName : columnNamesInDb1) {
                if (!columnNamesInDb2.contains(columnName)) {
                    diff.add("column exists only in " + db1 + ": " + columnName);
                    hasDifference = true;
                }
            }
            for (String columnName : columnNamesInDb2) {
                if (!columnNamesInDb1.contains(columnName)) {
                    diff.add("column exists only in " + db2 + ": " + columnName);
                    hasDifference = true;
                }
            }

            if (!diff.isEmpty()) result.put(table, diff);
        }

        if (!hasDifference) {
            result.put("info", Collections.singletonList("No column differences found"));
        }

        return result;
    }

    // API
    public static Map<String, List<String>> compareColumnsApi(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> selectedTables) {
        return compareColumnsLogic(db1Config, db2Config, selectedTables);
    }

    // CONSOLE — now prints a formatted table
    public static void compareColumns(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> selectedTables) {

        Map<String, List<String>> result = compareColumnsLogic(db1Config, db2Config, selectedTables);
        TablePrinter.printColumnComparison(result,
                db1Config.getDatabaseName(),
                db2Config.getDatabaseName());


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
    private static Map<String, List<String>> compareDataLogic(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> selectedTables) {

        String db1Name = db1Config.getDatabaseName();
        String db2Name = db2Config.getDatabaseName();

        Map<String, List<String>> result = new HashMap<>();
        List<String> sharedTables = getSharedTables(db1Config, db2Config, selectedTables);

        if (sharedTables.isEmpty()) {
            result.put("info", Collections.singletonList("No common tables to compare data."));
            return result;
        }

        boolean hasDifference = false;

        for (String table : sharedTables) {

            Map<String, Map<String, String>> dataInDb1 = TableColumnsService.GetData(db1Config, table);
            Map<String, Map<String, String>> dataInDb2 = TableColumnsService.GetData(db2Config, table);

            List<String> diff = new ArrayList<>();

            // 🔥 جمع جميع IDs
            Set<String> allIds = new HashSet<>();
            allIds.addAll(dataInDb1.keySet());
            allIds.addAll(dataInDb2.keySet());

            for (String id : allIds) {

                Map<String, String> row1 = dataInDb1.get(id);
                Map<String, String> row2 = dataInDb2.get(id);

                // ❗ row ناقص
                if (row1 == null || row2 == null) {
                    diff.add("Row " + id + " is missing in one of the databases");
                    hasDifference = true;
                    continue;
                }

                // 🔍 compare columns
                Set<String> allColumns = new HashSet<>();
                allColumns.addAll(row1.keySet());
                allColumns.addAll(row2.keySet());

                for (String column : allColumns) {

                    String val1 = row1.get(column);
                    String val2 = row2.get(column);

                    if (!Objects.equals(val1, val2)) {
                        diff.add("Row " + id + ", column " + column + " differs: " +
                                db1Name + "=" + val1 + " | " +
                                db2Name + "=" + val2);

                        hasDifference = true;
                    }
                }
            }

            if (!diff.isEmpty()) {
                result.put(table, diff);
            }
        }

        if (!hasDifference) {
            result.put("info", Collections.singletonList("No data differences found."));
        }

        return result;
    }

    // API
    public static Map<String, List<String>> compareDataApi(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> selectedTables) {
        return compareDataLogic(db1Config, db2Config, selectedTables);
    }

    // CONSOLE — now prints a formatted table
    public static void compareData(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> selectedTables) {

        Map<String, List<String>> result = compareDataLogic(db1Config, db2Config, selectedTables);
        TablePrinter.printDataComparison(result,                // ← table output
                db1Config.getDatabaseName(),
                db2Config.getDatabaseName());
    }
}
