package interfaces;

import java.util.List;

public interface ShowColumTables {
    List<String> GetData(String databaseName, String tableName);

    void ShowData(String databaseName, String tableName);
}