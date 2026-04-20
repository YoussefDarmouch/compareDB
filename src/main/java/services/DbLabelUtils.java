package services;

public final class DbLabelUtils {

    private DbLabelUtils() {
    }

    public static String displayName(DbConnectionFactory.DbConfig config) {
        if (config == null) {
            return "<unknown>";
        }

        if (config.getEngine() == DbConnectionFactory.DbEngine.ORACLE) {
            return safe(config.getDatabaseName()) + "/" + safe(config.getUser());
        }

        return safe(config.getDatabaseName());
    }

    private static String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "<none>";
        }
        return value.trim();
    }
}