package ApiServer;

import java.util.Set;

public class CompareRequest {
    

    private Set<String> tablesToCompare;

    private DbConfig db1;
    private DbConfig db2;



    public Set<String> getTablesToCompare() { return tablesToCompare; }

    public DbConfig getDb1() { return db1; }
    public DbConfig getDb2() { return db2; }

    public static class DbConfig {
        private String engine;
        private String databaseName;
        private String host;
        private int port;
        private String user;
        private String password;

        public String getEngine() { return engine; }
        public String getDatabaseName() { return databaseName; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUser() { return user; }
        public String getPassword() { return password; }
    }
}