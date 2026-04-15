import java.sql.*;
import services.DbConnectionFactory;

public class MyJDBC {


        public static void main(String[] args) throws Exception {
            Connection conn = DbConnectionFactory.getConnection(
                    new DbConnectionFactory.DbConfig(
                            DbConnectionFactory.DbEngine.ORACLE,
                            "XEPDB1",
                            "localhost",
                            "1521",
                            "d2",
                            "1234",
                            ""
                    )
            );

            System.out.println("Connected: " + !conn.isClosed());

            conn.close();

    }
}