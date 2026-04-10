package ApiServer;

import  services.DbConnectionFactory;
import  Package.CompareTablesDb;
import  Package.CompareDataService;
import  Package.CompareFuncDB;
import java.util.Map;
import java.util.HashMap;

public class CompareService {

    public Map<String, Object> runSelectedComparisons(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config,

            CompareRequest request) {

        Map<String, Object> result = new HashMap<>();

        if (request.isTables()) {
            result.put("tables", CompareTablesDb.compareTablesApi(db1Config, db2Config));
        }

        if (request.isColumns()) {
            result.put("columns", CompareDataService.compareColumnsApi(db1Config, db2Config,request.getTablesToCompare() ));
        }

        if (request.isData()) {
            result.put("data", CompareDataService.compareDataApi(db1Config, db2Config, request.getTablesToCompare()));
        }

        if (request.isTypes()) {
            result.put("types", CompareDataService.compareTypesApi(db1Config, db2Config, request.getTablesToCompare()));
        }

        if (request.isFunctions()) {
            result.put("functions", CompareFuncDB.compareFunctionsApi(db1Config, db2Config));
        }

        if (request.isProcedures()) {
            result.put("procedures", CompareFuncDB.compareProceduresApi(db1Config, db2Config));
        }

        if (request.isTriggers()) {
            result.put("triggers", CompareFuncDB.compareTriggersApi(db1Config, db2Config));
        }

        return result;
    }
}