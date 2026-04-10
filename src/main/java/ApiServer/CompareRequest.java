package ApiServer;

import java.util.Set;

public class CompareRequest {

    private boolean tables;
    private boolean columns;
    private boolean data;
    private boolean types;
    private boolean functions;
    private boolean procedures;
    private boolean triggers;

    private Set<String> tablesToCompare;

    private DbConfig db1;
    private DbConfig db2;

    // getters
    public boolean isTables() { return tables; }
    public boolean isColumns() { return columns; }
    public boolean isData() { return data; }
    public boolean isTypes() { return types; }
    public boolean isFunctions() { return functions; }
    public boolean isProcedures() { return procedures; }
    public boolean isTriggers() { return triggers; }

    public Set<String> getTablesToCompare() { return tablesToCompare; }

    public DbConfig getDb1() { return db1; }
    public DbConfig getDb2() { return db2; }

    // nested class DbConfig
    public static class DbConfig {
        private String databaseName;
        private String host;
        private int port;
        private String user;
        private String password;

        public String getDatabaseName() { return databaseName; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUser() { return user; }
        public String getPassword() { return password; }
    }
}