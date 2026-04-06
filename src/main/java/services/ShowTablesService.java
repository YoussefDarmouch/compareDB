package services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShowTablesService {
    static public List<String> GetNameTable(String databaseName) {
        return GetNameTable(DbConnectionFactory.getDefaultConfig(databaseName));
    }

    static public List<String> GetNameTable(DbConnectionFactory.DbConfig dbConfig) {
        List<String> listTable = new ArrayList<>();
        String databaseName = dbConfig.getDatabaseName();

        try (Connection connection = DbConnectionFactory.getConnection(dbConfig);
             Statement statementTable = connection.createStatement();
             ResultSet TableName = statementTable.executeQuery("SHOW TABLES FROM " + databaseName)) {
            while (TableName.next()) {
                String tableName = TableName.getString(1);
                listTable.add(tableName);
            }

        } catch (SQLException e) {
            e.printStackTrace();

        }
        return listTable;
    }

    public void ShowNameTables(String databaseName) {
        List<String> listTable = GetNameTable(databaseName);
        for (String tableName : listTable) {
            System.out.println("   📌 Table: " + tableName);
        }
    }
}