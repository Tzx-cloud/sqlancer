package sqlancer;

import org.junit.jupiter.api.Test;

public class TestMySQLFuzzing {


    @Test
    public void testMySQLFuzzing() {
        String[] args={"--use-reducer","mysql","--oracle","TLP_WHERE"};
        Main.main(args);

    }
}
