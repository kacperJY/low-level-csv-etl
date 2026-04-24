package pl.kacperjy.etl.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.database.DatabaseManager;
import pl.kacperjy.etl.database.SchemaDAO;
import pl.kacperjy.etl.exceptions.SQLCreateSchemaException;
import pl.kacperjy.etl.io.JSONSchemaReader;
import pl.kacperjy.etl.io.SchemaFilesManager;
import pl.kacperjy.etl.model.Schema;
import pl.kacperjy.etl.utils.ConsoleReader;
import pl.kacperjy.etl.utils.Printer;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final DataSource dataSource;
    private final AppConfig appConfig;

    // DAO
    private final SchemaDAO schemaDAO;

    private final List<Schema> schemaList = new ArrayList<>();

    // UTILS
    private final Printer printer;
    private final ConsoleReader consoleReader;

    public Application(DataSource dataSource, AppConfig appConfig, Printer printer) {
        this.dataSource = dataSource;
        this.appConfig = appConfig;

        // DAO
        schemaDAO = new SchemaDAO(dataSource);

        // UTILS
        this.printer = printer;
        this.consoleReader = new ConsoleReader(printer);
    }

    public void start() {
        initializeSchemas();

        Option option;
        do {
            printer.printMenu(Option.values());

            option = Option.getOptionFromIndex(consoleReader.getInt());
            if (option == null) {
                printer.printLine("There is no such option in menu. Try again");
                continue;
            }

            switch (option) {
                case EXIT -> {
                    exit();
                }
                case SHOW_SCHEMAS -> {
                    showSchemasList();
                }
                case PERSIST_SCHEMAS -> {
                    persistSchemas();
                }
                case LOAD_DATA -> {
                }
            }
        } while (option != Option.EXIT);
    }

    private void showSchemasList() {
        printer.printHeader("SCHEMA LIST");
        schemaList.forEach(schema -> printer.printLine(schema.tableName()));
    }

    private void initializeSchemas() {
        try {
            List<Path> paths = SchemaFilesManager.loadSchemasFromDirectory(appConfig);

            for (Path path : paths) {
                try {
                    Schema schema = JSONSchemaReader.read(path);
                    schemaList.add(schema);
                } catch (IOException e) {
                    printer.printLine("Cannot read schema from file: Path = " + path + ". This file will be ignored");
                    logger.error(e.getMessage(), e);
                }
            }

        } catch (IOException e) {
            printer.printErrorMessage("Schemas folder doesn't exists or no permissions to read files in schemas folder : Path = " + appConfig.directorySchemasPath());
        }
    }

    private void persistSchemas() {
        for (Schema schema : schemaList) {
            try {
                schemaDAO.create(schema);
                printer.printLine("Schema : %s has been successfully saved to database".formatted(schema.tableName()));
            } catch (SQLCreateSchemaException e) {
                logger.error(e.getMessage(), e);
                printer.printErrorMessage("Cannot persist schema: %s. Probably schema is already in database. This schema will be ignored.".formatted(schema.tableName()));
            }
        }
    }

    private void exit() {
        try {
            DatabaseManager.shutdown();
            printer.printLine("Closing application...");
        } catch (Exception e) {
            printer.printErrorMessage("Unexpected error during closing application");
            logger.error("### CRITICAL ERROR : Cannot close database connection.", e);
        }
    }

    private static enum Option {
        EXIT(0, "Exit"),
        SHOW_SCHEMAS(1, "Show correctly loaded schemas"),
        PERSIST_SCHEMAS(2, "Persist schemas"),
        LOAD_DATA(3, "Load csv files");

        private final int optionIndex;
        private final String description;

        Option(int optionIndex, String description) {
            this.optionIndex = optionIndex;
            this.description = description;
        }

        @Override
        public String toString() {
            return optionIndex + " - " + description;
        }

        public int getOptionIndex() {
            return optionIndex;
        }

        public String getDescription() {
            return description;
        }

        public static Option getOptionFromIndex(int index) {
            for (Option value : Option.values()) {
                if (value.optionIndex == index)
                    return value;
            }
            return null;
        }
    }

}
