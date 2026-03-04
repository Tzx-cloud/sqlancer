package sqlancer;

import java.util.Arrays;
import java.util.List;
import java.util.Set;


import static sqlancer.AFLMonitor.coverageBuf;
import static sqlancer.BaseConfigurationGenerator.parameterFeatureProbabilities;
import static sqlancer.MainOptions.AFL_MAP_SIZE;

/**
 * Implements Parameter-Aware Test Case Synthesis.
 * This class calculates the generation probability of SQL features based on
 * coverage feedback under specific database parameter configurations.
 */
public class ParameteraAwareGenerator {


    // Data structures to hold counts for probability calculations.
    // In a real scenario, these would be populated by a coverage tracker.
    // For this example, we'll use placeholder data.
    // Map<ParameterConfig, Map<GeneratorNode, Integer>>

    public static Set<ExpressionAction> featureSet = new java.util.HashSet<>();
    private final ExpressionAction[] actions;
    private int testCounts = 0;
    private final long[] featureCounts;
    //
    private final long[] edgeCounts = new long[AFL_MAP_SIZE];
    // Map<ParameterConfig, Map<GeneratorNode, Map<Edge, Integer>>>
    private final long[][] featureEdgeCounts;
    // Map<Edge, Integer>
    private final long[] totalEdgeHitCounts = new long[AFL_MAP_SIZE];
    public static double[] comActionProbabilities;


    /**
     * 构造函数。
     * @param actionClass 具体的 Action 枚举的 Class 对象，例如 MySQLExpressionGenerator.Actions.class
     */
    public ParameteraAwareGenerator(Class<? extends ExpressionAction> actionClass) {
        this.actions = actionClass.getEnumConstants();
        if (this.actions == null || this.actions.length == 0) {
            throw new IllegalArgumentException("Action enum class cannot be empty: " + actionClass.getName());
        }
        int numActions = this.actions.length;
        this.featureCounts = new long[numActions];
        this.featureEdgeCounts = new long[numActions][AFL_MAP_SIZE];
        comActionProbabilities = new double[numActions];
    }
    /**
     * Calculates the novelty score for an edge.
     * @param edge The edge identifier.
     * @return The novelty score.
     */
    private double getNovelty(int edge) {
        long hitCount = totalEdgeHitCounts[edge];
        return 1.0 / Math.sqrt(1.0 + hitCount);
    }



    /**
     * Calculates the mutual information between a feature and an edge for a given parameter configuration.
     * MI(f, e | c) = Σ P(f, e | c) * log2( P(f, e | c) / (P(f | c) * P(e | c)) )
     * @param feature The SQL feature (GeneratorNode).
     * @param edge The edge identifier.
     * @return The mutual information value.
     */
    private double calculateMutualInformation(int feature, int edge) {
        // 从数组中获取计数
        long countF1 = featureCounts[feature];
        long countE1 = edgeCounts[edge];
        long countF1E1 = featureEdgeCounts[feature][edge];



        // 计算联合事件和边缘事件的计数
        long countF0 = testCounts - countF1;
        long countE0 = testCounts - countE1;
        long countF1E0 = countF1 - countF1E1;
        long countF0E1 = countE1 - countF1E1;
        long countF0E0 = countF0 - countF0E1;

        // 如果总测试次数、特征计数或边计数为零，则互信息为零，提前返回
        if (testCounts == 0 || countF1 == 0 || countE1 == 0||countE0==0||countF0==0) {
            return 0.0;
        }
        double mi = 0.0;
        // 根据互信息公式，逐项计算
        // MI = Σ p(x,y) * log2( p(x,y) / (p(x)*p(y)) )
        if (countF1E1 > 0) {
            double pF1E1 = (double) countF1E1 / testCounts;
            mi += pF1E1 * Math.log((double) (countF1E1 * testCounts) / (countF1 * countE1));
        }
        if (countF1E0 > 0) {
            double pF1E0 = (double) countF1E0 / testCounts;
            mi += pF1E0 * Math.log((double) (countF1E0 * testCounts)/ (countF1  * countE0 ));
        }
        if (countF0E1 > 0) {
            double pF0E1 = (double) countF0E1 / testCounts;
            mi += pF0E1 * Math.log((double) (countF0E1 * testCounts) / (countF0 * countE1));

        }
        if (countF0E0 > 0) {
            double pF0E0 = (double) countF0E0 / testCounts;
            mi += pF0E0 * Math.log((double) (countF0E0 * testCounts) / ( countF0  * countE0 ));
        }
        // 将对数底从自然对数e转换为2
        return mi / Math.log(2);
    }

    /**
     * Calculates the weights for all features under the current parameter configuration.
     * @return A map from GeneratorNode to its calculated weight.
     */
    private double[] getFeatureWeights() {
        double[] weights = new double[featureCounts.length];

        // 优化：交换内外循环，外层遍历边，内层遍历特征
        for (int j = 0; j < edgeCounts.length; j++) {
            // 优化：如果一个边从未被触发，它对任何特征的权重贡献都为0，跳过
            if (edgeCounts[j] == 0) {
                continue;
            }

            // 优化：在内层循环外计算一次 novelty
            // 修正：getNovelty应该使用边的索引j，而不是特征的索引i
            double novelty = getNovelty(j);

            for (int i = 0; i < featureCounts.length; i++) {
                double mi = calculateMutualInformation(i, j);
                assert mi>=0:"MI should be non-negative";
                weights[i] += mi * novelty;
            }
        }
        return weights;
    }


    /**
     * Calculates the generation probabilities for all features based on their weights.
     * @return A map from GeneratorNode to its generation probability.
     */
    public double[]getFeatureProbabilities() {
        double[] weights = getFeatureWeights();
        double[] probabilities = new double[weights.length];
        double totalWeightPowered = Arrays.stream(weights).sum();

        if (totalWeightPowered == 0) {
            // Fallback to uniform probability if all weights are zero
            int numFeatures = actions.length;
            for (int i=0;i<weights.length;i++) {
                probabilities[i]= 1.0 / numFeatures;
            }
            return probabilities;
        }

        for (int i=0;i<weights.length;i++) {
            probabilities[i]= weights[i] / totalWeightPowered;
        }
        return probabilities;
    }

    public void updateCounts() {
        testCounts++;

        // 将对 featureCounts 的更新移到循环外，因为它与 coverageBuf 的内容无关
        for (ExpressionAction feature : featureSet) {
            featureCounts[feature.ordinal()] += 1;
        }

        // 只遍历一次 coverageBuf
        for (int i = 0; i < AFL_MAP_SIZE; i++) {
            // 仅在覆盖信息不为零时处理
            if (coverageBuf[i] != 0) {

                // 更新基本边计数
                edgeCounts[i] += 1;
                totalEdgeHitCounts[i] +=(coverageBuf[i]& 0xFF);

                // 一次性更新所有 feature 相关的边计数
                for (ExpressionAction feature : featureSet) {
                    featureEdgeCounts[feature.ordinal()][i] += 1;
                }
            }
        }
    }

    public void chooseFeature(List<BaseConfigurationGenerator.ConfigurationAction> configurationActions) {

        if (Randomly.getBooleanWithSmallProbability()) {
            // 随机选择特性
            for (int i = 0; i < actions.length; i++) {
                comActionProbabilities[i] = 1.0 / actions.length;
            }
        }else {
            computeComProbabilities(configurationActions);
        }

    }

    private void computeComProbabilities(List<BaseConfigurationGenerator.ConfigurationAction> configurationActions ) {
        double[] pro1 = parameterFeatureProbabilities.get(configurationActions.get(0));
        double[] pro2 = parameterFeatureProbabilities.get(configurationActions.get(1));
        for (int i = 0; i < actions.length; i++) {
            comActionProbabilities[i] = (pro1[i] + pro2[i])/2;
        }
    }
}
