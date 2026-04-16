package sqlancer;

import org.junit.jupiter.api.Test;
import java.lang.Thread;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestMySQLFuzzing {


    @Test
    public void testMySQLFuzzing() throws InterruptedException {
        String[] args={"mysql","--oracle","TLP_WHERE"};
//--dbms-path","/home/tzx/sqlite-src-3510200/sqlite3","--map-size","57366","mysql",
        Main.main(args);
        //Main.executeMainOnWeb(args);
//        SqlancerRunner runner = new SqlancerRunner();
//
//        runner.startFuzzing(args);
//
//        while(true){
//            Thread.sleep(1000);
//            Map<String, Object> testStatus = runner.getTestStatus();
//
//        }



    }


}
