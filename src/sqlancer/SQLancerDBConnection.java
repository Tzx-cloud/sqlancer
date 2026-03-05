package sqlancer;

import java.sql.SQLException;

public interface SQLancerDBConnection extends AutoCloseable {

    String getDatabaseVersion() throws Exception;
    boolean isValid() throws SQLException ;
}
