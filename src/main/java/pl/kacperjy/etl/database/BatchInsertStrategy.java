package pl.kacperjy.etl.database;

import java.sql.Connection;
import java.util.List;

public interface BatchInsertStrategy {


    void execute(Connection connection, String sql, List<Object> batchParameters);
}
