package sqlancer;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import sqlancer.StateToReproduce.OracleRunReproductionState;
import sqlancer.common.DBMSCommon;
import sqlancer.common.oracle.CompositeTestOracle;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.schema.AbstractSchema;

import static sqlancer.ParameteraAwareGenerator.featureSet;

public abstract class ProviderAdapter<G extends GlobalState<O, ? extends AbstractSchema<G, ?>, C>, O extends DBMSSpecificOptions<? extends OracleFactory<G>>, C extends SQLancerDBConnection>
        implements DatabaseProvider<G, O, C> {

    private final Class<G> globalClass;
    private final Class<O> optionClass;

    // Variables for QPG
    Map<String, String> queryPlanPool = new HashMap<>();
    static double[] weightedAverageReward; // static variable for sharing across all threads
    int currentSelectRewards;
    int currentSelectCounts;
    int currentMutationOperator = -1;

    protected ProviderAdapter(Class<G> globalClass, Class<O> optionClass) {
        this.globalClass = globalClass;
        this.optionClass = optionClass;
    }

    @Override
    public StateToReproduce getStateToReproduce(String databaseName) {
        return new StateToReproduce(databaseName, this);
    }

    @Override
    public Class<G> getGlobalStateClass() {
        return globalClass;
    }

    @Override
    public Class<O> getOptionClass() {
        return optionClass;
    }

    public Class<? extends ExpressionAction> getActionClass(){return ExpressionAction.class;}

    @Override
    //Tang: 生成配置参数并进行测试
    public void generateDatabaseWithConfigurationTest(G globalState, List<BaseConfigurationGenerator.ConfigurationAction> actions) throws Exception{
        //Tang: 生成配置参数并进行训练
        ParameteraAwareGenerator parameterAwareGenerator = new ParameteraAwareGenerator(getActionClass());
        List<? extends OracleFactory<G>> testOracleFactory = globalState.getDbmsSpecificOptions()
                .getTestOracleFactory();
        try {
            for (int i = 0; i < BaseConfigurationGenerator.TRAINING_SAMPLES; i++) {
                generateConfiguration(globalState, actions.get(0));
                generateConfiguration(globalState, actions.get(1));
                generateDatabase(globalState);
                checkViewsAreValid(globalState);
                globalState.getManager().incrementCreateDatabase();
                TestOracle<G> testOracle = testOracleFactory.get(0).create(globalState);
                for (int j = 0; j <10000; j++) {
                    try (OracleRunReproductionState localState = globalState.getState().createLocalState()) {
                        assert localState != null;
                        try {
                            globalState.getManager().incrementSelectQueryCount();
                            AFLMonitor.getInstance().clearCoverage();
                            testOracle.check();
                            AFLMonitor.getInstance().refreshBuffer();
                            parameterAwareGenerator.updateCounts();

                            Main.nrSuccessfulActions.addAndGet(1);
                        } catch (IgnoreMeException ignored) {
                        } catch (AssertionError e) {
                            e.printStackTrace();
                            throw e;
                        }
                        localState.executedWithoutError();
                    }
                }
            }
            generateDefaultConfiguration(globalState, actions.get(0));
            generateDefaultConfiguration(globalState, actions.get(1));
        }finally {
            globalState.setSchema(null);
            globalState.getConnection().close();
        }
    }

    @Override
    public void generateDatabaseWithConfigurationTraining(G globalState, BaseConfigurationGenerator.ConfigurationAction action) throws Exception{
        //Tang: 生成配置参数并进行训练
        ParameteraAwareGenerator parameterAwareGenerator = new ParameteraAwareGenerator(getActionClass());
        List<? extends OracleFactory<G>> testOracleFactory = globalState.getDbmsSpecificOptions()
                .getTestOracleFactory();
        try {
            for (int i = 0; i < BaseConfigurationGenerator.TRAINING_SAMPLES; i++) {
                generateConfiguration(globalState, action);
                generateDatabase(globalState);
                checkViewsAreValid(globalState);
                globalState.getManager().incrementCreateDatabase();
                TestOracle<G> testOracle = testOracleFactory.get(0).create(globalState);
                for (int j = 0; j <10000; j++) {
                    try (OracleRunReproductionState localState = globalState.getState().createLocalState()) {
                        assert localState != null;
                        try {
                            globalState.getManager().incrementSelectQueryCount();
                            featureSet.clear();
                            AFLMonitor.getInstance().clearCoverage();
                            testOracle.genSelect();
                            AFLMonitor.getInstance().refreshBuffer();
                            parameterAwareGenerator.updateCounts();

                            Main.nrSuccessfulActions.addAndGet(1);
                        } catch (IgnoreMeException ignored) {
                        } catch (AssertionError e) {
                            e.printStackTrace();
                            throw e;
                        }
                        localState.executedWithoutError();
                    }
                }
            }
            generateDefaultConfiguration(globalState, action);
        }finally {
            double[] featureProbabilities = parameterAwareGenerator.getFeatureProbabilities();
            BaseConfigurationGenerator.parameterFeatureProbabilities.putIfAbsent(action, featureProbabilities.clone());
            globalState.setSchema(null);
            globalState.getConnection().close();
        }

    }



    @Override
    public Reproducer<G> generateAndTestDatabase(G globalState) throws Exception {
        try {
            generateDatabase(globalState);
            checkViewsAreValid(globalState);
            globalState.getManager().incrementCreateDatabase();

            TestOracle<G> oracle = getTestOracle(globalState);
            for (int i = 0; i < globalState.getOptions().getNrQueries(); i++) {
                try (OracleRunReproductionState localState = globalState.getState().createLocalState()) {
                    assert localState != null;
                    try {
                        oracle.check();
                        globalState.getManager().incrementSelectQueryCount();
                    } catch (IgnoreMeException ignored) {
                    } catch (AssertionError e) {
                        Reproducer<G> reproducer = oracle.getLastReproducer();
                        if (reproducer != null) {
                            return reproducer;
                        }
                        throw e;
                    }
                    localState.executedWithoutError();
                }
            }
        } finally {
            globalState.getConnection().close();
        }
        return null;
    }

    protected abstract void checkViewsAreValid(G globalState) throws SQLException;

    protected TestOracle<G> getTestOracle(G globalState) throws Exception {
        List<? extends OracleFactory<G>> testOracleFactory = globalState.getDbmsSpecificOptions()
                .getTestOracleFactory();
        boolean testOracleRequiresMoreThanZeroRows = testOracleFactory.stream()
                .anyMatch(OracleFactory::requiresAllTablesToContainRows);
        boolean userRequiresMoreThanZeroRows = globalState.getOptions().testOnlyWithMoreThanZeroRows();
        boolean checkZeroRows = testOracleRequiresMoreThanZeroRows || userRequiresMoreThanZeroRows;
        if (checkZeroRows && globalState.getSchema().containsTableWithZeroRows(globalState)) {
            if (globalState.getOptions().enableQPG()) {
                addRowsToAllTables(globalState);
            } else {
                throw new IgnoreMeException();
            }
        }
        if (testOracleFactory.size() == 1) {
            return testOracleFactory.get(0).create(globalState);
        } else {
            return new CompositeTestOracle<>(testOracleFactory.stream().map(o -> {
                try {
                    return o.create(globalState);
                } catch (Exception e1) {
                    throw new AssertionError(e1);
                }
            }).collect(Collectors.toList()), globalState);
        }
    }

    public abstract void generateDatabase(G globalState) throws Exception;
    //Tang: 生成配置参数
    public void generateConfiguration(G globalState, BaseConfigurationGenerator.ConfigurationAction action) throws Exception{};
    public void generateDefaultConfiguration(G globalState, BaseConfigurationGenerator.ConfigurationAction action) throws Exception{};

    // QPG: entry function
    @Override
    public void generateAndTestDatabaseWithQueryPlanGuidance(G globalState) throws Exception {
        if (weightedAverageReward == null) {
            weightedAverageReward = initializeWeightedAverageReward(); // Same length as the list of mutators
        }
        try {
            generateDatabase(globalState);
            checkViewsAreValid(globalState);
            globalState.getManager().incrementCreateDatabase();

            Long executedQueryCount = 0L;
            while (executedQueryCount < globalState.getOptions().getNrQueries()) {
                int numOfNoNewQueryPlans = 0;
                TestOracle<G> oracle = getTestOracle(globalState);
                while (executedQueryCount < globalState.getOptions().getNrQueries()) {
                    try (OracleRunReproductionState localState = globalState.getState().createLocalState()) {
                        assert localState != null;
                        try {
                            oracle.check();
                            String query = oracle.getLastQueryString();
                            executedQueryCount += 1;
                            if (addQueryPlan(query, globalState)) {
                                numOfNoNewQueryPlans = 0;
                            } else {
                                numOfNoNewQueryPlans++;
                            }
                            globalState.getManager().incrementSelectQueryCount();
                        } catch (IgnoreMeException e) {

                        }
                        localState.executedWithoutError();
                    }
                    // exit loop to mutate tables if no new query plans have been found after a while
                    if (numOfNoNewQueryPlans > globalState.getOptions().getQPGMaxMutationInterval()) {
                        mutateTables(globalState);
                        break;
                    }
                }
            }
        } finally {
            globalState.getConnection().close();
        }
    }

    // QPG: mutate tables for a new database state
    private synchronized boolean mutateTables(G globalState) throws Exception {
        // Update rewards based on a set of newly generated queries in last iteration
        if (currentMutationOperator != -1) {
            weightedAverageReward[currentMutationOperator] += ((double) currentSelectRewards
                    / (double) currentSelectCounts) * globalState.getOptions().getQPGk();
        }
        currentMutationOperator = -1;

        // Choose mutator based on the rewards
        int selectedActionIndex = 0;
        if (Randomly.getPercentage() < globalState.getOptions().getQPGProbability()) {
            selectedActionIndex = globalState.getRandomly().getInteger(0, weightedAverageReward.length);
        } else {
            selectedActionIndex = DBMSCommon.getMaxIndexInDoubleArray(weightedAverageReward);
        }
        int reward = 0;

        try {
            executeMutator(selectedActionIndex, globalState);
            checkViewsAreValid(globalState); // Remove the invalid views
            reward = checkQueryPlan(globalState);
        } catch (IgnoreMeException | AssertionError e) {
        } finally {
            // Update rewards based on existing queries associated with the query plan pool
            updateReward(selectedActionIndex, (double) reward / (double) queryPlanPool.size(), globalState);
            currentMutationOperator = selectedActionIndex;
        }

        // Clear the variables for storing the rewards of the action on a set of newly generated queries
        currentSelectRewards = 0;
        currentSelectCounts = 0;
        return true;
    }

    // QPG: add a query plan to the query plan pool and return true if the query plan is new
    private boolean addQueryPlan(String selectStr, G globalState) throws Exception {
        String queryPlan = getQueryPlan(selectStr, globalState);

        if (globalState.getOptions().logQueryPlan()) {
            globalState.getLogger().writeQueryPlan(queryPlan);
        }

        currentSelectCounts += 1;
        if (queryPlanPool.containsKey(queryPlan)) {
            return false;
        } else {
            queryPlanPool.put(queryPlan, selectStr);
            currentSelectRewards += 1;
            return true;
        }
    }

    // Obtain the reward of the current action based on the queries associated with the query plan pool
    private int checkQueryPlan(G globalState) throws Exception {
        int newQueryPlanFound = 0;
        HashMap<String, String> modifiedQueryPlan = new HashMap<>();
        for (Iterator<Map.Entry<String, String>> it = queryPlanPool.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> item = it.next();
            String queryPlan = item.getKey();
            String selectStr = item.getValue();
            String newQueryPlan = getQueryPlan(selectStr, globalState);
            if (newQueryPlan.isEmpty()) { // Invalid query
                it.remove();
            } else if (!queryPlan.equals(newQueryPlan)) { // A query plan has been changed
                it.remove();
                modifiedQueryPlan.put(newQueryPlan, selectStr);
                if (!queryPlanPool.containsKey(newQueryPlan)) { // A new query plan is found
                    newQueryPlanFound++;
                }
            }
        }
        queryPlanPool.putAll(modifiedQueryPlan);
        return newQueryPlanFound;
    }

    // QPG: update the reward of current action
    private void updateReward(int actionIndex, double reward, G globalState) {
        weightedAverageReward[actionIndex] += (reward - weightedAverageReward[actionIndex])
                * globalState.getOptions().getQPGk();
    }

    // QPG: initialize the weighted average reward of all mutation operators (required implementation in specific DBMS)
    protected double[] initializeWeightedAverageReward() {
        throw new UnsupportedOperationException();
    }

    // QPG: obtain the query plan of a query (required implementation in specific DBMS)
    protected String getQueryPlan(String selectStr, G globalState) throws Exception {
        throw new UnsupportedOperationException();
    }

    // QPG: execute a mutation operator (required implementation in specific DBMS)
    protected void executeMutator(int index, G globalState) throws Exception {
        throw new UnsupportedOperationException();
    }

    // QPG: add rows to all tables (required implementation in specific DBMS when enabling PQS oracle for QPG)
    protected boolean addRowsToAllTables(G globalState) throws Exception {
        throw new UnsupportedOperationException();
    }

}
