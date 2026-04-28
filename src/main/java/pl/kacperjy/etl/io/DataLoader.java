package pl.kacperjy.etl.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.database.DataDAO;
import pl.kacperjy.etl.database.FailFastBatchInsertStrategy;
import pl.kacperjy.etl.exceptions.DatabaseException;
import pl.kacperjy.etl.exceptions.FileScanException;
import pl.kacperjy.etl.model.Schema;
import pl.kacperjy.etl.utils.Printer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class DataLoader {

    private final Collection<Schema> schemaCollection;
    private final DataDAO dataDAO;

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final Semaphore semaphore = new Semaphore(15);

    private final Printer printer;

    public DataLoader(Collection<Schema> schemaCollection, DataDAO dataDAO, Printer printer) {
        this.schemaCollection = schemaCollection;
        this.dataDAO = dataDAO;
        this.printer = printer;
    }

    public void loadData() {
        var failFastStrategy = new FailFastBatchInsertStrategy();


        for (Schema schema : schemaCollection) {

            try (
                    StructuredTaskScope<Void, Void> scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())
            ) {
                DataFileScanner.scanAndPush(schema, dataBatch -> {

                    try {
                        semaphore.acquire();

                        scope.fork(() -> {
                            try {
                                List<String[]> batchRows = DataBatchParser.parseDataBatch(dataBatch);
                                dataDAO.insert(failFastStrategy, batchRows, schema);
                            } finally {
                                semaphore.release();
                            }
                        });
                    } catch (InterruptedException e) {
                        logger.error("Thread that was waiting for semaphore has been interrupted. SCHEMA: {}", schema.tableName());
                    }
                });

                try {
                    scope.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException("CRITICAL ERROR: Application has been terminated");
                }

                printer.printLine("SCHEMA: {%s} has been correctly saved in database".formatted(schema.tableName()));

            } catch (StructuredTaskScope.FailedException e) {
                Throwable caused = e.getCause();
                switch (caused){
                    case DatabaseException dbEx ->{
                        printer.printLine("--------------------------------------");
                        printer.printErrorMessage(dbEx.getMessage());
                        printer.printLine("\t", "Schema {%s} will not be loaded correctly. Check your CSV file or Schema configuration".formatted(schema.tableName()));
                        printer.printLine("--------------------------------------");
                    }
                    default -> throw new RuntimeException(caused.getMessage(),caused);
                }
            } catch (FileScanException e){
                printer.printLine(e.getMessage());
            }
        }

    }
}
