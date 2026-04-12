import static spark.Spark.*;

import ApiServer.CompareService;
import ApiServer.CompareRequest;
import com.google.gson.Gson;
import services.DbConnectionFactory;

import java.util.Set;

public class ApiServer {

    public static void main(String[] args) {

        port(8080);

        Gson gson = new Gson();

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "http://localhost:4200");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equalsIgnoreCase(req.requestMethod())) {
                res.status(200);
                halt();
            }
        });

        // health check
        get("/api/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"OK\"}";
        });

        final CompareService service = new CompareService();

        // helper DB builder (JAVA 8 safe)
        java.util.function.Function<CompareRequest.DbConfig, DbConnectionFactory.DbConfig> buildDb =
                new java.util.function.Function<CompareRequest.DbConfig, DbConnectionFactory.DbConfig>() {
                    @Override
                    public DbConnectionFactory.DbConfig apply(CompareRequest.DbConfig db) {
                        return new DbConnectionFactory.DbConfig(
                                db.getDatabaseName(),
                                db.getHost(),
                                String.valueOf(db.getPort()),
                                db.getUser(),
                                db.getPassword(),
                                "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                        );
                    }
                };

        // ======================
        // TABLES
        // ======================
        post("/api/compare/tables", (req, res) -> {

            CompareRequest request = gson.fromJson(req.body(), CompareRequest.class);

            DbConnectionFactory.DbConfig db1 = buildDb.apply(request.getDb1());
            DbConnectionFactory.DbConfig db2 = buildDb.apply(request.getDb2());

            Object result = service.compareTables(db1, db2);

            res.type("application/json");
            return gson.toJson(result);
        });

        // ======================
        // COLUMNS
        // ======================
        post("/api/compare/columns", (req, res) -> {

            CompareRequest request = gson.fromJson(req.body(), CompareRequest.class);

            DbConnectionFactory.DbConfig db1 = buildDb.apply(request.getDb1());
            DbConnectionFactory.DbConfig db2 = buildDb.apply(request.getDb2());

            Set<String> tables = request.getTablesToCompare();

            Object result = service.compareColumns(db1, db2, tables);

            res.type("application/json");
            return gson.toJson(result);
        });

        // ======================
        // DATA
        // ======================
        post("/api/compare/data", (req, res) -> {

            CompareRequest request = gson.fromJson(req.body(), CompareRequest.class);

            DbConnectionFactory.DbConfig db1 = buildDb.apply(request.getDb1());
            DbConnectionFactory.DbConfig db2 = buildDb.apply(request.getDb2());

            Object result = service.compareData(db1, db2, request.getTablesToCompare());

            res.type("application/json");
            return gson.toJson(result);
        });

        // ======================
        // TYPES
        // ======================
        post("/api/compare/types", (req, res) -> {

            CompareRequest request = gson.fromJson(req.body(), CompareRequest.class);

            DbConnectionFactory.DbConfig db1 = buildDb.apply(request.getDb1());
            DbConnectionFactory.DbConfig db2 = buildDb.apply(request.getDb2());

            Object result = service.compareTypes(db1, db2, request.getTablesToCompare());

            res.type("application/json");
            return gson.toJson(result);
        });

        // ======================
        // FUNCTIONS
        // ======================
        post("/api/compare/functions", (req, res) -> {

            CompareRequest request = gson.fromJson(req.body(), CompareRequest.class);

            DbConnectionFactory.DbConfig db1 = buildDb.apply(request.getDb1());
            DbConnectionFactory.DbConfig db2 = buildDb.apply(request.getDb2());

            Object result = service.compareFunctions(db1, db2);

            res.type("application/json");
            return gson.toJson(result);
        });

        // ======================
        // PROCEDURES
        // ======================
        post("/api/compare/procedures", (req, res) -> {

            CompareRequest request = gson.fromJson(req.body(), CompareRequest.class);

            DbConnectionFactory.DbConfig db1 = buildDb.apply(request.getDb1());
            DbConnectionFactory.DbConfig db2 = buildDb.apply(request.getDb2());

            Object result = service.compareProcedures(db1, db2);

            res.type("application/json");
            return gson.toJson(result);
        });

        // ======================
        // TRIGGERS
        // ======================
        post("/api/compare/triggers", (req, res) -> {

            CompareRequest request = gson.fromJson(req.body(), CompareRequest.class);

            DbConnectionFactory.DbConfig db1 = buildDb.apply(request.getDb1());
            DbConnectionFactory.DbConfig db2 = buildDb.apply(request.getDb2());

            Object result = service.compareTriggers(db1, db2);

            res.type("application/json");
            return gson.toJson(result);
        });
    }
}