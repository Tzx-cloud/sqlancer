package sqlancer.sqlite3.oracle.tlp;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.AFLMonitor;
import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.sqlite3.SQLite3GlobalState;
import sqlancer.sqlite3.SQLite3Visitor;
import sqlancer.sqlite3.ast.SQLite3Expression;
import sqlancer.sqlite3.ast.SQLite3Expression.SQLite3ColumnName;

public class SQLite3TLPGroupByOracle extends SQLite3TLPBase {

    private String generatedQueryString;

    public SQLite3TLPGroupByOracle(SQLite3GlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        select.setGroupByClause(select.getFetchColumns());
        select.setWhereClause(null);
        String originalQueryString = SQLite3Visitor.asString(select);
        generatedQueryString = originalQueryString;
        try {
            AFLMonitor.getInstance().executeSQLStatement( generatedQueryString);
            List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

            select.setWhereClause(predicate);
            String firstQueryString = SQLite3Visitor.asString(select);
            select.setWhereClause(negatedPredicate);
            String secondQueryString = SQLite3Visitor.asString(select);
            select.setWhereClause(isNullPredicate);
            String thirdQueryString = SQLite3Visitor.asString(select);
            List<String> combinedString = new ArrayList<>();
            List<String> secondResultSet = null;
            secondResultSet = ComparatorHelper.getCombinedResultSetNoDuplicates(firstQueryString,
                    secondQueryString, thirdQueryString, combinedString, true, state, errors);
            AFLMonitor.getInstance().executeSQLStatement(combinedString.get(0));
            ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                    state);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    List<SQLite3Expression> generateFetchColumns() {
        return Randomly.nonEmptySubset(targetTables.getColumns()).stream().map(c -> new SQLite3ColumnName(c, null))
                .collect(Collectors.toList());
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }

}
