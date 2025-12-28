package sqlancer;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static sqlancer.BaseConfigurationGenerator.proParameterCombos;

@Service
public class SqlancerRunner {
    private ExecutorService executorService = null;
    private Future<?> sqlancerTaskFuture = null;
    private long timeMillis = System.currentTimeMillis();
    private long lastNrQueries;
    /**
     * 启动 SQLancer 模糊测试
     * @param option 要测试的数据库名称，例如 "mysql"
     */
    public void startFuzzing(String... option) {
        if (sqlancerTaskFuture != null && !sqlancerTaskFuture.isDone()) {
            System.out.println("SQLancer is already running.");
            return;
        }

        // 使用单线程的 ExecutorService 来运行后台任务
        executorService = Executors.newSingleThreadExecutor();

        sqlancerTaskFuture = executorService.submit(() -> {
            try {
                Main.executeMainOnWeb(option);
            }catch (Exception e) {
                System.err.println("An error occurred during SQLancer initialization or training.");
                e.printStackTrace();
            }

        });

    }

    /**
     * 停止 SQLancer 模糊测试
     */
    public void stopFuzzing() {
        if (executorService != null && !executorService.isShutdown()) {
            System.out.println("Attempting to stop SQLancer...");
            // 中断正在执行的任务
            executorService.shutdownNow();
            sqlancerTaskFuture = null;
            executorService = null;
        } else {
            System.out.println("SQLancer is not running.");
        }
    }

    /**
     * 获取 SQLancer 的内部执行状态
     * @return 包含状态信息的 Map
     */
    public java.util.Map<String, Object> getTestStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("task_status", sqlancerTaskFuture != null && !sqlancerTaskFuture.isDone());
        status.put("coverage", AFLMonitor.getInstance().getCoverageRate());

        long elapsedTimeMillis = System.currentTimeMillis() - timeMillis;
        long currentNrQueries = Main.nrQueries.get();
        long nrCurrentQueries = currentNrQueries - lastNrQueries;
        status.put("execution_count", currentNrQueries);
        status.put("current_param_combo",  BaseConfigurationGenerator.currentGeneratedActions);
        status.put("throughput",  (nrCurrentQueries / (elapsedTimeMillis / 1000d)));
        status.put("bug_count",  Main.bugs.get());
        lastNrQueries = currentNrQueries;
        timeMillis = System.currentTimeMillis();
        return status;
    }

    public Map<String[], Double> getParamWeight(){
        return proParameterCombos.entrySet().stream()
                // 按权重 (Map.Entry.getValue()) 降序排序
                .sorted(Map.Entry.<Set<BaseConfigurationGenerator.ConfigurationAction>, Double>comparingByValue().reversed())
                // 只取前 20 个
                .limit(20)
                // 收集到新的 Map 中
                .collect(Collectors.toMap(
                        // 将 Set<ConfigurationAction> 转换为 String[] 作为键
                        entry -> entry.getKey().stream()
                                .map(BaseConfigurationGenerator.ConfigurationAction::getName)
                                .toArray(String[]::new),
                        // 保留原始的权重作为值
                        Map.Entry::getValue,
                        // 合并函数（理论上不会出现键冲突）
                        (v1, v2) -> v1,
                        // 使用 LinkedHashMap 来保持排序后的顺序
                        LinkedHashMap::new
                ));
    }

//    public java.util.Map<String, Object> getTestcaseStatus() {
////        java.util.Map<String, Object> status = new java.util.HashMap<>();
////        status.put("total_testcases", Main.testcaseSet.size());
////        status.put("testcase_details", Main.testcaseSet);
////        return status;
//    }

}
