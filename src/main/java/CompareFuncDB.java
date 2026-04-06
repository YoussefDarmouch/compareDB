import services.DatabaseFunctionService;
import services.DbConnectionFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class CompareFuncDB {

    public static void CompareFunc(String db1, String db2) {
        CompareFunc(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2)
        );
    }

    public static void CompareFunc(DbConnectionFactory.DbConfig db1Config, DbConnectionFactory.DbConfig db2Config) {
        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();
// name function
        List<String> functionsDb1 = DatabaseFunctionService.checkFun(db1Config);
        List<String> functionsDb2 = DatabaseFunctionService.checkFun(db2Config);

        Set<String> setDb1 = new HashSet<>(functionsDb1);
        Set<String> setDb2 = new HashSet<>(functionsDb2);

        boolean hasDifference = false;
// CompareFunction name 
        for (String functionName : setDb1) {
            if (!setDb2.contains(functionName)) {
                System.out.println("Function exists only in " + db1 + ": " + functionName);
                hasDifference = true;
            }
        }

        for (String functionName : setDb2) {
            if (!setDb1.contains(functionName)) {
                System.out.println("Function exists only in " + db2 + ": " + functionName);
                hasDifference = true;
            }
        }
// Step 1: copy functions from DB1 into a sorted set (TreeSet)
// Step 2: retain only functions that also exist in DB2
        Set<String> commonFunctions = new TreeSet<>(setDb1);
        commonFunctions.retainAll(setDb2);

        for (String functionName : commonFunctions) {
            int db1ParamCount = DatabaseFunctionService.getFunctionParameterCount(db1Config, functionName);
            int db2ParamCount = DatabaseFunctionService.getFunctionParameterCount(db2Config, functionName);

            if (db1ParamCount < 0 || db2ParamCount < 0) {
                System.out.println("Could not read parameter metadata for function: " + functionName);
                hasDifference = true;
                continue;
            }

            if (db1ParamCount != db2ParamCount) {
                System.out.println("Function parameter count differs for " + functionName +
                        " => " + db1 + ": " + db1ParamCount + " | " + db2 + ": " + db2ParamCount);
                hasDifference = true;
                continue;
            }

            DatabaseFunctionService.FunctionExecutionResult resultDb1 =
                    DatabaseFunctionService.executeFunctionForComparison(db1Config, functionName, db1ParamCount);
            DatabaseFunctionService.FunctionExecutionResult resultDb2 =
                    DatabaseFunctionService.executeFunctionForComparison(db2Config, functionName, db2ParamCount);

            if (!resultDb1.isExecutable() || !resultDb2.isExecutable()) {
                System.out.println("Function " + functionName + " => " + resultDb1.getMessage());
                continue;
            }

            if (!resultDb1.isSuccess() || !resultDb2.isSuccess()) {
                System.out.println("Function " + functionName + " output check failed");
                System.out.println("   " + db1 + ": " + resultDb1.getMessage());
                System.out.println("   " + db2 + ": " + resultDb2.getMessage());
                hasDifference = true;
                continue;
            }

            if (!Objects.equals(resultDb1.getValue(), resultDb2.getValue())) {
                System.out.println("Function output differs for " + functionName);
                System.out.println("   " + db1 + ": " + resultDb1.getValue());
                System.out.println("   " + db2 + ": " + resultDb2.getValue());
                hasDifference = true;
            }
        }

        if (!hasDifference) {
            System.out.println("No function differences found (name, signature, output)");
        } else {
            System.out.println("Function comparison completed with differences");
        }
    }

    public static void compareProcedures(String db1, String db2) {
        compareProcedures(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2)
        );
    }

    public static void compareProcedures(DbConnectionFactory.DbConfig db1Config, DbConnectionFactory.DbConfig db2Config) {
        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        List<String> proceduresDb1 = DatabaseFunctionService.getProcedureNames(db1Config);
        List<String> proceduresDb2 = DatabaseFunctionService.getProcedureNames(db2Config);

        Set<String> setDb1 = new HashSet<String>(proceduresDb1);
        Set<String> setDb2 = new HashSet<String>(proceduresDb2);

        boolean hasDifference = false;

        for (String procedureName : setDb1) {
            if (!setDb2.contains(procedureName)) {
                System.out.println("Procedure exists only in " + db1 + ": " + procedureName);
                hasDifference = true;
            }
        }

        for (String procedureName : setDb2) {
            if (!setDb1.contains(procedureName)) {
                System.out.println("Procedure exists only in " + db2 + ": " + procedureName);
                hasDifference = true;
            }
        }

        Set<String> commonProcedures = new TreeSet<String>(setDb1);
        commonProcedures.retainAll(setDb2);

        for (String procedureName : commonProcedures) {
            int db1ParamCount = DatabaseFunctionService.getProcedureParameterCount(db1Config, procedureName);
            int db2ParamCount = DatabaseFunctionService.getProcedureParameterCount(db2Config, procedureName);

            if (db1ParamCount < 0 || db2ParamCount < 0) {
                System.out.println("Could not read parameter metadata for procedure: " + procedureName);
                hasDifference = true;
                continue;
            }

            if (db1ParamCount != db2ParamCount) {
                System.out.println("Procedure parameter count differs for " + procedureName +
                        " => " + db1 + ": " + db1ParamCount + " | " + db2 + ": " + db2ParamCount);
                hasDifference = true;
            }
        }

        if (!hasDifference) {
            System.out.println("No procedure differences found (name, signature)");
        } else {
            System.out.println("Procedure comparison completed with differences");
        }
    }

    public static void compareTriggers(String db1, String db2) {
        compareTriggers(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2)
        );
    }

    public static void compareTriggers(DbConnectionFactory.DbConfig db1Config, DbConnectionFactory.DbConfig db2Config) {
        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        Set<String> triggerNamesDb1 = DatabaseFunctionService.getTriggerNames(db1Config);
        Set<String> triggerNamesDb2 = DatabaseFunctionService.getTriggerNames(db2Config);

        boolean hasDifference = false;

        for (String triggerName : triggerNamesDb1) {
            if (!triggerNamesDb2.contains(triggerName)) {
                System.out.println("Trigger exists only in " + db1 + ": " + triggerName);
                hasDifference = true;
            }
        }

        for (String triggerName : triggerNamesDb2) {
            if (!triggerNamesDb1.contains(triggerName)) {
                System.out.println("Trigger exists only in " + db2 + ": " + triggerName);
                hasDifference = true;
            }
        }

        Set<String> commonTriggers = new TreeSet<String>(triggerNamesDb1);
        commonTriggers.retainAll(triggerNamesDb2);

        for (String triggerName : commonTriggers) {
            DatabaseFunctionService.TriggerInfo infoDb1 = DatabaseFunctionService.getTriggerInfo(db1Config, triggerName);
            DatabaseFunctionService.TriggerInfo infoDb2 = DatabaseFunctionService.getTriggerInfo(db2Config, triggerName);

            if (infoDb1 == null || infoDb2 == null) {
                System.out.println("Could not read metadata for trigger: " + triggerName);
                hasDifference = true;
                continue;
            }

            if (!infoDb1.sameAs(infoDb2)) {
                System.out.println("Trigger definition differs for " + triggerName);
                System.out.println("   " + db1 + ": " + infoDb1.describe());
                System.out.println("   " + db2 + ": " + infoDb2.describe());
                hasDifference = true;
            }
        }

        if (!hasDifference) {
            System.out.println("No trigger differences found (name, event, timing, statement)");
        } else {
            System.out.println("Trigger comparison completed with differences");
        }
    }
}