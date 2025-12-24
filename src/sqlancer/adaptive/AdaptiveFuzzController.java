package sqlancer.adaptive;

import sqlancer.AFLMonitor;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class AdaptiveFuzzController {

    // ===== 方案参数 =====
    private final double epsInit = 0.5;
    private final double epsMin = 0.1;
    private final double tau1 = 10000.0;

    private final double beta = 1.0;          // 奖励放大系数，可调
    private final double lambda = 0.0001;     // 时间衰减率
    private final int updateEvery = 1000;     // 每1000个参数组合更新

    private final long sliceMs = 60_000;      // 1分钟
    private final AFLMonitor monitor;

    // 参数组合空间（可由你的 Pre-allocation 生成/2-wise 生成器喂入）
    private final List<ParamCombo> allCombos;
    private final FeatureSpace featureSpace;

    // 运行状态
    private long t = 0;
    private long lastDiscoveryT = 0;

    // 动态统计（用于R）
    private final Map<ParamCombo, ComboStats> stats = new HashMap<>();

    // 分布 p(C): 用权重表实现（soft distribution）
    private final WeightedSampler<ParamCombo> comboSampler;

    public AdaptiveFuzzController(AFLMonitor monitor,
                                  List<ParamCombo> allCombos,
                                  FeatureSpace featureSpace,
                                  Map<ParamCombo, Double> staticWeights) {
        this.monitor = Objects.requireNonNull(monitor);
        this.allCombos = Collections.unmodifiableList(new ArrayList<>(allCombos));
        this.featureSpace = Objects.requireNonNull(featureSpace);

        for (ParamCombo c : this.allCombos) {
            stats.put(c, new ComboStats());
        }
        this.comboSampler = new WeightedSampler<>();
        // 初始化 p(C) 为静态权重
        for (ParamCombo c : this.allCombos) {
            double w = staticWeights.getOrDefault(c, 1.0);
            comboSampler.setWeight(c, Math.max(1e-12, w));
        }
    }

    public void runForever() {
        while (true) {
            stepOnce();
        }
    }

    public void stepOnce() {
        t++;

        double eps1 = epsilon1(t);
        double eps2 = Math.min(1.0, eps1 * 2.0);

        // 1) 选择参数组合 Ct（ε-greedy + 2-wise风格的uniform探索）
        ParamCombo ct = chooseCombo(eps1);

        // 2) 基于 ct 选择特性 ft（conditional + ε2 uniform）
        Feature ft = chooseFeature(ct, eps2);

        // 3) 时间片执行与变异策略（1min, 有新边续1min, 否则变异再1min）
        boolean discovered = executeWithTimeSlicing(ct, ft);

        if (discovered) {
            lastDiscoveryT = t;
        }

        // 5) 每1000次动态更新 p(C)
        if (t % updateEvery == 0) {
            updateComboDistribution();
        }
    }

    private double epsilon1(long t) {
        double decayed = epsInit * Math.exp(-(double) t / tau1);
        return Math.max(epsMin, decayed);
    }

    private ParamCombo chooseCombo(double eps1) {
        double r = ThreadLocalRandom.current().nextDouble();
        if (r < eps1) {
            // Uniform(C^(k))：这里用 allCombos 近似（你可替换成“2-wise生成器输出集合”）
            return allCombos.get(ThreadLocalRandom.current().nextInt(allCombos.size()));
        }
        return comboSampler.sample();
    }

    private Feature chooseFeature(ParamCombo ct, double eps2) {
        double r = ThreadLocalRandom.current().nextDouble();
        if (r < eps2) {
            return featureSpace.uniformSample();
        }
        // p(f_k | c_t)
        return featureSpace.sampleConditional(ct);
    }

    private boolean executeWithTimeSlicing(ParamCombo ct, Feature ft) {
        // 基线覆盖率快照
        monitor.refreshBuffer();
        byte[] before = Arrays.copyOf(monitor.getCoverageBuf(), AFLMonitor.AFL_MAP_SIZE);

        // 第一次 1 分钟
        RunResult r1 = runForDuration(ct, ft, sliceMs);

        // 计算 new_edges
        monitor.refreshBuffer();
        byte[] after1 = Arrays.copyOf(monitor.getCoverageBuf(), AFLMonitor.AFL_MAP_SIZE);
        int newEdges1 = CoverageDiff.countNewEdges(before, after1);

        recordStats(ct, newEdges1, r1.execCount);

        if (newEdges1 > 0) {
            // 发现新边则延长 1 分钟（方案要求）
            RunResult r2 = runForDuration(ct, ft, sliceMs);

            monitor.refreshBuffer();
            byte[] after2 = Arrays.copyOf(monitor.getCoverageBuf(), AFLMonitor.AFL_MAP_SIZE);
            int newEdges2 = CoverageDiff.countNewEdges(after1, after2);

            recordStats(ct, newEdges2, r2.execCount);
            return (newEdges1 + newEdges2) > 0;
        }

        // 未发现新边：对参数值进行变异，再测试 1 分钟
        ParamCombo mutated = ct.mutateValues();
        RunResult r3 = runForDuration(mutated, ft, sliceMs);

        monitor.refreshBuffer();
        byte[] after3 = Arrays.copyOf(monitor.getCoverageBuf(), AFLMonitor.AFL_MAP_SIZE);
        int newEdges3 = CoverageDiff.countNewEdges(after1, after3); // 与after1比较即可

        // 变异也要把统计归到“原组合ct”或“变异组合mutated”，这里按你的定义选其一
        recordStats(ct, newEdges3, r3.execCount);

        return newEdges3 > 0;
    }

    private void recordStats(ParamCombo ct, int newEdges, long execCount) {
        ComboStats s = stats.get(ct);
        s.totalExec += execCount;
        s.newEdges += newEdges;
        if (newEdges > 0) {
            s.lastDiscoveryT = t;
        }
    }

    private void updateComboDistribution() {
        for (ParamCombo c : allCombos) {
            ComboStats s = stats.get(c);

            // R(C_t,t) = new_edges/ (total_exec+1) * exp(-lambda * Δt)
            long deltaT = Math.max(0, t - (s.lastDiscoveryT > 0 ? s.lastDiscoveryT : lastDiscoveryT));
            double timeFactor = Math.exp(-lambda * (double) deltaT);
            double r = ((double) s.newEdges) / ((double) s.totalExec + 1.0) * timeFactor;

            // w_dynamic = w_static * (1 + beta * R)
            double wStatic = comboSampler.getBaseWeightOrOne(c);
            double wDynamic = wStatic * (1.0 + beta * r);

            comboSampler.setWeight(c, Math.max(1e-12, wDynamic));
        }
        comboSampler.rebuild();
    }

    private RunResult runForDuration(ParamCombo ct, Feature ft, long durationMs) {
        long deadline = System.currentTimeMillis() + durationMs;
        long exec = 0;

        while (System.currentTimeMillis() < deadline) {
            // 4) 测试样例生成：依据参数与特性权重生成
            // 5) 执行与验证：提交给DBMS，并通过Oracle验证
            // 注意：这里留空给你接 Sqlancer 的实际执行入口
            runOneTestCase(ct, ft);
            exec++;
        }
        return new RunResult(exec);
    }

    private void runOneTestCase(ParamCombo ct, Feature ft) {
        // TODO: 在这里把 ct 的参数写入 DB（SET GLOBAL/SESSION 等），
        // 再按 ft 选择对应 SQL 生成器权重/模板，生成 SQL，执行并跑 oracle 校验。
        // 该函数应确保异常被捕获，避免打断主循环。
        try {
            // placeholder
        } catch (Throwable ignored) {
        }
    }

    private static final class RunResult {
        final long execCount;

        RunResult(long execCount) {
            this.execCount = execCount;
        }
    }

    private static final class ComboStats {
        long totalExec = 0;
        long newEdges = 0;
        long lastDiscoveryT = 0;
    }
}
