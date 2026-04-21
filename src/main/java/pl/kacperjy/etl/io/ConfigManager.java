package pl.kacperjy.etl.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.exceptions.ConfigurationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigManager {

    private static final Path APP_CONFIG_PATH = Path.of("etl-config.properties");

    private static final String DEFAULT_CONFIGURATION_PROPERTY_VALUE = "##DEFAULT";

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    public Properties loadConfig() throws IOException {
        if (!Files.exists(APP_CONFIG_PATH)) {
            generateDefaultConfiguration();
            throw new ConfigurationException("Cannot connect to database with default generated configuration");
        }

        Properties propertiesResult = readPropertiesFromFile();
        validateConfig(propertiesResult);
        return propertiesResult;
    }

    private Properties readPropertiesFromFile() throws IOException{
        Properties properties = new Properties();
        try (
                BufferedReader bufferedReader = Files.newBufferedReader(APP_CONFIG_PATH)
        ) {
            properties.load(bufferedReader);
            return properties;
        } catch (IOException e) {
            logger.error("Cannot read configuration file: {}", APP_CONFIG_PATH, e);
            throw new ConfigurationException("Cannot read configuration file: " + APP_CONFIG_PATH, e);
        }
    }

    private void validateConfig(Properties properties){
        for (ConfigProperties propertyName : ConfigProperties.values()) {
            String propertyValue = properties.getProperty(propertyName.getPropertyName());
            if(propertyValue == null || propertyValue.isBlank() || propertyValue.equals(DEFAULT_CONFIGURATION_PROPERTY_VALUE)){
                String message = String.format("Cannot connect with parameter {%s} set as default value",propertyName.getPropertyName());
                throw new ConfigurationException(message);
            }
        }
    }

    private void generateDefaultConfiguration() throws IOException{
        Properties defaultProperties = new Properties();

        // Set default values
        for (ConfigProperties propertyName : ConfigProperties.values()) {
            defaultProperties.setProperty(propertyName.getPropertyName(), DEFAULT_CONFIGURATION_PROPERTY_VALUE);
        }

        try (
                BufferedWriter bufferedWriter = Files.newBufferedWriter(APP_CONFIG_PATH)
        ) {
            defaultProperties.store(bufferedWriter, "### Default generated configuration ###");
            logger.info("There is no configuration file. Default configuration has been generated, it has to be changed from default settings: {}", APP_CONFIG_PATH);
        } catch (IOException e) {
            logger.error("Cannot create new configuration file: {}", APP_CONFIG_PATH, e);
            throw new ConfigurationException("No permission to create new configuration file: " + APP_CONFIG_PATH, e);
        }
    }

    private enum ConfigProperties {
        DB_URL("db.url"),
        DB_USER("db.user"),
        DB_PASSWORD("db.password"),
        SCHEMAS_CONFIG("schemas.directory.path");

        private final String propertyName;

        ConfigProperties(String propertyName) {
            this.propertyName = propertyName;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }

}
