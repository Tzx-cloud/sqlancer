package sqlancer.sqlite3;

import sqlancer.AFLMonitor;
import sqlancer.SQLConnection;

import java.io.IOException;
import java.sql.*;

public class SQLite3AFLConnection extends SQLConnection {
    public static String databaseName;
    private final AFLMonitor aflMonitor;

    public SQLite3AFLConnection(String name) throws IOException {
        super(null);
        databaseName=name;
        aflMonitor = AFLMonitor.getInstance();
        AFLMonitor.getInstance().executeSQLStatement(".open "+databaseName);// 不使用JDBC连接
    }

    @Override
    public Statement createStatement() throws SQLException {
        return super.createStatement();
    }

    @Override
    public void close() throws SQLException {
        //AFLMonitor.getInstance().stopDBMS();
        //super.close();
    }

}

  // 实现其他必需的Statement方法...

