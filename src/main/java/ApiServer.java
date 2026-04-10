import static spark.Spark.*;
import ApiServer.CompareService;
import ApiServer.CompareRequest;
import com.google.gson.Gson;
import services.DbConnectionFactory;

public class ApiServer {

    public static void main(String[] args) {

        port(8080);

        Gson gson = new Gson();


        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", " http://localhost:4200");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equalsIgnoreCase(req.requestMethod())) {
                res.status(200);
                halt();
            }
        });

        // test
        get("/api/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"OK\"}";
        });

        
        spark.Route handler = (req, res) -> {

            CompareRequest request = gson.fromJson(req.body(), CompareRequest.class);

            DbConnectionFactory.DbConfig db1 = new DbConnectionFactory.DbConfig(
                    request.getDb1().getDatabaseName(),
                    request.getDb1().getHost(),
                    String.valueOf(request.getDb1().getPort()),
                    request.getDb1().getUser(),
                    request.getDb1().getPassword(),
                    "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            );

            DbConnectionFactory.DbConfig db2 = new DbConnectionFactory.DbConfig(
                    request.getDb2().getDatabaseName(),
                    request.getDb2().getHost(),
                    String.valueOf(request.getDb2().getPort()),
                    request.getDb2().getUser(),
                    request.getDb2().getPassword(),
                    "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            );

            CompareService service = new CompareService();

            Object result = service.runSelectedComparisons(db1, db2, request);

            res.type("application/json");
            return gson.toJson(result);
        };

        // ✅ ROUTES
        post("/api/compare", handler);
        post("/api/compare/columns", handler);
        post("/api/compare/tables", handler);
        post("/api/compare/data", handler);
        post("/api/compare/types", handler);
        post("/api/compare/functions", handler);
        post("/api/compare/procedures", handler);
        post("/api/compare/triggers", handler);
    }
}