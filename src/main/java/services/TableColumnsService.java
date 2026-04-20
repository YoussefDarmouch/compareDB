package services;

import java.sql.*;
import java.util.*;

public class TableColumnsService {

    private static final Set<String> ORACLE_CHAR_TYPES = new HashSet<String>(
        Arrays.asList("CHAR", "NCHAR", "VARCHAR2", "NVARCHAR2", "VARCHAR", "RAW"));

    private static final Set<String> ORACLE_PRECISION_TYPES = new HashSet<String>(
        Arrays.asList("NUMBER", "FLOAT", "DECIMAL", "NUMERIC"));

    // =========================
    // GET DATA SAFE
    // =========================
    public static Map<String, Map<String, String>> GetData(String databaseName, String tableName) {
        return GetData(DbConnectionFactory.getDefaultConfig(databaseName), tableName);
    }

    public static Map<String, Map<String, String>> GetData(DbConnectionFactory.DbConfig dbConfig, String tableName) {

        Map<String, Map<String, String>> data = new LinkedHashMap<>();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig)) {

            // ⚠ safer than raw concatenation (basic validation)
            if (!tableName.matches("[A-Za-z0-9_]+")) {
                throw new RuntimeException("Invalid table name");
            }

            String sql = "SELECT * FROM " + tableName;

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(sql)) {

                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {

                    Map<String, String> row = new LinkedHashMap<>();

                    String rowId = null;

                    for (int i = 1; i <= columnCount; i++) {

                        String colName = meta.getColumnName(i);
                        String value = rs.getString(i);

                        if (i == 1) {
                            rowId = value;
                        }

                        row.put(colName, value);
                    }

                    if (rowId == null) {
                        rowId = UUID.randomUUID().toString();
                    }

                    data.put(rowId, row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return data;
    }

    // =========================
    // GET COLUMNS (MYSQL + ORACLE FIXED)
    // =========================
    public static List<String> GetColumns(String databaseName, String tableName) {
        return GetColumns(DbConnectionFactory.getDefaultConfig(databaseName), tableName);
    }

    public static List<String> GetColumns(DbConnectionFactory.DbConfig dbConfig, String tableName) {

        List<String> columns = new ArrayList<>();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig)) {

            if (DbConnectionFactory.DbEngine.MYSQL.equals(dbConfig.getEngine())) {

                String query = "DESCRIBE " + tableName;

                try (Statement statement = connection.createStatement();
                     ResultSet rs = statement.executeQuery(query)) {

                    while (rs.next()) {
                        columns.add(rs.getString("Field") + " (" + rs.getString("Type") + ")");
                    }
                }

            } else {

                String query = "SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION, DATA_SCALE, " +
                        "CHAR_LENGTH, CHAR_USED " +
                        "FROM ALL_TAB_COLUMNS " +
                        "WHERE TABLE_NAME = ? AND OWNER = ? " +
                        "ORDER BY COLUMN_ID";

                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, tableName.toUpperCase());
                    statement.setString(2, dbConfig.getUser().toUpperCase());

                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            String oracleType = formatOracleColumnType(rs);
                            columns.add(rs.getString("COLUMN_NAME") + " (" + oracleType + ")");
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columns;
    }

    // =========================
    // SHOW DATA
    // =========================
    public static void ShowData(String databaseName, String tableName) {

        Map<String, Map<String, String>> data = GetData(databaseName, tableName);

        for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {

            System.out.println("ID: " + entry.getKey());

            for (Map.Entry<String, String> col : entry.getValue().entrySet()) {
                System.out.println("   " + col.getKey() + " : " + col.getValue());
            }
        }
    }

    private static String formatOracleColumnType(ResultSet rs) throws SQLException {
        String dataType = rs.getString("DATA_TYPE");
        if (dataType == null) {
            return "UNKNOWN";
        }

        String normalizedType = dataType.trim().toUpperCase(Locale.ROOT);

        if (ORACLE_CHAR_TYPES.contains(normalizedType)) {
            Integer length = getNullableInt(rs, "CHAR_LENGTH");
            if (length == null || length.intValue() == 0) {
                length = getNullableInt(rs, "DATA_LENGTH");
            }
            if (length != null && length.intValue() > 0) {
                String charUsed = rs.getString("CHAR_USED");
                if ("C".equalsIgnoreCase(charUsed)) {
                    return normalizedType + "(" + length + " CHAR)";
                }
                return normalizedType + "(" + length + ")";
            }
            return normalizedType;
        }

        if (ORACLE_PRECISION_TYPES.contains(normalizedType)) {
            Integer precision = getNullableInt(rs, "DATA_PRECISION");
            Integer scale = getNullableInt(rs, "DATA_SCALE");

            if (precision != null) {
                if (scale != null && scale.intValue() > 0) {
                    return normalizedType + "(" + precision + "," + scale + ")";
                }
                return normalizedType + "(" + precision + ")";
            }

            if (scale != null && scale.intValue() > 0) {
                return normalizedType + "(" + scale + ")";
            }
        }

        return normalizedType;
    }

    private static Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : Integer.valueOf(value);
    }
}