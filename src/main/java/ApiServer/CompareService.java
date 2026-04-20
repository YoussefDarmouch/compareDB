package ApiServer;

import services.DbConnectionFactory;
import Package.CompareTablesDb;
import Package.CompareDataService;
import Package.CompareFuncDB;

import java.util.Set;

public class CompareService {

    public Object compareTables(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return CompareTablesDb.compareTablesApi(db1Config, db2Config);
    }

    public Object compareColumns(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> tables) {

        return CompareDataService.compareColumnsApi(db1Config, db2Config, tables);
    }

    public Object compareData(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> tables) {

        return CompareDataService.compareDataApi(db1Config, db2Config, tables);
    }

    public Object compareTypes(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,
            Set<String> tables) {

        return CompareDataService.compareTypesApi(db1Config, db2Config, tables);
    }

    public Object compareFunctions(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return CompareFuncDB.compareFunctionsApi(db1Config, db2Config);
    }

    public Object compareProcedures(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return CompareFuncDB.compareProceduresApi(db1Config, db2Config);
    }

    public Object compareTriggers(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return CompareFuncDB.compareTriggersApi(db1Config, db2Config);
    }

    public Object comparePackages(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return CompareFuncDB.comparePackagesApi(db1Config, db2Config);
    }
}