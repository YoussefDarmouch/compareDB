package Package;

import services.DbConnectionFactory;
import services.DbLabelUtils;
import services.DbObjectComparisonService;
import services.DbObjectDiff;
import services.DbObjectDiffFormatter;
import services.TablePrinter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        return compareByType(db1Config, db2Config, DbObjectDiff.ObjectType.FUNCTION, "functions", "No function differences found");
    }

    public static Map<String, List<String>> compareFunctionsApi(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return compareFunctionsLogic(db1Config, db2Config);
    }

    public static void compareFunctions(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        Map<String, List<String>> result = compareFunctionsLogic(db1Config, db2Config);
        TablePrinter.printComparisonFunction(result, buildDisplayLabel(db1Config), buildDisplayLabel(db2Config));
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

        return compareByType(db1Config, db2Config, DbObjectDiff.ObjectType.PROCEDURE, "procedures", "No procedure differences found");
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
        TablePrinter.printComparisonProcedures(result, buildDisplayLabel(db1Config), buildDisplayLabel(db2Config));
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

        return compareByType(db1Config, db2Config, DbObjectDiff.ObjectType.TRIGGER, "triggers", "No trigger differences found");
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
        TablePrinter.printComparisonTrigger(result, buildDisplayLabel(db1Config), buildDisplayLabel(db2Config));
    }

    public static void comparePackages(String db1, String db2) {
        comparePackages(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2)
        );
    }

    private static Map<String, List<String>> comparePackagesLogic(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return compareByType(db1Config, db2Config, DbObjectDiff.ObjectType.PACKAGE, "packages", "No package differences found");
    }

    public static Map<String, List<String>> comparePackagesApi(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return comparePackagesLogic(db1Config, db2Config);
    }

    public static void comparePackages(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        Map<String, List<String>> result = comparePackagesLogic(db1Config, db2Config);
        TablePrinter.printComparisonPackages(result, buildDisplayLabel(db1Config), buildDisplayLabel(db2Config));
    }

    private static Map<String, List<String>> compareByType(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            DbObjectDiff.ObjectType objectType,
            String resultKey,
            String noDiffMessage) {

        List<DbObjectDiff> diffs = DbObjectComparisonService.compareObjects(db1Config, db2Config, objectType);

        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        if (diffs.isEmpty()) {
            result.put("info", Collections.singletonList(noDiffMessage));
            return result;
        }

        List<String> formattedBlocks = new ArrayList<String>();
        String db1Label = buildDisplayLabel(db1Config);
        String db2Label = buildDisplayLabel(db2Config);

        for (DbObjectDiff diff : diffs) {
            formattedBlocks.add(DbObjectDiffFormatter.formatSummaryRow(
                    diff,
                    db1Label,
                    db2Label));
        }

        result.put(resultKey, formattedBlocks);
        return result;
    }

    private static String buildDisplayLabel(DbConnectionFactory.DbConfig config) {
        return DbLabelUtils.displayName(config);
    }
}
