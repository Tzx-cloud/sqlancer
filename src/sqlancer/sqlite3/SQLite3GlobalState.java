package sqlancer.sqlite3;

import java.sql.SQLException;

import sqlancer.AFLMonitor;
import sqlancer.ExecutionTimer;
import sqlancer.SQLConnection;
import sqlancer.SQLGlobalState;
import sqlancer.common.query.Query;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.sqlite3.schema.SQLite3Schema;

public class SQLite3GlobalState extends SQLGlobalState<SQLite3Options, SQLite3Schema> {

    @Override
    protected SQLite3Schema readSchema() throws SQLException {
        return SQLite3Schema.fromConnection(this);
    }

    @Override
    public boolean executeStatement(Query<SQLConnection> q, String... fills) throws Exception {

        AFLMonitor.getInstance().executeSQLStatement(q.getQueryString());
        // 原有逻辑

        return super.executeStatement(q, fills);
    }
}
