package sqlancer;

import sqlancer.common.query.SQLQueryAdapter;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseConfigurationGenerator  {
    protected final Randomly r;
    protected final StringBuilder sb = new StringBuilder();
    protected boolean isSingleThreaded;


    // 训练相关的静态变量
    public static Map<ConfigurationAction, double[]> parameterFeatureProbabilities = new HashMap<>(1000);
    public static Map<Set<ConfigurationAction>, Double> allParameterCombos= new HashMap<>(1000000);
    public static Map<Set<ConfigurationAction>, Double> proParameterCombos= new HashMap<>(1000);
    public static boolean isTrainingPhase = false;
    public static final int TRAINING_SAMPLES = 4;
    private double weightSum = 0.0;
    public static List<ConfigurationAction> currentGeneratedActions = new ArrayList<>();

    public abstract ConfigurationAction[] getAllActions();
    public  ConfigurationAction getActionByName(String name) {
            for (ConfigurationAction action : getAllActions()) {
                    if (action.getName().equals(name)) {
                        return action;
                    }
            }
        return null;
    }

    /**
     * 将 parameterFeatureProbabilities 的内容保存到文件。
     * 文件格式为: actionName:prob1,prob2,prob3...
     *
     * @param filePath 要保存到的文件路径
     * @throws IOException 如果文件写入失败
     */
    public void saveParameterFeatureProbabilitiesToFile(String filePath) throws IOException {
        filePath =filePath+ "_feature_weights.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Map.Entry<ConfigurationAction, double[]> entry : parameterFeatureProbabilities.entrySet()) {
                ConfigurationAction action = entry.getKey();
                double[] probabilities = entry.getValue();

                // 将 double 数组转换为逗号分隔的字符串
                String probabilitiesString = Arrays.stream(probabilities)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(","));

                // 写入文件
                writer.write(action.getName() + ":" + probabilitiesString);
                writer.newLine();
            }
        }
        System.out.println("成功将 " + parameterFeatureProbabilities.size() + " 个参数特性概率保存到 '" + filePath + "'。");
    }

    /**
     * 从文件中加载参数特性概率。
     * 文件格式应为: actionName:prob1,prob2,prob3...
     *
     * @param filePath 权重文件的路径
     * @throws IOException 如果文件读取失败
     */
    public boolean loadParameterFeatureProbabilitiesFromFile(String filePath) throws IOException {
        // 加载前清空当前的 Map
        parameterFeatureProbabilities.clear();
        filePath =filePath+ "_feature_weights.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) { // 忽略空行和注释
                    continue;
                }

                String[] parts = line.split(":");
                if (parts.length != 2) {
                    System.err.println("警告: 格式错误的行将被忽略: " + line);
                    continue;
                }

                String actionName = parts[0].trim();
                String probabilitiesPart = parts[1];

                try {
                    // 1. 根据名称查找 ConfigurationAction 对象
                    ConfigurationAction action = getActionByName(actionName);
                    if (action == null) {
                        System.err.println("警告: 未找到名为 '" + actionName + "' 的 Action，该行将被忽略。");
                        continue;
                    }

                    // 2. 解析概率字符串为 double 数组
                    double[] probabilities = Arrays.stream(probabilitiesPart.split(","))
                            .map(String::trim)
                            .mapToDouble(Double::parseDouble)
                            .toArray();

                    // 3. 存入 parameterFeatureProbabilities
                    parameterFeatureProbabilities.put(action, probabilities);

                } catch (NumberFormatException e) {
                    System.err.println("警告: 概率值格式错误，该行将被忽略: " + line);
                } catch (Exception e) {
                    System.err.println("警告: 处理行时发生未知错误，将被忽略: " + line + " - " + e.getMessage());
                }
            }
        }catch(FileNotFoundException e){
            System.err.println("警告: 未找到文件 '" + filePath + "'，无法加载参数特性概率。");
            return false;
        }
        if(parameterFeatureProbabilities.isEmpty()) {
            return false;
        }

        if(parameterFeatureProbabilities.values().iterator().next().length != getAllActions().length) {

        }
        if (parameterFeatureProbabilities.size() != getAllActions().length) {
            System.err.println("错误: 加载的参数数量与当前数量不匹配。");
            parameterFeatureProbabilities.clear();
            return false;
        }
        System.out.println("成功从 '" + filePath + "' 加载了 " + parameterFeatureProbabilities.size() + " 个参数特性概率。");
        return true;
    }

    /**
     * 从文件中加载参数组合的权重。
     * 文件格式应为: action1,action2:weight
     *
     * @param filePath 权重文件的路径
     * @throws IOException 如果文件读取失败
     */

    public  void loadWeightsFromFile(String filePath) throws IOException {
        // 在加载新权重前，清空当前的 Map
        allParameterCombos.clear();
        filePath =filePath+ "_config_weights.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) { // 忽略空行和注释
                    continue;
                }

                String[] parts = line.split(":");
                if (parts.length != 2) {
                    System.err.println("警告: 格式错误的行将被忽略: " + line);
                    continue;
                }

                String actionsPart = parts[0];
                String weightPart = parts[1];

                try {
                    // 1. 解析权重
                    double weight = Double.parseDouble(weightPart);

                    // 2. 解析 Action 名称并转换为 Set<ConfigurationAction>
                    // 使用提供的函数将名称映射到对象
                    // 过滤掉未找到的 action
                    Set<ConfigurationAction> actionSet = new HashSet<>();
                    for (String s : actionsPart.split(",")) {
                        String trim = s.trim();
                        ConfigurationAction actionByName = getActionByName(trim);
                        if (actionByName != null) {
                            actionSet.add(actionByName);
                        }
                    }

                    if (actionSet.isEmpty()) {
                        System.err.println("警告: 未能从 '" + actionsPart + "' 中解析出任何有效的 Action，该行将被忽略。");
                        continue;
                    }

                    // 3. 存入 allParameterCombos
                    allParameterCombos.put(actionSet, weight);

                } catch (NumberFormatException e) {
                    System.err.println("警告: 权重值格式错误，该行将被忽略: " + line);
                } catch (Exception e) {
                    System.err.println("警告: 处理行时发生未知错误，将被忽略: " + line + " - " + e.getMessage());
                }
            }
        }catch(FileNotFoundException e){
            System.err.println("警告: 未找到文件 '" + filePath + "'，无法加载参数特性概率。");
        }
        System.out.println("成功从 '" + filePath + "' 加载了 " + allParameterCombos.size() + " 个参数组合权重。");
    }
    /** 判断 a 是否比 b 更应该留在 Top-K（更大权重更好）。 */
    private boolean isBetter(Map.Entry<?,Double> a, Map.Entry<?,Double> b) {
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

        public boolean equals(Object o);

        public int hashCode();
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            //if (o == null || getClass() != o.getClass()) return false;
            ConfigurationAction that = (ConfigurationAction) o;
            // 比较所有关键字段
            if (!Objects.equals(name, that.getName())) return false;
            return true;
        }

        @Override
        public int hashCode() {
            // 使用相同的关键字段生成哈希码
            return Objects.hash(name);
        }
    }

    public BaseConfigurationGenerator(Randomly r, MainOptions options) {
        this.r = r;
        this.isSingleThreaded = options.getNumberConcurrentThreads() == 1;
    }



    // 抽象方法，子类必须实现
    protected abstract String getDatabaseType();
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

//    private Double[] calculateEdgeScores() {
//        Double[] edgeScores = new Double[AFLMonitor.AFL_MAP_SIZE];
//        int totalParameters = parameterEdgeCoverage.size();
//
//        for (int i=0;i< AFLMonitor.AFL_MAP_SIZE;i++) {
//            int coveringParametersCount = 0;
//            for (byte[] coveredEdges : parameterEdgeCoverage.values()) {
//                if (coveredEdges[i]!=0) {
//                    coveringParametersCount++;
//                }
//            }
//
//            if (coveringParametersCount > 0) {
//                double score = Math.log(1.0 + (double) totalParameters / coveringParametersCount);
//                edgeScores[i]=score;
//            }
//        }
//
//        return edgeScores;
//    }
//
//    private SQLQueryAdapter generateByWeight() {
//        Object selectedAction = selectActionsByWeight();
//        return generateConfigForAction(selectedAction);
//    }

    private List<ConfigurationAction> selectActionsByWeight() {

        double random = Randomly.getPercentage() * weightSum;
        double cumulativeProbability = 0.0;

        for (Set<ConfigurationAction> actions : proParameterCombos.keySet()) {

            double probability = proParameterCombos
                    .getOrDefault(actions, 1.0 / proParameterCombos.size());
            cumulativeProbability += probability;

            if (random <= cumulativeProbability) {
                return new ArrayList<>(actions);
            }
        }
        Set randomActionSet = Randomly.fromOptions(proParameterCombos.keySet().toArray(new Set[0]));
        return new ArrayList<>(randomActionSet);
    }

    public void generateActions() {
        if(Randomly.getBooleanWithSmallProbability()) {
            Set randomActionSet = Randomly.fromOptions(allParameterCombos.keySet().toArray(new Set[0]));
            while (proParameterCombos.containsKey(randomActionSet)) {
                randomActionSet = Randomly.fromOptions(allParameterCombos.keySet().toArray(new Set[0]));
            }
            currentGeneratedActions= new ArrayList<>(randomActionSet);
        } else {
            currentGeneratedActions= selectActionsByWeight();
        }
    }
    public void topKSnapshot(int k) {
        // 最小堆：堆顶是当前 Top-K 里最小的那个
        weightSum=0.0;
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
            weightSum += e.getValue();
            top.put(e.getKey(), e.getValue());
        }

        proParameterCombos.clear();
        proParameterCombos.putAll(top);

    }
}

