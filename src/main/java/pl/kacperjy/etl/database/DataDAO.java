package pl.kacperjy.etl.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.exceptions.DatabaseException;
import pl.kacperjy.etl.model.Schema;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class DataDAO {

    private static final Logger logger = LoggerFactory.getLogger(DataDAO.class);

    private final DataSource dataSource;

    public DataDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insert(BatchInsertStrategy batchInsertStrategy, List<String[]> rows, Schema schema) {
        try (
                Connection connection = dataSource.getConnection();
        ) {
            String sql = SQLBuilder.sqlInsert(schema);
            batchInsertStrategy.execute(connection, sql, rows, schema);
        } catch (DatabaseException e) {
            DatabaseManager.shutdown();
            throw new DatabaseException(e.getMessage(),e);
        } catch (SQLException e) {
            DatabaseManager.shutdown();
            throw new DatabaseException("CRITICAL database error - cannot connect to database");
        }
    }
}
