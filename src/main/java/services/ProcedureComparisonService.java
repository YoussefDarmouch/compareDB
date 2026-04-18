package services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Compares stored procedures between two databases. */
public final class ProcedureComparisonService {

    private ProcedureComparisonService() {}

    public static List<DbObjectDiff> compare(
            DbConnectionFactory.DbConfig db1,
            DbConnectionFactory.DbConfig db2) {

        java.util.Set<String> names1 = DatabaseFunctionService.getObjectNames(db1, DbObjectDiff.ObjectType.PROCEDURE);
        java.util.Set<String> names2 = DatabaseFunctionService.getObjectNames(db2, DbObjectDiff.ObjectType.PROCEDURE);

        java.util.Set<String> all = new TreeSet<String>(names1);
        all.addAll(names2);

        List<DbObjectDiff> result = new ArrayList<DbObjectDiff>();

        for (String name : all) {
            boolean in1 = names1.contains(name);
            boolean in2 = names2.contains(name);

            if (in1 && !in2) {
                String src = DatabaseFunctionService.getObjectSource(db1, DbObjectDiff.ObjectType.PROCEDURE, name);
                Map<String, DatabaseFunctionService.RoutineParameter> params =
                        DatabaseFunctionService.getObjectParameters(db1, DbObjectDiff.ObjectType.PROCEDURE, name);
                result.add(ComparisonHelper.buildSingleSideDiff(
                        name, DbObjectDiff.ObjectType.PROCEDURE, DbObjectDiff.ChangeType.ADDED, src, params, true));
                continue;
            }

            if (!in1 && in2) {
                String src = DatabaseFunctionService.getObjectSource(db2, DbObjectDiff.ObjectType.PROCEDURE, name);
                Map<String, DatabaseFunctionService.RoutineParameter> params =
                        DatabaseFunctionService.getObjectParameters(db2, DbObjectDiff.ObjectType.PROCEDURE, name);
                result.add(ComparisonHelper.buildSingleSideDiff(
                        name, DbObjectDiff.ObjectType.PROCEDURE, DbObjectDiff.ChangeType.REMOVED, src, params, false));
                continue;
            }

            // present in both – compare
            String s1 = DatabaseFunctionService.getObjectSource(db1, DbObjectDiff.ObjectType.PROCEDURE, name);
            String s2 = DatabaseFunctionService.getObjectSource(db2, DbObjectDiff.ObjectType.PROCEDURE, name);
            Map<String, DatabaseFunctionService.RoutineParameter> p1 =
                    DatabaseFunctionService.getObjectParameters(db1, DbObjectDiff.ObjectType.PROCEDURE, name);
            Map<String, DatabaseFunctionService.RoutineParameter> p2 =
                    DatabaseFunctionService.getObjectParameters(db2, DbObjectDiff.ObjectType.PROCEDURE, name);

            DbObjectDiff modified = ComparisonHelper.buildModified(
                    name, DbObjectDiff.ObjectType.PROCEDURE, s1, s2, p1, p2);
            if (modified != null) result.add(modified);
        }

        return result;
    }
}

