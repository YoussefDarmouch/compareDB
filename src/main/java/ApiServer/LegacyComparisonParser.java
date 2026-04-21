package ApiServer;

import ApiServer.dto.ColumnDifferenceResult;
import ApiServer.dto.DataDifferenceResult;
import ApiServer.dto.DatabaseSideSummary;
import ApiServer.dto.DbObjectComparisonResult;
import ApiServer.dto.FunctionResult;
import ApiServer.dto.LineChange;
import ApiServer.dto.PackageResult;
import ApiServer.dto.ParameterChange;
import ApiServer.dto.ProcedureResult;
import ApiServer.dto.SubProgramResult;
import ApiServer.dto.TableDifferenceResult;
import ApiServer.dto.TriggerResult;
import ApiServer.dto.TypeDifferenceResult;
import services.DbObjectDiff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegacyComparisonParser {

    private static final Pattern LINE_COUNT_PATTERN = Pattern.compile("(\\d+)\\s+lines?", Pattern.CASE_INSENSITIVE);

    private LegacyComparisonParser() {
    }

    public static List<TableDifferenceResult> parseTableDifferences(List<String> legacyRows) {
        List<TableDifferenceResult> results = new ArrayList<TableDifferenceResult>();
        if (legacyRows == null) {
            return results;
        }

        for (String row : legacyRows) {
            if (isInformationalMessage(row)) {
                continue;
            }
            results.add(parseTableDifference(row));
        }

        return results;
    }

    public static List<ColumnDifferenceResult> parseColumnDifferences(Map<String, List<String>> legacyResult) {
        List<ColumnDifferenceResult> results = new ArrayList<ColumnDifferenceResult>();
        if (legacyResult == null) {
            return results;
        }

        for (Map.Entry<String, List<String>> entry : legacyResult.entrySet()) {
            if (isInfoKey(entry.getKey())) {
                continue;
            }

            String tableName = entry.getKey();
            List<String> rows = entry.getValue();
            if (rows == null) {
                continue;
            }

            for (String row : rows) {
                if (isInformationalMessage(row)) {
                    continue;
                }
                results.add(parseColumnDifference(tableName, row));
            }
        }

        return results;
    }

    public static List<TypeDifferenceResult> parseTypeDifferences(Map<String, List<String>> legacyResult,
                                                                  String db1Name,
                                                                  String db2Name) {
        List<TypeDifferenceResult> results = new ArrayList<TypeDifferenceResult>();
        if (legacyResult == null) {
            return results;
        }

        for (Map.Entry<String, List<String>> entry : legacyResult.entrySet()) {
            if (isInfoKey(entry.getKey())) {
                continue;
            }

            String tableName = entry.getKey();
            List<String> rows = entry.getValue();
            if (rows == null) {
                continue;
            }

            for (String row : rows) {
                if (isInformationalMessage(row)) {
                    continue;
                }
                results.add(parseTypeDifference(tableName, row, db1Name, db2Name));
            }
        }

        return results;
    }

    public static List<DataDifferenceResult> parseDataDifferences(Map<String, List<String>> legacyResult,
                                                                  String db1Name,
                                                                  String db2Name) {
        List<DataDifferenceResult> results = new ArrayList<DataDifferenceResult>();
        if (legacyResult == null) {
            return results;
        }

        for (Map.Entry<String, List<String>> entry : legacyResult.entrySet()) {
            if (isInfoKey(entry.getKey())) {
                continue;
            }

            String tableName = entry.getKey();
            List<String> rows = entry.getValue();
            if (rows == null) {
                continue;
            }

            for (String row : rows) {
                if (isInformationalMessage(row)) {
                    continue;
                }
                results.add(parseDataDifference(tableName, row, db1Name, db2Name));
            }
        }

        return results;
    }

    public static List<FunctionResult> parseFunctionResults(List<String> legacyRows, List<DbObjectDiff> diffs) {
        return parseObjectResults(legacyRows, diffs, new ObjectResultFactory<FunctionResult>() {
            @Override
            public FunctionResult create() {
                return new FunctionResult();
            }
        }, "FUNCTION");
    }

    public static List<ProcedureResult> parseProcedureResults(List<String> legacyRows, List<DbObjectDiff> diffs) {
        return parseObjectResults(legacyRows, diffs, new ObjectResultFactory<ProcedureResult>() {
            @Override
            public ProcedureResult create() {
                return new ProcedureResult();
            }
        }, "PROCEDURE");
    }

    public static List<TriggerResult> parseTriggerResults(List<String> legacyRows, List<DbObjectDiff> diffs) {
        return parseObjectResults(legacyRows, diffs, new ObjectResultFactory<TriggerResult>() {
            @Override
            public TriggerResult create() {
                return new TriggerResult();
            }
        }, "TRIGGER");
    }

    public static List<PackageResult> parsePackageResults(List<String> legacyRows, List<DbObjectDiff> diffs) {
        return parseObjectResults(legacyRows, diffs, new ObjectResultFactory<PackageResult>() {
            @Override
            public PackageResult create() {
                return new PackageResult();
            }
        }, "PACKAGE");
    }

    private static TableDifferenceResult parseTableDifference(String row) {
        TableDifferenceResult result = new TableDifferenceResult();
        result.setLegacyMessage(row);

        String prefix = "Tables missing in ";
        if (row != null && row.startsWith(prefix)) {
            int separator = row.indexOf(": ", prefix.length());
            if (separator > prefix.length()) {
                result.setMissingIn(row.substring(prefix.length(), separator).trim());
                result.setTableName(row.substring(separator + 2).trim());
                result.setStatus("MISSING_TABLE");
                return result;
            }
        }

        result.setStatus("UNPARSEABLE");
        return result;
    }

    private static ColumnDifferenceResult parseColumnDifference(String tableName, String row) {
        ColumnDifferenceResult result = new ColumnDifferenceResult();
        result.setTableName(tableName);
        result.setLegacyMessage(row);

        String prefix = "column exists only in ";
        if (row != null && row.toLowerCase().startsWith(prefix)) {
            int separator = row.indexOf(": ", prefix.length());
            if (separator > prefix.length()) {
                result.setOnlyIn(row.substring(prefix.length(), separator).trim());
                result.setColumnName(row.substring(separator + 2).trim());
                result.setStatus("COLUMN_MISSING");
                return result;
            }
        }

        result.setStatus("UNPARSEABLE");
        return result;
    }

    private static TypeDifferenceResult parseTypeDifference(String tableName,
                                                            String row,
                                                            String db1Name,
                                                            String db2Name) {
        TypeDifferenceResult result = new TypeDifferenceResult();
        result.setTableName(tableName);
        result.setDb1Name(db1Name);
        result.setDb2Name(db2Name);
        result.setLegacyMessage(row);

        String prefix = "Column ";
        String marker = " => ";
        if (row == null || !row.startsWith(prefix)) {
            result.setStatus("UNPARSEABLE");
            return result;
        }

        int markerIndex = row.indexOf(marker, prefix.length());
        if (markerIndex <= prefix.length()) {
            result.setStatus("UNPARSEABLE");
            return result;
        }

        result.setColumnName(row.substring(prefix.length(), markerIndex).trim());

        String remainder = row.substring(markerIndex + marker.length());
        String db1Prefix = safe(db1Name) + ": ";
        String db2Prefix = " | " + safe(db2Name) + ": ";

        if (remainder.startsWith(db1Prefix)) {
            int db2Index = remainder.indexOf(db2Prefix, db1Prefix.length());
            if (db2Index >= 0) {
                result.setDb1Type(remainder.substring(db1Prefix.length(), db2Index));
                result.setDb2Type(remainder.substring(db2Index + db2Prefix.length()));
                result.setStatus("TYPE_MISMATCH");
                return result;
            }
        }

        result.setStatus("UNPARSEABLE");
        return result;
    }

    private static DataDifferenceResult parseDataDifference(String tableName,
                                                            String row,
                                                            String db1Name,
                                                            String db2Name) {
        DataDifferenceResult result = new DataDifferenceResult();
        result.setTableName(tableName);
        result.setDb1Name(db1Name);
        result.setDb2Name(db2Name);
        result.setLegacyMessage(row);

        if (row == null) {
            result.setStatus("UNPARSEABLE");
            return result;
        }

        if (row.startsWith("Row ") && row.endsWith(" is missing in both databases")) {
            result.setRowId(row.substring(4, row.length() - " is missing in both databases".length()).trim());
            result.setStatus("ROW_MISSING_IN_BOTH");
            return result;
        }

        String rowPrefix = "Row ";
        String columnMarker = ", column ";
        String differsMarker = " differs: ";

        if (!row.startsWith(rowPrefix)) {
            result.setStatus("UNPARSEABLE");
            return result;
        }

        int columnIndex = row.indexOf(columnMarker, rowPrefix.length());
        if (columnIndex < 0) {
            result.setStatus("UNPARSEABLE");
            return result;
        }

        int differsIndex = row.indexOf(differsMarker, columnIndex + columnMarker.length());
        if (differsIndex < 0) {
            result.setStatus("UNPARSEABLE");
            return result;
        }

        result.setRowId(row.substring(rowPrefix.length(), columnIndex).trim());
        result.setColumnName(row.substring(columnIndex + columnMarker.length(), differsIndex).trim());

        String valuesPart = row.substring(differsIndex + differsMarker.length());
        String db1Prefix = safe(db1Name) + "=";
        String db2Prefix = " | " + safe(db2Name) + "=";

        if (valuesPart.startsWith(db1Prefix)) {
            int db2Index = valuesPart.indexOf(db2Prefix, db1Prefix.length());
            if (db2Index >= 0) {
                result.setDb1Value(valuesPart.substring(db1Prefix.length(), db2Index));
                result.setDb2Value(valuesPart.substring(db2Index + db2Prefix.length()));
                result.setStatus("VALUE_MISMATCH");
                return result;
            }
        }

        result.setStatus("UNPARSEABLE");
        return result;
    }

    private static <T extends DbObjectComparisonResult> List<T> parseObjectResults(List<String> legacyRows,
                                                                                    List<DbObjectDiff> diffs,
                                                                                    ObjectResultFactory<T> factory,
                                                                                    String defaultObjectType) {
        List<T> results = new ArrayList<T>();
        if (legacyRows == null) {
            return results;
        }

        Map<String, DbObjectDiff> diffByName = new LinkedHashMap<String, DbObjectDiff>();
        if (diffs != null) {
            for (DbObjectDiff diff : diffs) {
                if (diff != null && diff.getObjectName() != null) {
                    diffByName.put(diff.getObjectName(), diff);
                }
            }
        }

        for (String row : legacyRows) {
            if (isInformationalMessage(row)) {
                continue;
            }

            T result = factory.create();
            applyParsedObjectRow(result, row, defaultObjectType);

            DbObjectDiff diff = diffByName.get(result.getName());
            if (diff != null) {
                applyDiffDetails(result, diff);
            }

            if (result.getChangeType() == null || result.getChangeType().trim().isEmpty()) {
                result.setChangeType(resolveChangeType(result));
            }

            results.add(result);
        }

        return results;
    }

    private static void applyParsedObjectRow(DbObjectComparisonResult result,
                                             String row,
                                             String defaultObjectType) {
        result.setLegacySummary(row);
        result.setObjectType(defaultObjectType);

        if (row == null || row.trim().isEmpty()) {
            result.setStatus("UNPARSEABLE");
            result.setRisk("UNKNOWN");
            result.setDetails("Empty legacy row");
            return;
        }

        String[] parts = row.split("\\|", 6);
        if (parts.length < 6) {
            result.setName(row.trim());
            result.setStatus("UNPARSEABLE");
            result.setRisk("UNKNOWN");
            result.setDetails("Unable to parse legacy summary row");
            return;
        }

        result.setName(parts[0].trim());
        result.setStatus(parts[1].trim());
        result.setDb1(parseDatabaseSide(parts[2]));
        result.setDb2(parseDatabaseSide(parts[3]));
        result.setRisk(parts[4].trim());
        result.setDetails(parts[5].trim());
    }

    private static DatabaseSideSummary parseDatabaseSide(String rawSummary) {
        DatabaseSideSummary result = new DatabaseSideSummary();
        String summary = rawSummary != null ? rawSummary.trim() : "";
        result.setSummary(summary);
        result.setPresent(!summary.isEmpty() && !"MISSING".equalsIgnoreCase(summary));

        Matcher matcher = LINE_COUNT_PATTERN.matcher(summary);
        if (matcher.find()) {
            result.setSourceLineCount(Integer.valueOf(matcher.group(1)));
        }

        return result;
    }

    private static void applyDiffDetails(DbObjectComparisonResult result, DbObjectDiff diff) {
        if (diff == null) {
            return;
        }

        if (diff.getObjectName() != null) {
            result.setName(diff.getObjectName());
        }
        if (diff.getObjectType() != null) {
            result.setObjectType(diff.getObjectType().name());
        }
        if (diff.getChangeType() != null) {
            result.setChangeType(diff.getChangeType().name());
            syncPresenceFromChangeType(result, diff.getChangeType());
        }
        if (diff.getSimilarity() >= 0) {
            result.setSimilarity(Double.valueOf(diff.getSimilarity()));
        }

        result.setExecutionResult(diff.getExecutionResult());
        result.setLineChanges(toLineChanges(diff.getSourceDiffs()));
        result.setParameterChanges(toParameterChanges(diff.getParameterDiffs()));
        result.setSubPrograms(toSubPrograms(diff.getSubProgramDiffs()));

        applySourceLineCount(result.getDb1(), diff.getDb1SourceLines());
        applySourceLineCount(result.getDb2(), diff.getDb2SourceLines());
    }

    private static void syncPresenceFromChangeType(DbObjectComparisonResult result,
                                                   DbObjectDiff.ChangeType changeType) {
        if (changeType == null) {
            return;
        }

        if (changeType == DbObjectDiff.ChangeType.ADDED) {
            result.getDb1().setPresent(true);
            result.getDb2().setPresent(false);
            return;
        }

        if (changeType == DbObjectDiff.ChangeType.REMOVED) {
            result.getDb1().setPresent(false);
            result.getDb2().setPresent(true);
            return;
        }

        if (changeType == DbObjectDiff.ChangeType.MODIFIED) {
            result.getDb1().setPresent(true);
            result.getDb2().setPresent(true);
        }
    }

    private static void applySourceLineCount(DatabaseSideSummary side, int sourceLineCount) {
        if (side == null || sourceLineCount <= 0) {
            return;
        }
        if (side.getSourceLineCount() == null) {
            side.setSourceLineCount(Integer.valueOf(sourceLineCount));
        }
    }

    private static List<LineChange> toLineChanges(List<DbObjectDiff.SourceDiffLine> sourceDiffs) {
        List<LineChange> results = new ArrayList<LineChange>();
        if (sourceDiffs == null) {
            return results;
        }

        for (DbObjectDiff.SourceDiffLine line : sourceDiffs) {
            if (line == null) {
                continue;
            }

            LineChange result = new LineChange();
            result.setLineNumber(Integer.valueOf(line.getLineNumber()));
            result.setChangeType(line.getChangeType());
            result.setOldLine(line.getOldLine());
            result.setNewLine(line.getNewLine());
            result.setTokenSummary(line.getCharChangeSummary());
            results.add(result);
        }

        return results;
    }

    private static List<ParameterChange> toParameterChanges(List<DbObjectDiff.ParameterDiffLine> parameterDiffs) {
        List<ParameterChange> results = new ArrayList<ParameterChange>();
        if (parameterDiffs == null) {
            return results;
        }

        for (DbObjectDiff.ParameterDiffLine line : parameterDiffs) {
            if (line == null) {
                continue;
            }

            ParameterChange result = new ParameterChange();
            result.setName(line.getParameterName());
            result.setOldMode(line.getOldMode());
            result.setNewMode(line.getNewMode());
            result.setOldDataType(line.getOldDataType());
            result.setNewDataType(line.getNewDataType());
            results.add(result);
        }

        return results;
    }

    private static List<SubProgramResult> toSubPrograms(List<DbObjectDiff.SubProgramDiff> subProgramDiffs) {
        List<SubProgramResult> results = new ArrayList<SubProgramResult>();
        if (subProgramDiffs == null) {
            return results;
        }

        for (DbObjectDiff.SubProgramDiff diff : subProgramDiffs) {
            if (diff == null) {
                continue;
            }

            SubProgramResult result = new SubProgramResult();
            result.setName(diff.getName());
            result.setType(diff.getType());
            result.setStatus(diff.getStatus());
            result.setStartLine(Integer.valueOf(diff.getStartLine()));
            result.setEndLine(Integer.valueOf(diff.getEndLine()));
            result.setChangedLines(diff.getChangedLines());
            results.add(result);
        }

        return results;
    }

    private static String resolveChangeType(DbObjectComparisonResult result) {
        if (result == null) {
            return null;
        }
        if (result.getDb1() != null && result.getDb2() != null) {
            if (result.getDb1().isPresent() && !result.getDb2().isPresent()) {
                return "ADDED";
            }
            if (!result.getDb1().isPresent() && result.getDb2().isPresent()) {
                return "REMOVED";
            }
        }
        if ("DIFFERENT".equalsIgnoreCase(result.getStatus())) {
            return "MODIFIED";
        }
        return null;
    }

    private static boolean isInfoKey(String key) {
        return key == null || "info".equalsIgnoreCase(key.trim());
    }

    private static boolean isInformationalMessage(String row) {
        if (row == null) {
            return true;
        }

        String trimmed = row.trim();
        return trimmed.isEmpty()
                || trimmed.startsWith("No ")
                || trimmed.startsWith("No common tables");
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private interface ObjectResultFactory<T extends DbObjectComparisonResult> {
        T create();
    }
}
