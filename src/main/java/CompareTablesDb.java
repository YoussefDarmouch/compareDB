import java.util.List;

import services.DbConnectionFactory;
import services.ShowTablesService;

public class CompareTablesDb {

    public static void compareTables(String db1, String db2) {
        compareTables(
                DbConnectionFactory.getDefaultConfig(db1),
                DbConnectionFactory.getDefaultConfig(db2)
        );
    }

    public static void compareTables(DbConnectionFactory.DbConfig db1Config, DbConnectionFactory.DbConfig db2Config) {
        String db1 = db1Config.getDatabaseName();
        String db2 = db2Config.getDatabaseName();

        List<String> tablesDb1 = ShowTablesService.GetNameTable(db1Config);
        List<String> tablesDb2 = ShowTablesService.GetNameTable(db2Config);
        boolean hasDifference = false;

        for (String table : tablesDb1) {
            if (!tablesDb2.contains(table)) {
                System.out.println(" Tables missing in " + db2 + ": " + table);
                hasDifference = true;
            }
        }

        for (String table : tablesDb2) {
            if (!tablesDb1.contains(table)) {
                System.out.println(" Tables missing in " + db1 + ": " + table);
                hasDifference = true;
            }
        }

        if (!hasDifference) {
            System.out.println("No differences found between DB1 and DB2 (Tables)");
        }
    }
}