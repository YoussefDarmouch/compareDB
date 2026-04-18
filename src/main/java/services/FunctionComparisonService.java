package services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Compares stored functions between two databases. */
public final class FunctionComparisonService {

    private FunctionComparisonService() {}

    public static List<DbObjectDiff> compare(
            DbConnectionFactory.DbConfig db1,
            DbConnectionFactory.DbConfig db2) {

        java.util.Set<String> names1 = DatabaseFunctionService.getObjectNames(db1, DbObjectDiff.ObjectType.FUNCTION);
        java.util.Set<String> names2 = DatabaseFunctionService.getObjectNames(db2, DbObjectDiff.ObjectType.FUNCTION);

        java.util.Set<String> all = new TreeSet<String>(names1);
        all.addAll(names2);

        List<DbObjectDiff> result = new ArrayList<DbObjectDiff>();

        for (String name : all) {
            boolean in1 = names1.contains(name);
            boolean in2 = names2.contains(name);

            if (in1 && !in2) {
                String src = DatabaseFunctionService.getObjectSource(db1, DbObjectDiff.ObjectType.FUNCTION, name);
                Map<String, DatabaseFunctionService.RoutineParameter> params =
                        DatabaseFunctionService.getObjectParameters(db1, DbObjectDiff.ObjectType.FUNCTION, name);
                result.add(ComparisonHelper.buildSingleSideDiff(
                        name, DbObjectDiff.ObjectType.FUNCTION, DbObjectDiff.ChangeType.ADDED, src, params, true));
                continue;
            }

            if (!in1 && in2) {
                String src = DatabaseFunctionService.getObjectSource(db2, DbObjectDiff.ObjectType.FUNCTION, name);
                Map<String, DatabaseFunctionService.RoutineParameter> params =
                        DatabaseFunctionService.getObjectParameters(db2, DbObjectDiff.ObjectType.FUNCTION, name);
                result.add(ComparisonHelper.buildSingleSideDiff(
                        name, DbObjectDiff.ObjectType.FUNCTION, DbObjectDiff.ChangeType.REMOVED, src, params, false));
                continue;
            }

            // present in both – compare
            String s1 = DatabaseFunctionService.getObjectSource(db1, DbObjectDiff.ObjectType.FUNCTION, name);
            String s2 = DatabaseFunctionService.getObjectSource(db2, DbObjectDiff.ObjectType.FUNCTION, name);
            Map<String, DatabaseFunctionService.RoutineParameter> p1 =
                    DatabaseFunctionService.getObjectParameters(db1, DbObjectDiff.ObjectType.FUNCTION, name);
            Map<String, DatabaseFunctionService.RoutineParameter> p2 =
                    DatabaseFunctionService.getObjectParameters(db2, DbObjectDiff.ObjectType.FUNCTION, name);

            DbObjectDiff modified = ComparisonHelper.buildModified(
                    name, DbObjectDiff.ObjectType.FUNCTION, s1, s2, p1, p2);
            if (modified != null) result.add(modified);
        }

        return result;
    }
}

