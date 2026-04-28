package pl.kacperjy.etl.database;

import pl.kacperjy.etl.model.Schema;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface BatchInsertStrategy {


    void execute(Connection connection, String sql, List<String[]> batchParameters, Schema schema) throws SQLException;
}
