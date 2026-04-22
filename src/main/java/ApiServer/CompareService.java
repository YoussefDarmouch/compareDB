package ApiServer;

import ApiServer.dto.ColumnDifferenceResult;
import ApiServer.dto.DataDifferenceResult;
import ApiServer.dto.FunctionResult;
import ApiServer.dto.PackageResult;
import ApiServer.dto.ProcedureResult;
import ApiServer.dto.TableDifferenceResult;
import ApiServer.dto.TriggerResult;
import ApiServer.dto.TypeDifferenceResult;
import services.DbConnectionFactory;
import services.DbLabelUtils;
import services.DbObjectDiff;
import services.DbObjectDiffFormatter;
import Package.CompareTablesDb;
import Package.CompareDataService;
import Package.CompareFuncDB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CompareService {



    public List<TableDifferenceResult> compareTablesV2(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return LegacyComparisonParser.parseTableDifferences(
                CompareTablesDb.compareTablesApi(db1Config, db2Config));
    }

    public List<ColumnDifferenceResult> compareColumnsV2(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> tables) {

        return LegacyComparisonParser.parseColumnDifferences(
                CompareDataService.compareColumnsApi(db1Config, db2Config, tables));
    }

    public List<DataDifferenceResult> compareDataV2(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> tables) {

        return LegacyComparisonParser.parseDataDifferences(
                CompareDataService.compareDataApi(db1Config, db2Config, tables),
                DbLabelUtils.displayName(db1Config),
                DbLabelUtils.displayName(db2Config));
    }

    public List<TypeDifferenceResult> compareTypesV2(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> tables) {

        return LegacyComparisonParser.parseTypeDifferences(
                CompareDataService.compareTypesApi(db1Config, db2Config, tables),
                DbLabelUtils.displayName(db1Config),
                DbLabelUtils.displayName(db2Config));
    }

    public List<FunctionResult> compareFunctionsV2(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        List<DbObjectDiff> diffs = CompareFuncDB.compareFunctionsDiffs(db1Config, db2Config);
        return LegacyComparisonParser.parseFunctionResults(
                buildLegacySummaryRows(diffs, db1Config, db2Config),
                diffs);
    }

    public List<ProcedureResult> compareProceduresV2(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        List<DbObjectDiff> diffs = CompareFuncDB.compareProceduresDiffs(db1Config, db2Config);
        return LegacyComparisonParser.parseProcedureResults(
                buildLegacySummaryRows(diffs, db1Config, db2Config),
                diffs);
    }

    public List<TriggerResult> compareTriggersV2(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        List<DbObjectDiff> diffs = CompareFuncDB.compareTriggersDiffs(db1Config, db2Config);
        return LegacyComparisonParser.parseTriggerResults(
                buildLegacySummaryRows(diffs, db1Config, db2Config),
                diffs);
    }

    public List<PackageResult> comparePackagesV2(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        List<DbObjectDiff> diffs = CompareFuncDB.comparePackagesDiffs(db1Config, db2Config);
        return LegacyComparisonParser.parsePackageResults(
                buildLegacySummaryRows(diffs, db1Config, db2Config),
                diffs);
    }

    private List<String> buildLegacySummaryRows(
            List<DbObjectDiff> diffs,
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        List<String> rows = new ArrayList<String>();
        if (diffs == null || diffs.isEmpty()) {
            return rows;
        }

        String db1Label = DbLabelUtils.displayName(db1Config);
        String db2Label = DbLabelUtils.displayName(db2Config);

        for (DbObjectDiff diff : diffs) {
            rows.add(DbObjectDiffFormatter.formatSummaryRow(diff, db1Label, db2Label));
        }

        return rows;
    }
}
