package interfaces;

import java.util.List;

public interface ShowNameTables {
    List<String> GetNameTable(String databaseName);

    void ShowNameTables(String databaseName);
}