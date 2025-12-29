package sqlancer;

import org.junit.jupiter.api.Test;
import java.lang.Thread;

import java.util.Map;

public class TestMySQLFuzzing {


    @Test
    public void testMySQLFuzzing() throws InterruptedException {
        String[] args={"mysql","--oracle","TLP_WHERE"};
        //Main.main(args);
        Main.executeMainOnWeb(args);
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
