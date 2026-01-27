package sqlancer.mariadb.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.BaseConfigurationGenerator;
import sqlancer.ExpressionAction;
import sqlancer.ParameteraAwareGenerator;
import sqlancer.Randomly;
import sqlancer.common.gen.NoRECGenerator;
import sqlancer.common.gen.TLPWhereGenerator;
import sqlancer.common.schema.AbstractTables;
import sqlancer.mariadb.MariaDBProvider;
import sqlancer.mariadb.MariaDBSchema.MariaDBColumn;
import sqlancer.mariadb.MariaDBSchema.MariaDBDataType;
import sqlancer.mariadb.MariaDBSchema.MariaDBTable;
import sqlancer.mariadb.ast.*;
import sqlancer.mariadb.ast.MariaDBAggregate.MariaDBAggregateFunction;
import sqlancer.mariadb.ast.MariaDBBinaryOperator.MariaDBBinaryComparisonOperator;
import sqlancer.mariadb.ast.MariaDBPostfixUnaryOperation.MariaDBPostfixUnaryOperator;
import sqlancer.mariadb.ast.MariaDBSelectStatement.MariaDBSelectType;
import sqlancer.mariadb.ast.MariaDBUnaryPrefixOperation.MariaDBUnaryPrefixOperator;


import static sqlancer.ParameteraAwareGenerator.featureSet;

public class MariaDBExpressionGenerator
        implements NoRECGenerator<MariaDBSelectStatement, MariaDBJoin, MariaDBExpression, MariaDBTable, MariaDBColumn>, TLPWhereGenerator<MariaDBSelectStatement, MariaDBJoin, MariaDBExpression, MariaDBTable, MariaDBColumn> {

    private final Randomly r;
    private List<MariaDBTable> targetTables = new ArrayList<>();
    private List<MariaDBColumn> columns = new ArrayList<>();

    public MariaDBExpressionGenerator(Randomly r) {
        this.r = r;
    }

    public static MariaDBConstant getRandomConstant(Randomly r) {
        MariaDBDataType option = Randomly.fromOptions(MariaDBDataType.values());
        return getRandomConstant(r, option);
    }

    public static MariaDBConstant getRandomConstant(Randomly r, MariaDBDataType option) throws AssertionError {
        if (Randomly.getBooleanWithSmallProbability()) {
            return MariaDBConstant.createNullConstant();
        }
        switch (option) {
        case REAL:
            // FIXME: bug workaround for MDEV-21032
            return MariaDBConstant.createIntConstant(r.getInteger());
        // double val;
        // do {
        // val = r.getDouble();
        // } while (Double.isInfinite(val));
        // return MariaDBConstant.createDoubleConstant(val);
        case INT:
            return MariaDBConstant.createIntConstant(r.getInteger());
        case VARCHAR:
            return MariaDBConstant.createTextConstant(r.getString());
        case BOOLEAN:
            return MariaDBConstant.createBooleanConstant(Randomly.getBoolean());
        default:
            throw new AssertionError(option);
        }
    }

    public MariaDBExpressionGenerator setColumns(List<MariaDBColumn> columns) {
        this.columns = columns;
        return this;
    }

    //Tang: add parameter-aware generation
    private ExpressionType selectAction(){
        List<ExpressionType> expressionTypes = new ArrayList<>(Arrays.asList(ExpressionType.values()));
        if (columns.isEmpty()) {
            expressionTypes.remove(ExpressionType.COLUMN);
        }
        if(BaseConfigurationGenerator.isTrainingPhase){
            ExpressionType actions = Randomly.fromList(expressionTypes);
            featureSet.add(actions);
            return actions;
        } else {
            double random = Randomly.getPercentage();
            double cumulativeProbability = 0.0;

            for(ExpressionType action : expressionTypes) {

                cumulativeProbability += ParameteraAwareGenerator.comActionProbabilities[action.ordinal()];

                if (random <= cumulativeProbability) {
                    return action;
                }
            }
            return Randomly.fromOptions(ExpressionType.values());
        }
    }

    @Override
    public MariaDBExpression negatePredicate(MariaDBExpression predicate) {
        return new MariaDBUnaryPrefixOperation(predicate, MariaDBUnaryPrefixOperation.MariaDBUnaryPrefixOperator.NOT);
    }

    @Override
    public MariaDBExpression isNull(MariaDBExpression expr) {
        return new MariaDBPostfixUnaryOperation(MariaDBPostfixUnaryOperation.MariaDBPostfixUnaryOperator.IS_NULL,expr );
    }

    public enum ExpressionType implements ExpressionAction {
        COLUMN,LITERAL, BINARY_COMPARISON, UNARY_PREFIX_OPERATOR, UNARY_POSTFIX_OPERATOR, FUNCTION, IN
    }

    public MariaDBExpression getRandomExpression(int depth) {
        if (depth >= MariaDBProvider.MAX_EXPRESSION_DEPTH || Randomly.getBooleanWithRatherLowProbability()) {
            if (Randomly.getBoolean() || columns.isEmpty()) {
                return getRandomConstant(r);
            } else {
                return getRandomColumn();
            }
        }

        //ExpressionType expressionType = Randomly.fromList(expressionTypes);
        switch (selectAction()) {
        case COLUMN:
            getRandomColumn();
        case LITERAL:
            return getRandomConstant(r);
        case BINARY_COMPARISON:
            return new MariaDBBinaryOperator(getRandomExpression(depth + 1), getRandomExpression(depth + 1),
                    MariaDBBinaryComparisonOperator.getRandom());
        case UNARY_PREFIX_OPERATOR:
            return new MariaDBUnaryPrefixOperation(getRandomExpression(depth + 1),
                    MariaDBUnaryPrefixOperator.getRandom());
        case UNARY_POSTFIX_OPERATOR:
            return new MariaDBPostfixUnaryOperation(MariaDBPostfixUnaryOperator.getRandom(),
                    getRandomExpression(depth + 1));
        case FUNCTION:
            MariaDBFunctionName func = MariaDBFunctionName.getRandom();
            return new MariaDBFunction(func, getArgs(func, depth + 1));
        case IN:
            return new MariaDBInOperation(getRandomExpression(depth + 1), getSmallNumberRandomExpressions(depth + 1),
                    Randomly.getBoolean());
        default:
            throw new AssertionError();
        }
    }

    private List<MariaDBExpression> getSmallNumberRandomExpressions(int depth) {
        List<MariaDBExpression> expressions = new ArrayList<>();
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            expressions.add(getRandomExpression(depth + 1));
        }
        return expressions;
    }

    private List<MariaDBExpression> getArgs(MariaDBFunctionName func, int depth) {
        List<MariaDBExpression> expressions = new ArrayList<>();
        for (int i = 0; i < func.getNrArgs(); i++) {
            expressions.add(getRandomExpression(depth + 1));
        }
        if (func.isVariadic()) {
            for (int i = 0; i < Randomly.smallNumber(); i++) {
                expressions.add(getRandomExpression(depth + 1));
            }
        }
        return expressions;
    }

    private MariaDBExpression getRandomColumn() {
        MariaDBColumn randomColumn = Randomly.fromList(columns);
        return new MariaDBColumnName(randomColumn);
    }

    public MariaDBExpression getRandomExpression() {
        return getRandomExpression(0);
    }

    @Override
    public MariaDBExpressionGenerator setTablesAndColumns(AbstractTables<MariaDBTable, MariaDBColumn> targetTables) {
        this.targetTables = targetTables.getTables();
        this.columns = targetTables.getColumns();
        return this;
    }

    @Override
    public List<MariaDBExpression> getTableRefs() {
        List<MariaDBExpression> tableRefs = new ArrayList<>();
        for (MariaDBTable t : targetTables) {
            MariaDBTableReference tableRef = new MariaDBTableReference(t);
            tableRefs.add(tableRef);
        }
        return tableRefs;
    }

    @Override
    public List<MariaDBExpression> generateFetchColumns(boolean shouldCreateDummy) {
        return columns.stream().map(c -> new MariaDBColumnName(c)).collect(Collectors.toList());
    }

    @Override
    public List<MariaDBExpression> generateOrderBys() {
        List<MariaDBExpression> expressions =  getSmallNumberRandomExpressions(Randomly.smallNumber()%3+1);
        List<MariaDBExpression> newOrderBys = new ArrayList<>();
        for (MariaDBExpression expr : expressions) {
            if (Randomly.getBoolean()) {
                MariaDBOrderByTerm newExpr = new MariaDBOrderByTerm(expr, MariaDBOrderByTerm.MariaDBOrder.getRandomOrder());
                newOrderBys.add(newExpr);
            } else {
                newOrderBys.add(expr);
            }
        }
        return newOrderBys;
    }

    @Override
    public MariaDBExpression generateBooleanExpression() {
        return getRandomExpression();
    }

    @Override
    public MariaDBSelectStatement generateSelect() {
        return new MariaDBSelectStatement();
    }

    @Override
    public List<MariaDBJoin> getRandomJoinClauses() {
        return MariaDBJoin.getRandomJoinClauses(targetTables, r);
    }

    @Override
    public String generateOptimizedQueryString(MariaDBSelectStatement select, MariaDBExpression whereCondition,
            boolean shouldUseAggregate) {
        if (shouldUseAggregate) {
            MariaDBAggregate aggr = new MariaDBAggregate(
                    new MariaDBColumnName(new MariaDBColumn("*", MariaDBDataType.INT, false, 0)),
                    MariaDBAggregateFunction.COUNT);
            select.setFetchColumns(Arrays.asList(aggr));
        } else {
            MariaDBColumnName aggr = new MariaDBColumnName(MariaDBColumn.createDummy("*"));
            select.setFetchColumns(Arrays.asList(aggr));
        }

        select.setWhereClause(whereCondition);
        select.setSelectType(MariaDBSelectType.ALL);
        return select.asString();
    }

    @Override
    public String generateUnoptimizedQueryString(MariaDBSelectStatement select, MariaDBExpression whereCondition) {
        MariaDBPostfixUnaryOperation isTrue = new MariaDBPostfixUnaryOperation(MariaDBPostfixUnaryOperator.IS_TRUE,
                whereCondition);
        MariaDBText asText = new MariaDBText(isTrue, " as count", false);
        select.setFetchColumns(Arrays.asList(asText));
        select.setSelectType(MariaDBSelectType.ALL);

        return "SELECT SUM(count) FROM (" + select.asString() + ") as asdf";
    }
}
