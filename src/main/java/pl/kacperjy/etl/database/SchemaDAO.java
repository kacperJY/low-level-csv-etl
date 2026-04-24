package pl.kacperjy.etl.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.exceptions.SQLCreateSchemaException;
import pl.kacperjy.etl.model.Schema;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public record SchemaDAO(DataSource dataSource) {
    private static final Logger logger = LoggerFactory.getLogger(SchemaDAO.class);

    public void create(Schema schema) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement prep = connection.prepareStatement(SQLBuilder.sqlCreate(schema));
        ) {
            try {
                prep.executeUpdate();
                connection.commit();
            } catch (SQLException e){
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new SQLCreateSchemaException("SQL EXCEPTION: Cannot execute CREATE schema for provided Schema object: %s. Provided schema probably is already in database"
                    .formatted(schema.tableName()), e);
        }
    }

}
