package pl.kacperjy.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.app.AppConfig;
import pl.kacperjy.etl.app.Application;
import pl.kacperjy.etl.database.DatabaseManager;
import pl.kacperjy.etl.exceptions.ConfigurationException;
import pl.kacperjy.etl.io.ConfigFileManager;
import pl.kacperjy.etl.io.SchemaFilesManager;
import pl.kacperjy.etl.utils.Printer;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.List;
import java.util.Properties;

class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static void main(String[] args) {
        Printer printer = new Printer();

        AppConfig appConfig = loadAppConfig(printer);

        DataSource dataSource = null;

        // Connection test
        try {
            DatabaseManager.initializeDatasource(appConfig);
            dataSource = DatabaseManager.getDatasource();

            try (Connection connection = dataSource.getConnection()) {} // MANUAL TEST

        } catch (Exception e) {
            logger.error("Critical error: Application cannot connect to database: {}.", appConfig.dbUrl(), e);

            printer.printErrorMessage("Application cannot connect to database " + appConfig.dbUrl() + ". Check your configuration file");
            System.exit(1);
        }

        logger.info("Successfully connected to database: {}", appConfig.dbUrl());

        if(!SchemaFilesManager.verifySchemaDirectoryExists(appConfig)){
            logger.error("Schemas directory doesn't exists :: Path = {}", appConfig.directorySchemasPath());
            printer.printErrorMessage("Cannot find schemas folder in path: " + appConfig.directorySchemasPath() + ". Check if path is correct in configuration file");
            System.exit(1);
        }

        Application application = new Application(dataSource, appConfig,printer);
        application.start();
    }

    private static AppConfig loadAppConfig(Printer printer) {
        try {
            ConfigFileManager configFileManager = new ConfigFileManager();
            Properties properties = configFileManager.loadConfig();
            return new AppConfig(
                    properties.getProperty(ConfigFileManager.ConfigProperties.DB_URL.getPropertyName()),
                    properties.getProperty(ConfigFileManager.ConfigProperties.DB_USER.getPropertyName()),
                    properties.getProperty(ConfigFileManager.ConfigProperties.DB_PASSWORD.getPropertyName()),
                    properties.getProperty(ConfigFileManager.ConfigProperties.SCHEMAS_DIRECTORY_PATH.getPropertyName())
            );
        } catch (ConfigurationException | IOException e) {
            if (e instanceof ConfigurationException ce)
                logger.error(ce.getMessage());
            else
                logger.error("## Critical error of start application: {}", ((IOException) e).getMessage());
            printer.printErrorMessage("CONFIGURATION FILE PROBLEM ### There is no configuration file or configuration file contains default generated values");
            System.exit(1);
            return null; // Zadowala kompilator, proces i tak zginie linijkę wyżej
        }
    }
}
