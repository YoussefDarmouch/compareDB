package services;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DbConnectionFactory {
    private static final Properties CONFIG = new Properties();

    public enum DbEngine {
        MYSQL,
        ORACLE;

        public static DbEngine from(String value) {
            if (value == null || value.trim().isEmpty()) {
                return MYSQL;
            }
            String normalized = value.trim().toUpperCase();
            if ("PLSQL".equals(normalized)) {
                return ORACLE;
            }
            try {
                return DbEngine.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                return MYSQL;
            }
        }
    }

    public static class DbConfig {
        private final DbEngine engine;
        private final String databaseName;
        private final String host;
        private final String port;
        private final String user;
        private final String password;
        private final String params;

        public DbConfig(DbEngine engine, String databaseName, String host, String port, String user, String password, String params) {
            this.engine = engine == null ? DbEngine.MYSQL : engine;
            this.databaseName = databaseName;
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
            this.params = params;
        }

        public DbConfig(String databaseName, String host, String port, String user, String password, String params) {
            this(DbEngine.MYSQL, databaseName, host, port, user, password, params);
        }

        public DbEngine getEngine() {
            return engine;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public String getHost() {
            return host;
        }

        public String getPort() {
            return port;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public String getParams() {
            return params;
        }
    }

    static {
        try (InputStream inputStream = DbConnectionFactory.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (inputStream != null) {
                CONFIG.load(inputStream);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load db.properties: " + e.getMessage());
        }
    }

    private DbConnectionFactory() {
    }

    private static String getConfig(String key, String defaultValue) {
        String envValue = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }
        String value = CONFIG.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public static Connection getConnection(String databaseName) throws SQLException {
        return getConnection(getDefaultConfig(databaseName));
    }

    public static Connection getConnection(DbConfig dbConfig) throws SQLException {
        String jdbcUrl;

        if (dbConfig.getEngine() == DbEngine.ORACLE) {
            jdbcUrl = "jdbc:oracle:thin:@//"
                    + dbConfig.getHost()
                    + ":"
                    + dbConfig.getPort()
                    + "/"
                    + dbConfig.getDatabaseName();
            if (dbConfig.getParams() != null && !dbConfig.getParams().trim().isEmpty()) {
                jdbcUrl += "?" + dbConfig.getParams();
            }
        } else {
            jdbcUrl = "jdbc:mysql://"
                    + dbConfig.getHost()
                    + ":"
                    + dbConfig.getPort()
                    + "/"
                    + dbConfig.getDatabaseName()
                    + "?"
                    + dbConfig.getParams();
        }

        return DriverManager.getConnection(jdbcUrl, dbConfig.getUser(), dbConfig.getPassword());
    }

    public static DbConfig getDefaultConfig(String databaseName) {
        DbEngine engine = DbEngine.from(getConfig("db.engine", "MYSQL"));
        return getDefaultConfig(databaseName, engine);
    }

    public static DbConfig getDefaultConfig(String databaseName, DbEngine engine) {
        String host = getConfig("db.host", "127.0.0.1");
        String port = engine == DbEngine.ORACLE
                ? getConfig("db.oracle.port", "1521")
                : getConfig("db.port", "3306");
        String user = getConfig("db.user", "root");
        String password = getConfig("db.password", "");
        String params = engine == DbEngine.ORACLE
                ? getConfig("db.oracle.params", "")
                : getConfig("db.params", "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");

        return new DbConfig(engine, databaseName, host, port, user, password, params);
    }
}