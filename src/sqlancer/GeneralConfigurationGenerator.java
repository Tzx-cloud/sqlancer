package sqlancer;


import sqlancer.mariadb.MariaDBOptions;
import sqlancer.mariadb.gen.MariaDBSetGenerator;
import sqlancer.mysql.MySQLOptions;
import sqlancer.mysql.gen.MySQLSetGenerator;
import sqlancer.postgres.PostgresOptions;
import sqlancer.postgres.gen.PostgresSetGenerator;
import sqlancer.sqlite3.SQLite3Options;
import sqlancer.sqlite3.gen.SQLite3SetGenerator;

public class GeneralConfigurationGenerator {


    public static sqlancer.BaseConfigurationGenerator createGenerator( Class<? extends DBMSSpecificOptions> dbmsOptionsClass, GlobalState globalState) {
        if (dbmsOptionsClass.equals(MySQLOptions.class)) {
            // 确保 getOptions() 返回的类型是 MySQLOptions
            // 这通常是安全的，因为 globalState 是与数据库类型对应的
            return MySQLSetGenerator.getInstance(globalState.getRandomly(),  globalState.getOptions());
        }
        else if (dbmsOptionsClass.equals(PostgresOptions.class)) {
             return PostgresSetGenerator.getInstance(globalState.getRandomly(),  globalState.getOptions());
        }
        else if (dbmsOptionsClass.equals(MariaDBOptions.class)) {
            return MariaDBSetGenerator.getInstance(globalState.getRandomly(),  globalState.getOptions());
        }
        else if(dbmsOptionsClass.equals(SQLite3Options.class)) {
            return SQLite3SetGenerator.getInstance(globalState.getRandomly(),  globalState.getOptions());
        }
        else {
            throw new IllegalArgumentException("Unsupported database type: " + dbmsOptionsClass.getName());
        }
    }

}
