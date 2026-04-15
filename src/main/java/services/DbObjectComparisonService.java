package services;

import java.util.*;

public final class DbObjectComparisonService {

    private DbObjectComparisonService() {}

    public static List<DbObjectDiff> compareObjects(
            DbConnectionFactory.DbConfig db1,
            DbConnectionFactory.DbConfig db2,
            DbObjectDiff.ObjectType objectType) {

        Set<String> names1 = DatabaseFunctionService.getObjectNames(db1, objectType);
        Set<String> names2 = DatabaseFunctionService.getObjectNames(db2, objectType);

        Set<String> all = new TreeSet<String>();
        all.addAll(names1);
        all.addAll(names2);

        List<DbObjectDiff> result = new ArrayList<DbObjectDiff>();

        for (String name : all) {

            boolean in1 = names1.contains(name);
            boolean in2 = names2.contains(name);

            // ================= ADDED =================
            if (in1 && !in2) {
                DbObjectDiff diff = new DbObjectDiff(name, objectType, DbObjectDiff.ChangeType.ADDED);

                addSource(diff,
                        DatabaseFunctionService.getObjectSource(db1, objectType, name),
                        true);

                addParams(diff,
                        DatabaseFunctionService.getObjectParameters(db1, objectType, name),
                        true);

                result.add(diff);
                continue;
            }

            // ================= REMOVED =================
            if (!in1 && in2) {
                DbObjectDiff diff = new DbObjectDiff(name, objectType, DbObjectDiff.ChangeType.REMOVED);

                addSource(diff,
                        DatabaseFunctionService.getObjectSource(db2, objectType, name),
                        false);

                addParams(diff,
                        DatabaseFunctionService.getObjectParameters(db2, objectType, name),
                        false);

                result.add(diff);
                continue;
            }

            // ================= MODIFIED =================
            String s1 = DatabaseFunctionService.getObjectSource(db1, objectType, name);
            String s2 = DatabaseFunctionService.getObjectSource(db2, objectType, name);

            Map<String, DatabaseFunctionService.RoutineParameter> p1 =
                    DatabaseFunctionService.getObjectParameters(db1, objectType, name);

            Map<String, DatabaseFunctionService.RoutineParameter> p2 =
                    DatabaseFunctionService.getObjectParameters(db2, objectType, name);

            DbObjectDiff modified = buildModified(name, objectType, s1, s2, p1, p2);

            if (modified != null) {
                result.add(modified);
            }
        }

        return result;
    }

    // =========================================================
    // MODIFIED
    // =========================================================
    private static DbObjectDiff buildModified(
            String name,
            DbObjectDiff.ObjectType type,
            String s1,
            String s2,
            Map<String, DatabaseFunctionService.RoutineParameter> p1,
            Map<String, DatabaseFunctionService.RoutineParameter> p2) {

        List<String> oldLines = split(s1);
        List<String> newLines = split(s2);

        DbObjectDiff diff = new DbObjectDiff(name, type, DbObjectDiff.ChangeType.MODIFIED);

        int max = Math.max(oldLines.size(), newLines.size());

        for (int i = 0; i < max; i++) {

            String o = i < oldLines.size() ? oldLines.get(i) : null;
            String n = i < newLines.size() ? newLines.get(i) : null;

            if (!normalize(o).equals(normalize(n))) {
                diff.getSourceDiffs().add(
                        new DbObjectDiff.SourceDiffLine(i + 1, o, n)
                );
            }
        }

        // ================= PARAMETERS =================
        Set<String> params = new TreeSet<String>();
        params.addAll(p1.keySet());
        params.addAll(p2.keySet());

        for (String key : params) {

            DatabaseFunctionService.RoutineParameter left = p1.get(key);
            DatabaseFunctionService.RoutineParameter right = p2.get(key);

            String lt = left != null ? left.getDataType() : null;
            String rt = right != null ? right.getDataType() : null;

            String lm = left != null ? left.getMode() : null;
            String rm = right != null ? right.getMode() : null;

            if (!normalize(lt).equals(normalize(rt)) ||
                    !normalize(lm).equals(normalize(rm))) {

                diff.getParameterDiffs().add(
                        new DbObjectDiff.ParameterDiffLine(key, lt, rt, lm, rm)
                );
            }
        }

        if (diff.getSourceDiffs().isEmpty() && diff.getParameterDiffs().isEmpty()) {
            return null;
        }

        return diff;
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private static void addSource(DbObjectDiff diff, String src, boolean added) {

        List<String> lines = split(src);

        for (int i = 0; i < lines.size(); i++) {

            diff.getSourceDiffs().add(
                    new DbObjectDiff.SourceDiffLine(
                            i + 1,
                            added ? null : lines.get(i),
                            added ? lines.get(i) : null
                    )
            );
        }
    }

    private static void addParams(DbObjectDiff diff,
                                  Map<String, DatabaseFunctionService.RoutineParameter> map,
                                  boolean added) {

        for (Map.Entry<String, DatabaseFunctionService.RoutineParameter> e : map.entrySet()) {

            String key = e.getKey();
            DatabaseFunctionService.RoutineParameter val = e.getValue();

            diff.getParameterDiffs().add(
                    new DbObjectDiff.ParameterDiffLine(
                            key,
                            added ? null : val.getDataType(),
                            added ? val.getDataType() : null,
                            added ? null : val.getMode(),
                            added ? val.getMode() : null
                    )
            );
        }
    }

    private static List<String> split(String s) {
        if (s == null || s.isEmpty()) return Collections.emptyList();
        return Arrays.asList(s.replace("\r\n", "\n").split("\n", -1));
    }

    private static String normalize(String v) {
        if (v == null) return "";
        return v.trim().replaceAll("\\s+", " ");
    }
}