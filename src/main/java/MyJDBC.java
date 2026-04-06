// import java.sql.*;
// import java.util.ArrayList;
// import java.util.List;

// import services.DbConnectionFactory;

// public class MyJDBC {
//     public static void main(String[] args) {
//         try {
//         Connection connection = DbConnectionFactory.getConnection("db1");
//             Statement statementTable = connection.createStatement();
//             Statement statementColumns = connection.createStatement();

//         Connection connection2 = DbConnectionFactory.getConnection("db2");

//             Statement statementTable2 = connection2.createStatement();
//             Statement statementColumns2 = connection2.createStatement();

//             ResultSet tables = statementTable.executeQuery("SHOW TABLES FROM db1");
//             ResultSet tables2 = statementTable2.executeQuery("SHOW TABLES FROM db2");

//             while (tables.next()) {
//                 String tableName = tables.getString(1);
//                 System.out.println("\n📌 Table: " + tableName);
//                 while (tables2.next()) {

//                     String tableName2 = tables2.getString(1);
//                     System.out.println("\n📌 Table: " + tableName2);

//                     ResultSet columns2 = statementColumns2.executeQuery("DESCRIBE db2." + tableName2);
//                     ResultSet columns = statementColumns.executeQuery("DESCRIBE db1." + tableName);

//                     List<String> db1 = new ArrayList<>();
//                     while (columns.next()) {
//                         db1.add(columns.getString("Field") + " (" + columns.getString("Type") + ")");
//                     }

//                     List<String> db2 = new ArrayList<>();
//                     while (columns2.next()) {
//                         db2.add(columns2.getString("Field") + " (" + columns2.getString("Type") + ")");
//                     }
//                     System.out.println("DB1:");
//                     for (String c : db1) {
//                         System.out.println("   " + c);
//                     }

//                     System.out.println("DB2:");
//                     for (String c : db2) {
//                         System.out.println("   " + c);
//                     }

//                     for (String c1 : db1) {
//                         if (!db2.contains(c1)) {
//                             System.out.println("Missing in table2: " + c1);
//                         }
//                     }
//                 }
//             }

//         } catch (SQLException e) {
//             e.printStackTrace();
//         }
//     }
// }