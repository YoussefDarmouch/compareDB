package services;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableColumnsService {
    static public Map<Integer, Map<String, String>> GetData(String databaseName, String tableName) {
        return GetData(DbConnectionFactory.getDefaultConfig(databaseName), tableName);
    }

    static public Map<Integer, Map<String, String>> GetData(DbConnectionFactory.DbConfig dbConfig, String tableName) {
        Map<Integer, Map<String, String>> data = new HashMap<>();
        try (Connection connection = DbConnectionFactory.getConnection(dbConfig);
             Statement statement = connection.createStatement();
             ResultSet Data = statement.executeQuery("SELECT * FROM  " + tableName)) {
            ResultSetMetaData meta = Data.getMetaData();
            int columnCount = meta.getColumnCount();
            while (Data.next()) {
                int id = Data.getInt(meta.getColumnName(1));
                Map<String, String> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String colName = meta.getColumnName(i);
                    String value = Data.getString(i);
                    row.put(colName, value);
                }
                data.put(id, row);

            }

        } catch (SQLException e) {
            e.printStackTrace();

        }
        return data;
    }

    public static List<String> GetColumns(String databaseName, String tableName) {
        return GetColumns(DbConnectionFactory.getDefaultConfig(databaseName), tableName);
    }

    public static List<String> GetColumns(DbConnectionFactory.DbConfig dbConfig, String tableName) {
        List<String> columns = new ArrayList<>();
        String databaseName = dbConfig.getDatabaseName();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig);
             Statement statementColumns = connection.createStatement();
             ResultSet column = statementColumns.executeQuery(
                     "DESCRIBE " + databaseName + "." + tableName
             )) {
            while (column.next()) {
                columns.add(column.getString("Field") + " (" + column.getString("Type") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columns;
    }

    public static void ShowData(String databaseName, String tableName) {
        Map<Integer, Map<String, String>> Data = GetData(databaseName, tableName);
        for (Map.Entry<Integer, Map<String, String>> entry : Data.entrySet()) {
            Integer id = entry.getKey();
            Map<String, String> row = entry.getValue();
            System.out.println("ID: " + id);
            for (Map.Entry<String, String> col : row.entrySet()) {
                System.out.println("   " + col.getKey() + " : " + col.getValue());
            }

        }

    }
}