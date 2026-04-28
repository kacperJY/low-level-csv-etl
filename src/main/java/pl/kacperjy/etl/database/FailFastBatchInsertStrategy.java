package pl.kacperjy.etl.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.exceptions.DatabaseException;
import pl.kacperjy.etl.model.SQLStateError;
import pl.kacperjy.etl.model.Schema;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class FailFastBatchInsertStrategy implements BatchInsertStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FailFastBatchInsertStrategy.class);

    @Override
    public void execute(Connection connection, String sql, List<String[]> batchParameters, Schema schema) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(sql);) {

            Collection<String[]> rowsCollection = null;

            int conflictKey = -1;
            String conflictTargetColumnName = schema.conflictTargets().getFirst();
            for (int i = 0; i < schema.columnDefList().size(); i++) {
                if(schema.columnDefList().get(i).name().equals(conflictTargetColumnName)){
                    conflictKey = i;
                    break;
                }
            }

            // SORT by ConflictTarget - eliminate DEADLOCK
            // EXAMPLE
            // THREAD1 -> ROW_A, ROW_B, ROW_A -> Block A then B ...
            // THREAD2 -> ROW_B, ROW_A, ROW_B -> Block B then A ... DEAD LOCK
            if(conflictKey != -1){
                Map<String, String[]> uniqueRowsMap = new TreeMap<>(String::compareTo);
                for (String[] row : batchParameters) {
                    uniqueRowsMap.put(row[conflictKey],row);
                }
                rowsCollection = uniqueRowsMap.values();
            } else {
                rowsCollection = batchParameters;
            }

            // Map will remove duplicate - that resolve mulit queries UPDATE same single row in the same batch - by filtering duplicate on conflictTargetColumn


            // MAIN LOOP
            for (String[] batchParameterArray : rowsCollection) {
                for (int i = 0; i < schema.columnDefList().size(); i++) {
                    pst.setObject(i + 1, batchParameterArray[i]);
                }
                // ADD BATCH : After set all parameter for single query update
                pst.addBatch();
            }
            pst.executeBatch();
            connection.commit();
        } catch (BatchUpdateException primaryEx) {
            safeRollback(connection, primaryEx);

            String sqlState = primaryEx.getSQLState();
            String friendlyMessage = null;

            for (SQLStateError value : SQLStateError.values())
                if (sqlState.equals(value.getSqlState())) friendlyMessage = value.getDescription();

            if (friendlyMessage == null)
                friendlyMessage = "FAIL-FAST STRATEGY: Critical error during load batch to database.";

            logger.error("""
                    FAIL-FAST STRATEGY: Critical error during load batch to database.
                    DESCRIPTION: {}
                    REJECT REASON: {}
                    """, friendlyMessage, primaryEx.getMessage());

            throw new DatabaseException(friendlyMessage, primaryEx);
        } catch (SQLException primaryEx) {
            // Fail_fast for other problems ex. lost connection
            safeRollback(connection, primaryEx);
            throw new DatabaseException("CRITICAL database error. Check connection do database. SQLState=" + primaryEx.getSQLState(), primaryEx);
        }
    }

    private void safeRollback(Connection connection, SQLException originalEx) {
        try {
            connection.rollback();
        } catch (SQLException rollbackEx) {
            originalEx.addSuppressed(rollbackEx);
        }
    }
}
