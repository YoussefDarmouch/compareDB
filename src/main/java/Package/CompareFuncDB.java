package Package;

import services.DatabaseFunctionService;
import services.DbConnectionFactory;
import services.TablePrinter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Collections;

public class CompareFuncDB {

    public static void CompareFunc(String db1, String db2) {
        compareFunctions(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2)
        );
    }


        private static Map<String, List<String>> compareFunctionsLogic(
                DbConnectionFactory.DbConfig db1Config,
                DbConnectionFactory.DbConfig db2Config) {

            String db1 = db1Config.getDatabaseName();
            String db2 = db2Config.getDatabaseName();

            Map<String, List<String>> result = new HashMap<>();

            List<String> functionsDb1 = DatabaseFunctionService.checkFun(db1Config);
            List<String> functionsDb2 = DatabaseFunctionService.checkFun(db2Config);

            Set<String> setDb1 = new HashSet<>(functionsDb1);
            Set<String> setDb2 = new HashSet<>(functionsDb2);

            List<String> diff = new ArrayList<>();

            // functions only in db1
            for (String functionName : setDb1) {
                if (!setDb2.contains(functionName)) {
                    diff.add("Function only in " + db1 + ": " + functionName);
                }
            }

            // functions only in db2
            for (String functionName : setDb2) {
                if (!setDb1.contains(functionName)) {
                    diff.add("Function only in " + db2 + ": " + functionName);
                }
            }

            // common functions
            Set<String> commonFunctions = new TreeSet<>(setDb1);
            commonFunctions.retainAll(setDb2);

            for (String functionName : commonFunctions) {

                int db1ParamCount = DatabaseFunctionService.getFunctionParameterCount(db1Config, functionName);
                int db2ParamCount = DatabaseFunctionService.getFunctionParameterCount(db2Config, functionName);

                if (db1ParamCount < 0 || db2ParamCount < 0) {
                    diff.add("Cannot read parameters for function: " + functionName);
                    continue;
                }

                if (db1ParamCount != db2ParamCount) {
                    diff.add("Parameter count differs for " + functionName +
                            " => " + db1 + ": " + db1ParamCount +
                            " | " + db2 + ": " + db2ParamCount);
                    continue;
                }

                DatabaseFunctionService.FunctionExecutionResult resultDb1 =
                        DatabaseFunctionService.executeFunctionForComparison(db1Config, functionName, db1ParamCount);

                DatabaseFunctionService.FunctionExecutionResult resultDb2 =
                        DatabaseFunctionService.executeFunctionForComparison(db2Config, functionName, db2ParamCount);

                if (!resultDb1.isExecutable() || !resultDb2.isExecutable()) {
                    diff.add("Function " + functionName + " not executable: " + resultDb1.getMessage());
                    continue;
                }

                if (!resultDb1.isSuccess() || !resultDb2.isSuccess()) {
                    diff.add("Function " + functionName + " execution failed");
                    diff.add("   " + db1 + ": " + resultDb1.getMessage());
                    diff.add("   " + db2 + ": " + resultDb2.getMessage());
                    continue;
                }

                if (!Objects.equals(resultDb1.getValue(), resultDb2.getValue())) {
                    diff.add("Function output differs: " + functionName);
                    diff.add("   " + db1 + ": " + resultDb1.getValue());
                    diff.add("   " + db2 + ": " + resultDb2.getValue());
                }
            }

            if (diff.isEmpty()) {
                result.put("info", Collections.singletonList("No function differences found"));
            } else {
                result.put("functions", diff);
            }

            return result;
        }

        //  API
        public static Map<String, List<String>> compareFunctionsApi(
                DbConnectionFactory.DbConfig db1Config,
                DbConnectionFactory.DbConfig db2Config) {

            return compareFunctionsLogic(db1Config, db2Config);
        }

        //  CONSOLE
        public static void compareFunctions(
                DbConnectionFactory.DbConfig db1Config,
                DbConnectionFactory.DbConfig db2Config) {

            Map<String, List<String>>result = compareFunctionsLogic(db1Config, db2Config);

            TablePrinter.printComparisonFunction(result);

        }


    public static void compareProcedures(String db1, String db2) {
        compareProcedures(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2)
        );
    }
    public static Map<String, List<String>> compareProceduresLogic(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        Map<String, List<String>> result = new HashMap<>();
        List<String> differences = new ArrayList<>();

        List<String> proceduresDb1 = DatabaseFunctionService.getProcedureNames(db1Config);
        List<String> proceduresDb2 = DatabaseFunctionService.getProcedureNames(db2Config);

        Set<String> setDb1 = new HashSet<>(proceduresDb1);
        Set<String> setDb2 = new HashSet<>(proceduresDb2);

        // ===== Procedures only in DB1 =====
        for (String procedureName : setDb1) {
            if (!setDb2.contains(procedureName)) {
                differences.add("Procedure only in " + db1 + ": " + procedureName);
            }
        }

        // ===== Procedures only in DB2 =====
        for (String procedureName : setDb2) {
            if (!setDb1.contains(procedureName)) {
                differences.add("Procedure only in " + db2 + ": " + procedureName);
            }
        }

        // ===== Common procedures =====
        Set<String> commonProcedures = new TreeSet<>(setDb1);
        commonProcedures.retainAll(setDb2);

        for (String procedureName : commonProcedures) {

            int db1ParamCount = DatabaseFunctionService.getProcedureParameterCount(db1Config, procedureName);
            int db2ParamCount = DatabaseFunctionService.getProcedureParameterCount(db2Config, procedureName);

            if (db1ParamCount < 0 || db2ParamCount < 0) {
                differences.add("Cannot read metadata for procedure: " + procedureName);
                continue;
            }

            if (db1ParamCount != db2ParamCount) {
                differences.add("Parameter count differs for " + procedureName +
                        " => " + db1 + ": " + db1ParamCount +
                        " | " + db2 + ": " + db2ParamCount);
            }
        }

        if (differences.isEmpty()) {
            result.put("info", Collections.singletonList("No procedure differences found"));
        } else {
            result.put("procedures", differences);
        }

        return result;
    }
    public static Map<String, List<String>> compareProceduresApi(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {


        return compareProceduresLogic(db1Config, db2Config);
    }
    public static void compareProcedures(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        Map<String, List<String>> result = compareProceduresLogic(db1Config, db2Config);



       TablePrinter.printComparisonProcedures(result);

    }
    public static void compareTriggers(String db1, String db2) {
        compareTriggers(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2)
        );
    }
    private static Map<String, List<String>> compareTriggersLogic(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        Map<String, List<String>> result = new HashMap<>();
        List<String> differences = new ArrayList<>();

        Set<String> triggerNamesDb1 = DatabaseFunctionService.getTriggerNames(db1Config);
        Set<String> triggerNamesDb2 = DatabaseFunctionService.getTriggerNames(db2Config);

        // ===== Only in DB1 =====
        for (String triggerName : triggerNamesDb1) {
            if (!triggerNamesDb2.contains(triggerName)) {
                differences.add("Trigger only in " + db1 + ": " + triggerName);
            }
        }

        // ===== Only in DB2 =====
        for (String triggerName : triggerNamesDb2) {
            if (!triggerNamesDb1.contains(triggerName)) {
                differences.add("Trigger only in " + db2 + ": " + triggerName);
            }
        }

        // ===== Common triggers =====
        Set<String> commonTriggers = new TreeSet<>(triggerNamesDb1);
        commonTriggers.retainAll(triggerNamesDb2);

        for (String triggerName : commonTriggers) {

            DatabaseFunctionService.TriggerInfo infoDb1 =
                    DatabaseFunctionService.getTriggerInfo(db1Config, triggerName);

            DatabaseFunctionService.TriggerInfo infoDb2 =
                    DatabaseFunctionService.getTriggerInfo(db2Config, triggerName);

            if (infoDb1 == null || infoDb2 == null) {
                differences.add("Cannot read metadata for trigger: " + triggerName);
                continue;
            }

            if (!infoDb1.sameAs(infoDb2)) {
                differences.add("Trigger differs: " + triggerName);
                differences.add("   " + db1 + ": " + infoDb1.describe());
                differences.add("   " + db2 + ": " + infoDb2.describe());
            }
        }

        if (differences.isEmpty()) {
            result.put("info", Collections.singletonList("No trigger differences found"));
        } else {
            result.put("triggers", differences);
        }

        return result;
    }
    public static Map<String, List<String>> compareTriggersApi(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return compareTriggersLogic(db1Config, db2Config);
    }
    public static void compareTriggers(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        Map<String, List<String>> result = compareTriggersLogic(db1Config, db2Config);

        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();
        TablePrinter.printComparisonTrigger(result);


    }


}