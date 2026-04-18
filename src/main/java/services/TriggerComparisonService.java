package services;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.TreeSet;

/**
 * Compares triggers between two databases.
 * Triggers have no routine parameters so only source diffs are produced.
 */
public final class TriggerComparisonService {

    private TriggerComparisonService() {}

    public static List<DbObjectDiff> compare(
            DbConnectionFactory.DbConfig db1,
            DbConnectionFactory.DbConfig db2) {

        java.util.Set<String> names1 = DatabaseFunctionService.getObjectNames(db1, DbObjectDiff.ObjectType.TRIGGER);
        java.util.Set<String> names2 = DatabaseFunctionService.getObjectNames(db2, DbObjectDiff.ObjectType.TRIGGER);

        java.util.Set<String> all = new TreeSet<String>(names1);
        all.addAll(names2);

        List<DbObjectDiff> result = new ArrayList<DbObjectDiff>();

        for (String name : all) {
            boolean in1 = names1.contains(name);
            boolean in2 = names2.contains(name);

            if (in1 && !in2) {
                String src = DatabaseFunctionService.getObjectSource(db1, DbObjectDiff.ObjectType.TRIGGER, name);
                result.add(ComparisonHelper.buildSingleSideDiff(
                        name, DbObjectDiff.ObjectType.TRIGGER, DbObjectDiff.ChangeType.ADDED,
                        src, Collections.<String, DatabaseFunctionService.RoutineParameter>emptyMap(), true));
                continue;
            }

            if (!in1 && in2) {
                String src = DatabaseFunctionService.getObjectSource(db2, DbObjectDiff.ObjectType.TRIGGER, name);
                result.add(ComparisonHelper.buildSingleSideDiff(
                        name, DbObjectDiff.ObjectType.TRIGGER, DbObjectDiff.ChangeType.REMOVED,
                        src, Collections.<String, DatabaseFunctionService.RoutineParameter>emptyMap(), false));
                continue;
            }

            // present in both – compare source only (no params for triggers)
            String s1 = DatabaseFunctionService.getObjectSource(db1, DbObjectDiff.ObjectType.TRIGGER, name);
            String s2 = DatabaseFunctionService.getObjectSource(db2, DbObjectDiff.ObjectType.TRIGGER, name);

            DbObjectDiff modified = ComparisonHelper.buildModified(
                    name, DbObjectDiff.ObjectType.TRIGGER, s1, s2,
                    Collections.<String, DatabaseFunctionService.RoutineParameter>emptyMap(),
                    Collections.<String, DatabaseFunctionService.RoutineParameter>emptyMap());
            if (modified != null) result.add(modified);
        }

        return result;
    }
}

