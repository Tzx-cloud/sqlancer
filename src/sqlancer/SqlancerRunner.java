package sqlancer;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
        lastNrQueries = currentNrQueries;
        timeMillis = System.currentTimeMillis();
        return status;
    }

    public Map<String[], Double> getParamWeight(){
        Map<String[], Double> weights = new HashMap<>();
        for (Map.Entry<Set<BaseConfigurationGenerator.ConfigurationAction>, Double> parameter :proParameterCombos.entrySet()){
            String[] params = parameter.getKey().stream().map(BaseConfigurationGenerator.ConfigurationAction::getName).toArray(String[]::new);
            weights.put(params, parameter.getValue());
        }
        return weights ;
    }

//    public java.util.Map<String, Object> getTestcaseStatus() {
////        java.util.Map<String, Object> status = new java.util.HashMap<>();
////        status.put("total_testcases", Main.testcaseSet.size());
////        status.put("testcase_details", Main.testcaseSet);
////        return status;
//    }

}
