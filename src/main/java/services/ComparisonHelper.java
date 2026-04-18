package services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Low-level building blocks shared by all four type-specific comparison services.
 * No public API: callers are FunctionComparisonService, ProcedureComparisonService,
 * TriggerComparisonService, and PackageComparisonService.
 */
final class ComparisonHelper {

    private ComparisonHelper() {}

    // -------------------------------------------------------------------------
    // Single-side diff (ADDED or REMOVED)
    // -------------------------------------------------------------------------

    static DbObjectDiff buildSingleSideDiff(String name,
                                            DbObjectDiff.ObjectType type,
                                            DbObjectDiff.ChangeType changeType,
                                            String source,
                                            Map<String, DatabaseFunctionService.RoutineParameter> params,
                                            boolean fromDb1) {
        DbObjectDiff diff = new DbObjectDiff(name, type, changeType);
        List<String> lines = split(source);

        if (fromDb1) {
            diff.setDb1SourceLines(lines.size());
        } else {
            diff.setDb2SourceLines(lines.size());
        }

        addSource(diff, lines, fromDb1);
        addParams(diff, params, fromDb1);
        return diff;
    }

    // -------------------------------------------------------------------------
    // MODIFIED diff – LCS-based source line comparison
    // -------------------------------------------------------------------------

    /**
     * Builds a MODIFIED diff for functions/procedures/triggers.
     * Uses the full preprocessing + LCS diff pipeline.
     * Returns null when there are no differences.
     */
    static DbObjectDiff buildModified(String name,
                                      DbObjectDiff.ObjectType type,
                                      String s1,
                                      String s2,
                                      Map<String, DatabaseFunctionService.RoutineParameter> p1,
                                      Map<String, DatabaseFunctionService.RoutineParameter> p2) {
        List<String> oldLines = SourcePreprocessor.process(s1);
        List<String> newLines = SourcePreprocessor.process(s2);

        SourceDiffEngine.DiffResult diffResult = SourceDiffEngine.diff(oldLines, newLines);

        DbObjectDiff diff = new DbObjectDiff(name, type, DbObjectDiff.ChangeType.MODIFIED);
        diff.setSimilarity(diffResult.getSimilarity());

        for (SourceDiffEngine.DiffEntry entry : diffResult.getEntries()) {
            if (entry.getType() == SourceDiffEngine.DiffType.EQUAL) continue;

            int lineNum = entry.getOldLineNum() > 0 ? entry.getOldLineNum() : entry.getNewLineNum();
            String charSummary = entry.charChangeSummary();

            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(
                    lineNum,
                    entry.getOldLine(),
                    entry.getNewLine(),
                    entry.getType().name(),
                    charSummary.isEmpty() ? null : charSummary));
        }

        buildParamDiffs(diff, p1, p2);

        if (diff.getSourceDiffs().isEmpty() && diff.getParameterDiffs().isEmpty()) {
            return null;
        }

        diff.setDb1SourceLines(oldLines.size());
        diff.setDb2SourceLines(newLines.size());
        return diff;
    }

    // -------------------------------------------------------------------------
    // Parameter diff helper
    // -------------------------------------------------------------------------

    static void buildParamDiffs(DbObjectDiff diff,
                                Map<String, DatabaseFunctionService.RoutineParameter> p1,
                                Map<String, DatabaseFunctionService.RoutineParameter> p2) {
        if ((p1 == null || p1.isEmpty()) && (p2 == null || p2.isEmpty())) return;

        java.util.Set<String> params = new TreeSet<String>();
        if (p1 != null) params.addAll(p1.keySet());
        if (p2 != null) params.addAll(p2.keySet());

        for (String key : params) {
            DatabaseFunctionService.RoutineParameter left  = p1 != null ? p1.get(key) : null;
            DatabaseFunctionService.RoutineParameter right = p2 != null ? p2.get(key) : null;

            String lt = left  != null ? left.getDataType()  : null;
            String rt = right != null ? right.getDataType() : null;
            String lm = left  != null ? left.getMode()      : null;
            String rm = right != null ? right.getMode()     : null;

            if (!normalize(lt).equals(normalize(rt)) || !normalize(lm).equals(normalize(rm))) {
                diff.getParameterDiffs().add(new DbObjectDiff.ParameterDiffLine(key, lt, rt, lm, rm));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Text utilities
    // -------------------------------------------------------------------------

    static List<String> split(String s) {
        if (s == null || s.isEmpty()) return Collections.emptyList();
        return Arrays.asList(s.replace("\r\n", "\n").split("\n", -1));
    }

    static String normalize(String v) {
        if (v == null) return "";
        return v.trim().replaceAll("\\s+", " ");
    }

    // -------------------------------------------------------------------------
    // Source / parameter line builders
    // -------------------------------------------------------------------------

    private static void addSource(DbObjectDiff diff, List<String> lines, boolean fromDb1) {
        for (int i = 0; i < lines.size(); i++) {
            diff.getSourceDiffs().add(new DbObjectDiff.SourceDiffLine(
                    i + 1,
                    fromDb1 ? null : lines.get(i),
                    fromDb1 ? lines.get(i) : null
            ));
        }
    }

    private static void addParams(DbObjectDiff diff,
                                  Map<String, DatabaseFunctionService.RoutineParameter> map,
                                  boolean fromDb1) {
        if (map == null || map.isEmpty()) return;
        for (Map.Entry<String, DatabaseFunctionService.RoutineParameter> e : map.entrySet()) {
            DatabaseFunctionService.RoutineParameter val = e.getValue();
            diff.getParameterDiffs().add(new DbObjectDiff.ParameterDiffLine(
                    e.getKey(),
                    fromDb1 ? null : val.getDataType(),
                    fromDb1 ? val.getDataType() : null,
                    fromDb1 ? null : val.getMode(),
                    fromDb1 ? val.getMode() : null
            ));
        }
    }
}
