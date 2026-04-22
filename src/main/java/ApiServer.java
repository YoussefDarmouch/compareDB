import ApiServer.CompareRequest;
import ApiServer.CompareService;
import com.google.gson.Gson;
import services.DbConnectionFactory;
import spark.Route;

import java.util.function.Function;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;

public class ApiServer {

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String MYSQL_CONNECTION_PARAMS =
            "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static void main(String[] args) {

        port(8080);

        final Gson gson = new Gson();
        final CompareService service = new CompareService();

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "http://localhost:4201");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equalsIgnoreCase(req.requestMethod())) {
                res.status(200);
                halt();
            }
        });

        get("/api/health", (req, res) -> {
            res.type(JSON_CONTENT_TYPE);
            return "{\"status\":\"OK\"}";
        });

        final Function<CompareRequest.DbConfig, DbConnectionFactory.DbConfig> buildDb =
                new Function<CompareRequest.DbConfig, DbConnectionFactory.DbConfig>() {
                    @Override
                    public DbConnectionFactory.DbConfig apply(CompareRequest.DbConfig db) {
                        DbConnectionFactory.DbEngine engine = DbConnectionFactory.DbEngine.from(db.getEngine());
                        return new DbConnectionFactory.DbConfig(
                                engine,
                                db.getDatabaseName(),
                                db.getHost(),
                                String.valueOf(db.getPort()),
                                db.getUser(),
                                db.getPassword(),
                                engine == DbConnectionFactory.DbEngine.ORACLE ? "" : MYSQL_CONNECTION_PARAMS
                        );
                    }
                };

        registerCompareRoute("/api/compare/tables", gson, service, buildDb,
                new CompareRouteHandler() {
                    @Override
                    public Object handle(CompareService compareService,
                                         DbConnectionFactory.DbConfig db1,
                                         DbConnectionFactory.DbConfig db2,
                                         CompareRequest request) {
                        return compareService.compareTablesV2(db1, db2);
                    }
                });

        registerCompareRoute("/api/compare/columns", gson, service, buildDb,
                new CompareRouteHandler() {
                    @Override
                    public Object handle(CompareService compareService,
                                         DbConnectionFactory.DbConfig db1,
                                         DbConnectionFactory.DbConfig db2,
                                         CompareRequest request) {
                        return compareService.compareColumnsV2(db1, db2, request.getTablesToCompare());
                    }
                });

        registerCompareRoute("/api/compare/data", gson, service, buildDb,
                new CompareRouteHandler() {
                    @Override
                    public Object handle(CompareService compareService,
                                         DbConnectionFactory.DbConfig db1,
                                         DbConnectionFactory.DbConfig db2,
                                         CompareRequest request) {
                        return compareService.compareDataV2(db1, db2, request.getTablesToCompare());
                    }
                });

        registerCompareRoute("/api/compare/types", gson, service, buildDb,
                new CompareRouteHandler() {
                    @Override
                    public Object handle(CompareService compareService,
                                         DbConnectionFactory.DbConfig db1,
                                         DbConnectionFactory.DbConfig db2,
                                         CompareRequest request) {
                        return compareService.compareTypesV2(db1, db2, request.getTablesToCompare());
                    }
                });

        registerCompareRoute("/api/compare/functions", gson, service, buildDb,
                new CompareRouteHandler() {
                    @Override
                    public Object handle(CompareService compareService,
                                         DbConnectionFactory.DbConfig db1,
                                         DbConnectionFactory.DbConfig db2,
                                         CompareRequest request) {
                        return compareService.compareFunctionsV2(db1, db2);
                    }
                });

        registerCompareRoute("/api/compare/procedures", gson, service, buildDb,
                new CompareRouteHandler() {
                    @Override
                    public Object handle(CompareService compareService,
                                         DbConnectionFactory.DbConfig db1,
                                         DbConnectionFactory.DbConfig db2,
                                         CompareRequest request) {
                        return compareService.compareProceduresV2(db1, db2);
                    }
                });

        registerCompareRoute("/api/compare/triggers", gson, service, buildDb,
                new CompareRouteHandler() {
                    @Override
                    public Object handle(CompareService compareService,
                                         DbConnectionFactory.DbConfig db1,
                                         DbConnectionFactory.DbConfig db2,
                                         CompareRequest request) {
                        return compareService.compareTriggersV2(db1, db2);
                    }
                });

        registerCompareRoute("/api/compare/packages", gson, service, buildDb,
                new CompareRouteHandler() {
                    @Override
                    public Object handle(CompareService compareService,
                                         DbConnectionFactory.DbConfig db1,
                                         DbConnectionFactory.DbConfig db2,
                                         CompareRequest request) {
                        return compareService.comparePackagesV2(db1, db2);
                    }
                });
    }

    private static void registerCompareRoute(
            String path,
            final Gson gson,
            final CompareService service,
            final Function<CompareRequest.DbConfig, DbConnectionFactory.DbConfig> buildDb,
            final CompareRouteHandler handler) {

        post(path, createRoute(gson, service, buildDb, handler));
    }

    private static Route createRoute(
            final Gson gson,
            final CompareService service,
            final Function<CompareRequest.DbConfig, DbConnectionFactory.DbConfig> buildDb,
            final CompareRouteHandler handler) {

        return (req, res) -> {
            CompareRequest request = gson.fromJson(req.body(), CompareRequest.class);

            DbConnectionFactory.DbConfig db1 = buildDb.apply(request.getDb1());
            DbConnectionFactory.DbConfig db2 = buildDb.apply(request.getDb2());

            Object result = handler.handle(service, db1, db2, request);

            res.type(JSON_CONTENT_TYPE);
            return gson.toJson(result);
        };
    }

    private interface CompareRouteHandler {
        Object handle(CompareService compareService,
                      DbConnectionFactory.DbConfig db1,
                      DbConnectionFactory.DbConfig db2,
                      CompareRequest request);
    }
}
