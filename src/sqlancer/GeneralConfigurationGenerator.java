package sqlancer;


import sqlancer.mysql.MySQLOptions;
import sqlancer.mysql.gen.MySQLSetGenerator;

public class GeneralConfigurationGenerator {


    public static sqlancer.BaseConfigurationGenerator createGenerator( Class<? extends DBMSSpecificOptions> dbmsOptionsClass, GlobalState globalState) {
        if (dbmsOptionsClass.equals(MySQLOptions.class)) {
            // 确保 getOptions() 返回的类型是 MySQLOptions
            // 这通常是安全的，因为 globalState 是与数据库类型对应的
            return MySQLSetGenerator.getInstance(globalState.getRandomly(),  globalState.getOptions());
        }
        // else if (dbmsOptionsClass.equals(PostgresOptions.class)) {
        //     return PostgresConfigurationGenerator.getInstance(globalState.getRandomly(), (PostgresOptions) globalState.getOptions());
        // }
        else {
            throw new IllegalArgumentException("Unsupported database type: " + dbmsOptionsClass.getName());
        }
    }

}
