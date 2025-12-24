package sqlancer;

import sqlancer.common.query.SQLQueryAdapter;
import java.util.*;
import java.util.function.Function;

public abstract class BaseConfigurationGenerator {
    protected final Randomly r;
    protected final StringBuilder sb = new StringBuilder();
    protected boolean isSingleThreaded;


    // 训练相关的静态变量
    protected static Map<ConfigurationAction, Double> databaseParameterProbabilities = new HashMap<>(1000);
    public static Map<ConfigurationAction, double[]> parameterFeatureProbabilities = new HashMap<>(1000);
    public static Map<Set<ConfigurationAction>, Double> allParameterCombos= new HashMap<>(1000000);
    public static Map<Set<ConfigurationAction>, Double> proParameterCombos= new HashMap<>(1000);
    public static boolean isTrainingPhase = false;
    public static final int TRAINING_SAMPLES = 1;


    // 覆盖率相关
    public static Map<String, byte[]> parameterEdgeCoverage = new HashMap<>();

    /** 判断 a 是否比 b 更应该留在 Top-K（更大权重更好）。 */
    private static boolean isBetter(Map.Entry<?,Double> a, Map.Entry<?,Double> b) {
        int cmp = Double.compare(a.getValue(), b.getValue());
        if (cmp != 0) {
            return cmp > 0;
        }
        // 权重相同：tieBreaker 更大者当作更好（只为稳定）
        return proParameterCombos.containsKey(b.getKey()) ;
    }

    public enum Scope {
        GLOBAL, SESSION
    }

    public interface ConfigurationAction {
        String getName();
        Object generateValue(Randomly r);
        Scope[] getScopes();
        boolean canBeUsedInScope(Scope scope);
    }

    protected static class GenericAction implements ConfigurationAction {
        private final String name;
        private final Function<Randomly, Object> producer;
        private final Scope[] scopes;

        public GenericAction(String name, Function<Randomly, Object> producer, Scope... scopes) {
            if (scopes.length == 0) {
                throw new AssertionError("Action must have at least one scope: " + name);
            }
            this.name = name;
            this.producer = producer;
            this.scopes = scopes.clone();
        }
        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object generateValue(Randomly r) {
            return producer.apply(r);
        }

        @Override
        public Scope[] getScopes() {
            return scopes.clone();
        }

        @Override
        public boolean canBeUsedInScope(Scope scope) {
            for (Scope s : scopes) {
                if (s == scope) {
                    return true;
                }
            }
            return false;
        }


    }

    public BaseConfigurationGenerator(Randomly r, MainOptions options) {
        this.r = r;
        this.isSingleThreaded = options.getNumberConcurrentThreads() == 1;
    }



    // 抽象方法，子类必须实现
    protected abstract String getDatabaseType();
    public abstract ConfigurationAction[] getAllActions();
    protected abstract SQLQueryAdapter generateConfigForAction(Object action);
    protected abstract String getActionName(Object action);
    public abstract SQLQueryAdapter generateConfigForParameter( ConfigurationAction action);
    public abstract SQLQueryAdapter generateDefaultConfigForParameter( ConfigurationAction action);

//
//    public void calculateParameterWeights() {
//        Double[] edgeScores = calculateEdgeScores();
//        Map<String, Double> parameterWeights = new HashMap<>();
//        double totalWeight = 0.0;
//
//        // 计算每个参数的权重
//        for (String parameter : parameterEdgeCoverage.keySet()) {
//            double weight = 0.0;
//            byte[] coveredEdges = parameterEdgeCoverage.get(parameter);
//            for(int i=0;i< coveredEdges.length;i++){
//                if(coveredEdges[i]!=0){
//                    weight += edgeScores[i];
//                }
//            }
//            parameterWeights.put(parameter, weight);
//            totalWeight += weight;
//        }
//
//        // 计算概率
//        for (Map.Entry<String, Double> entry : parameterWeights.entrySet()) {
//            databaseParameterProbabilities.put(entry.getKey(),entry.getValue() / totalWeight);
//        }
//        parameterEdgeCoverage.clear();
//    }

    private Double[] calculateEdgeScores() {
        Double[] edgeScores = new Double[AFLMonitor.AFL_MAP_SIZE];
        int totalParameters = parameterEdgeCoverage.size();

        for (int i=0;i< AFLMonitor.AFL_MAP_SIZE;i++) {
            int coveringParametersCount = 0;
            for (byte[] coveredEdges : parameterEdgeCoverage.values()) {
                if (coveredEdges[i]!=0) {
                    coveringParametersCount++;
                }
            }

            if (coveringParametersCount > 0) {
                double score = Math.log(1.0 + (double) totalParameters / coveringParametersCount);
                edgeScores[i]=score;
            }
        }

        return edgeScores;
    }

    private SQLQueryAdapter generateByWeight() {
        Object selectedAction = selectActionsByWeight();
        return generateConfigForAction(selectedAction);
    }

    private List<ConfigurationAction> selectActionsByWeight() {

        double random = Math.random();
        double cumulativeProbability = 0.0;

        for (Set<ConfigurationAction> actions : proParameterCombos.keySet()) {

            double probability = proParameterCombos
                    .getOrDefault(actions, 1.0 / proParameterCombos.size());
            cumulativeProbability += probability;

            if (random <= cumulativeProbability) {
                return new ArrayList<>(actions);
            }
        }
        Set<ConfigurationAction> randomActionSet = Randomly.fromOptions(proParameterCombos.keySet().toArray(new Set[0]));
        return new ArrayList<>(randomActionSet);
    }

    public List<ConfigurationAction> generateActions() {
        if(Randomly.getBooleanWithSmallProbability()) {
            Set<ConfigurationAction> randomActionSet = Randomly.fromOptions(allParameterCombos.keySet().toArray(new Set[0]));
            while (proParameterCombos.containsKey(randomActionSet)) {
                randomActionSet = Randomly.fromOptions(allParameterCombos.keySet().toArray(new Set[0]));
            }
            return new ArrayList<>(randomActionSet);
        } else {
            return selectActionsByWeight();
        }
    }
    public void topKSnapshot(int k) {
        // 最小堆：堆顶是当前 Top-K 里最小的那个
        PriorityQueue<Map.Entry<Set<ConfigurationAction>,Double>> minHeap = new PriorityQueue<>(
                Comparator.comparingDouble(Map.Entry::getValue)
        );

        // tieBreaker 用 System.identityHashCode 防止大量相同 weight 时比较器不稳定导致异常
        for (Map.Entry<Set<ConfigurationAction>, Double> e : allParameterCombos.entrySet()) {
            Set<ConfigurationAction> id = e.getKey();
            double w = e.getValue() == null ? 0.0 : e.getValue();



            if (minHeap.size() < k) {
                minHeap.offer(e);
            } else if (isBetter(e, minHeap.peek())) {
                minHeap.poll();
                minHeap.offer(e);
            }
        }


        Map<Set<ConfigurationAction>, Double> top = new HashMap<>(minHeap.size());
        for (Map.Entry<Set<ConfigurationAction>, Double> e : minHeap) {
            top.put(e.getKey(), e.getValue());
        }

        proParameterCombos.clear();
        proParameterCombos.putAll(top);

    }
}

