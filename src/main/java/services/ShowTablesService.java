package services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShowTablesService {

    // entry by databaseName
    public static List<String> GetNameTable(String databaseName) {
        return GetNameTable(DbConnectionFactory.getDefaultConfig(databaseName));
    }

    // main safe method
    public static List<String> GetNameTable(DbConnectionFactory.DbConfig dbConfig) {

        List<String> listTable = new ArrayList<>();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig)) {

            String query;

            // MYSQL
            if (DbConnectionFactory.DbEngine.MYSQL.equals(dbConfig.getEngine())) {
                query = "SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'";
            }

            // ORACLE
            else {
                query = "SELECT table_name FROM all_tables WHERE owner = USER";
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    listTable.add(rs.getString(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return listTable;
    }

    // print helper
    public void ShowNameTables(String databaseName) {
        List<String> listTable = GetNameTable(databaseName);

        for (String tableName : listTable) {
            System.out.println("📌 Table: " + tableName);
        }
    }
}