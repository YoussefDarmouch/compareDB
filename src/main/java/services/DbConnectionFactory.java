package services;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DbConnectionFactory {
    private static final Properties CONFIG = new Properties();

    public static class DbConfig {
        private final String databaseName;
        private final String host;
        private final String port;
        private final String user;
        private final String password;
        private final String params;

        public DbConfig(String databaseName, String host, String port, String user, String password, String params) {
            this.databaseName = databaseName;
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
            this.params = params;
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
        String jdbcUrl = "jdbc:mysql://"
                + dbConfig.getHost()
                + ":"
                + dbConfig.getPort()
                + "/"
                + dbConfig.getDatabaseName()
                + "?"
                + dbConfig.getParams();

        return DriverManager.getConnection(jdbcUrl, dbConfig.getUser(), dbConfig.getPassword());
    }

    public static DbConfig getDefaultConfig(String databaseName) {
        String host = getConfig("db.host", "127.0.0.1");
        String port = getConfig("db.port", "3306");
        String user = getConfig("db.user", "root");
        String password = getConfig("db.password", "");
        String params = getConfig("db.params", "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");

        return new DbConfig(databaseName, host, port, user, password, params);
    }
}