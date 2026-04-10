package services;

import java.sql.*;
import java.util.*;

public class TableColumnsService {

    // 🔹 overload باستعمال databaseName
    public static Map<String, Map<String, String>> GetData(String databaseName, String tableName) {
        return GetData(DbConnectionFactory.getDefaultConfig(databaseName), tableName);
    }

    // 🔹 method الرئيسية
    public static Map<String, Map<String, String>> GetData(DbConnectionFactory.DbConfig dbConfig, String tableName) {
        Map<String, Map<String, String>> data = new HashMap<>();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName)) {

            ResultSetMetaData meta = resultSet.getMetaData();
            int columnCount = meta.getColumnCount();

            while (resultSet.next()) {


                String id = resultSet.getString(1);

                Map<String, String> row = new HashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String colName = meta.getColumnName(i);
                    String value = resultSet.getString(i); // safe لجميع الأنواع
                    row.put(colName, value);
                }

                data.put(id, row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return data;
    }

    // 🔹 جلب الأعمدة
    public static List<String> GetColumns(String databaseName, String tableName) {
        return GetColumns(DbConnectionFactory.getDefaultConfig(databaseName), tableName);
    }

    public static List<String> GetColumns(DbConnectionFactory.DbConfig dbConfig, String tableName) {
        List<String> columns = new ArrayList<>();
        String databaseName = dbConfig.getDatabaseName();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("DESCRIBE " + databaseName + "." + tableName)) {

            while (rs.next()) {
                columns.add(rs.getString("Field") + " (" + rs.getString("Type") + ")");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columns;
    }

    // 🔹 عرض البيانات (debug)
    public static void ShowData(String databaseName, String tableName) {
        Map<String, Map<String, String>> data = GetData(databaseName, tableName);

        for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
            String id = entry.getKey();
            Map<String, String> row = entry.getValue();

            System.out.println("ID: " + id);

            for (Map.Entry<String, String> col : row.entrySet()) {
                System.out.println("   " + col.getKey() + " : " + col.getValue());
            }
        }
    }
}