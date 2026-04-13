package sqlancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sqlancer.mysql.gen.MySQLSetGenerator;
import sqlancer.postgres.gen.PostgresSetGenerator;
import sqlancer.sqlite3.gen.SQLite3SetGenerator;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static sqlancer.BaseConfigurationGenerator.proParameterCombos;

@Service
public class SqlancerRunner {
    // 新增日志，便于定位问题
    private static final Logger log = LoggerFactory.getLogger(SqlancerRunner.class);

    private ExecutorService executorService = null;
    private Future<?> sqlancerTaskFuture = null;
    private long timeMillis = System.currentTimeMillis();
    private long lastNrQueries = 0;

    // 新增：权重文件目录（从外部传入）
    private String weightsDir;
    // 新增：目标数据库类型（mysql/postgres/sqlite3）
    private String targetDbms = "mysql";

    /**
     * 修复1：支持接收启动参数（包含权重目录、数据库类型等）
     * @param args SQLancer启动参数（如 --host localhost --port 3306 --weights-dir /xxx）
     */
    public void startFuzzing(String... args) {
        // 解析参数：提取 --weights-dir 和数据库类型
        parseArgs(args);
        String[] sanitizedArgs = sanitizeArgsForMain(args);

        System.out.println("===== 开始启动SQLancer =====");
        log.info("✅ 解析到权重目录：{}，目标数据库：{}", weightsDir, targetDbms);

        // 修复2：统一使用 executorService，确保能被 stopFuzzing 停止
        executorService = Executors.newSingleThreadExecutor();
        sqlancerTaskFuture = executorService.submit(() -> {
            try {
                // 强制加载权重文件：根据目标数据库类型创建具体 Generator 实例
                BaseConfigurationGenerator configGenerator = createConfigGenerator(targetDbms);
                configGenerator.loadWeightsFromFile(targetDbms);

                // 验证加载结果
                int comboSize = BaseConfigurationGenerator.allParameterCombos.size();
                System.out.println("参数组合总数: " + comboSize);
                log.info("✅ 权重文件加载完成，参数组合总数：{}", comboSize);

                if (comboSize == 0) {
                    System.err.println("❌ 权重文件加载后仍为空！请检查文件路径或格式");
                    log.error("权重文件加载后为空，数据库类型：{}", targetDbms);
                    return;
                }

                // 执行SQLancer主逻辑（传递启动参数）
                Main.executeMainOnWeb(sanitizedArgs);
            } catch (Exception e) {
                System.err.println("❌ SQLancer执行异常: " + e.getMessage());
                log.error("SQLancer执行异常", e);
                e.printStackTrace();
            } finally {
                if (executorService != null && !executorService.isShutdown()) {
                    executorService.shutdown();
                }
            }
        });
    }

    /**
     * 兼容旧方法：无参数启动（默认MySQL）
     */
    public void startFuzzing() {
        // 默认权重目录（与TestStatusService配置一致）
        this.weightsDir = "/home/sqlfuzzer/Fuzz20260315/Fuzz20260312/BackEnd/src/main/java/sqlancer";
        this.targetDbms = "mysql";
        startFuzzing(new String[]{});
    }

    /**
     * 修复3：停止测试逻辑（移除不存在的 stopFlag）
     */
    public void stopFuzzing() {
        if (executorService != null && !executorService.isShutdown()) {
            System.out.println("Attempting to stop SQLancer...");
            log.info("开始停止SQLancer测试");

            // 1. 中断执行器（移除 stopFlag 引用）
            executorService.shutdownNow();
            // 2. 重置状态
            sqlancerTaskFuture = null;
            executorService = null;
            lastNrQueries = 0;
            timeMillis = System.currentTimeMillis();

            log.info("✅ SQLancer已停止");
        } else {
            System.out.println("SQLancer is not running.");
            log.warn("SQLancer未运行，无需停止");
        }
    }

    /**
     * 修复4：状态统计（移除不存在的 stopFlag/coverageEdges 引用）
     */
    public Map<String, Object> getTestStatus() {
        Map<String, Object> status = new HashMap<>();
        // 修复：正确判断测试状态（移除 stopFlag）
        boolean isRunning = sqlancerTaskFuture != null && !sqlancerTaskFuture.isDone();
        status.put("task_status", isRunning);

        // 覆盖率计算（兼容非Linux环境）


        Integer coverageRate = AFLMonitor.getInstance().getCoverageEdges();

        status.put("coverage", coverageRate);

        // 修复：执行数/BUG数获取（空值保护）
        long currentNrQueries = Main.nrQueries != null ? Main.nrQueries.get() : 0;
        long bugCount = Main.bugs != null ? Main.bugs.get() : 0;

        // 修复：吞吐量计算（避免除以0）
        long elapsedTimeMillis = System.currentTimeMillis() - timeMillis;
        long nrCurrentQueries = currentNrQueries - lastNrQueries;
        double throughput = elapsedTimeMillis > 0 ? (nrCurrentQueries / (elapsedTimeMillis / 1000d)) : 0.0;
        // 保留2位小数，避免前端显示过多位数
        throughput = Math.round(throughput * 100.0) / 100.0;

        // 修复：参数组合显示（空值保护）
        Collection<?> comboObj = BaseConfigurationGenerator.currentGeneratedActions;
        String currentCombo;
        if (comboObj == null) {
            currentCombo = "无";
        } else {
            currentCombo = comboObj.isEmpty() ? "无" : comboObj.stream()
                                                       .map(Object::toString)
                                                       .collect(Collectors.joining(", "));
        }
        if ("无".equals(currentCombo) && isRunning) {
            currentCombo = "生成中";
        }

        // 填充状态（所有数值统一为基础类型，避免前端解析错误）
        status.put("execution_count", currentNrQueries);
        status.put("current_param_combo", currentCombo);
        status.put("throughput", throughput);
        status.put("bug_count", bugCount);

        // 更新上次统计值
        lastNrQueries = currentNrQueries;
        timeMillis = System.currentTimeMillis();

        // 调试日志：打印实时状态
        log.debug("SQLancer实时状态 - 执行数：{}，BUG数：{}，吞吐量：{}，覆盖率：{}%，当前组合：{}",
                currentNrQueries, bugCount, throughput, coverageRate, currentCombo);

        return status;
    }

    /**
     * 修复5：参数权重获取（空值保护）
     */
    public Map<String[], Double> getParamWeight() {
        // 空值保护：避免proParameterCombos为空时报错
        if (proParameterCombos == null || proParameterCombos.isEmpty()) {
            log.warn("参数权重集合为空，返回空Map");
            return new LinkedHashMap<>();
        }

        return proParameterCombos.entrySet().stream()
                .sorted(Map.Entry.<Set<BaseConfigurationGenerator.ConfigurationAction>, Double>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toMap(
                        (Map.Entry<Set<BaseConfigurationGenerator.ConfigurationAction>, Double> entry) ->
                                entry.getKey().stream()
                                        .map(BaseConfigurationGenerator.ConfigurationAction::getName)
                                        .toArray(String[]::new),
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    // ==================== 私有工具方法 ====================
    /**
     * 解析启动参数，提取权重目录、数据库类型等
     */
    private void parseArgs(String... args) {
        if (args == null || args.length == 0) {
            // 默认值
            this.weightsDir = "/home/sqlfuzzer/Fuzz20260315/Fuzz20260312/BackEnd/src/main/java/sqlancer";
            this.targetDbms = "mysql";
            return;
        }

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--weights-dir":
                    if (i + 1 < args.length) {
                        this.weightsDir = args[i + 1];
                        i++; // 跳过参数值
                    }
                    break;
                case "mysql":
                case "postgres":
                case "sqlite3":
                    this.targetDbms = args[i];
                    break;
                default:
                    // 忽略其他参数（如--host/--port等）
                    break;
            }
        }
    }

    /**
     * 过滤掉仅供 Runner 使用、Main 不识别的参数，避免 JCommander 抛错。
     */
    private String[] sanitizeArgsForMain(String... args) {
        if (args == null || args.length == 0) {
            return new String[0];
        }
        List<String> filtered = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--weights-dir".equals(arg)) {
                // 跳过该参数及其值（如果存在）
                if (i + 1 < args.length) {
                    i++;
                }
                continue;
            }
            filtered.add(arg);
        }
        return filtered.toArray(new String[0]);
    }

    /**
     * 根据数据库类型创建具体的 ConfigurationGenerator 实例（用于加载权重文件）
     */
    private BaseConfigurationGenerator createConfigGenerator(String dbms) {
        Randomly r = new Randomly();
        MainOptions options = MainOptions.DEFAULT_OPTIONS;
        return switch (dbms.toLowerCase()) {
            case "postgres" -> PostgresSetGenerator.getInstance(r, options);
            case "sqlite3" -> new SQLite3SetGenerator(r, options);
            default -> new MySQLSetGenerator(r, options); // mysql 及默认
        };
    }


}