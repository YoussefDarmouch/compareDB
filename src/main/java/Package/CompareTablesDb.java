package Package;

import  services.DbConnectionFactory;
import services.DbLabelUtils;
import services.ShowTablesService;
import services.TablePrinter;

import java.util.ArrayList;
import java.util.List;

public class CompareTablesDb {


    private static List<String> compareTablesLogic(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        String db1 = DbLabelUtils.displayName(db1Config);
        String db2 = DbLabelUtils.displayName(db2Config);

        List<String> tablesDb1 = ShowTablesService.GetNameTable(db1Config);
        List<String> tablesDb2 = ShowTablesService.GetNameTable(db2Config);

        List<String> result = new ArrayList<>();

        for (String table : tablesDb1) {
            if (!tablesDb2.contains(table)) {
                result.add("Tables missing in " + db2 + ": " + table);
            }
        }

        for (String table : tablesDb2) {
            if (!tablesDb1.contains(table)) {
                result.add("Tables missing in " + db1 + ": " + table);
            }
        }

        if (result.isEmpty()) {
            result.add("No differences found between DB1 and DB2 (Tables)");
        }

        return result;
    }

    // ===== API (RETURN) =====
    public static List<String> compareTablesApi(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        return compareTablesLogic(db1Config, db2Config);
    }

     //===== CONSOLE (PRINT) =====
    public static void compareTables(
            DbConnectionFactory.DbConfig db1Config,
            DbConnectionFactory.DbConfig db2Config) {

        List<String> result = compareTablesLogic(db1Config, db2Config);
        TablePrinter.compareTables(db1Config,db2Config);

    }



}