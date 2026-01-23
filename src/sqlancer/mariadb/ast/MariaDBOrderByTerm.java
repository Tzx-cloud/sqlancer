package sqlancer.mariadb.ast;

import sqlancer.Randomly;

public class MariaDBOrderByTerm implements MariaDBExpression {

    private final MariaDBOrder order;
    private final MariaDBExpression expr;

    public enum MariaDBOrder {
        ASC, DESC;

        public static MariaDBOrder getRandomOrder() {
            return Randomly.fromOptions(MariaDBOrder.values());
        }
    }

    public MariaDBOrderByTerm(MariaDBExpression expr, MariaDBOrder order) {
        this.expr = expr;
        this.order = order;
    }

    public MariaDBOrder getOrder() {
        return order;
    }

    public MariaDBExpression getExpr() {
        return expr;
    }


}
