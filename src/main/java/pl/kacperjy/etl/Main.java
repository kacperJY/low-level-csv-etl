package pl.kacperjy.etl;

import pl.kacperjy.etl.app.AppConfig;
import pl.kacperjy.etl.app.Application;
import pl.kacperjy.etl.exceptions.ConfigurationException;
import pl.kacperjy.etl.io.ConfigManager;

import java.io.IOException;
import java.util.Properties;

class Main {

    static void main(String[] args) {

        AppConfig appConfig = loadAppConfig();
        Application application = new Application(appConfig);
        application.start();
    }

    private static AppConfig loadAppConfig(){
        try {
            ConfigManager configManager = new ConfigManager();
            Properties properties = configManager.loadConfig();
            return new AppConfig(
                    properties.getProperty(ConfigManager.ConfigProperties.DB_URL.getPropertyName()),
                    properties.getProperty(ConfigManager.ConfigProperties.DB_USER.getPropertyName()),
                    properties.getProperty(ConfigManager.ConfigProperties.DB_PASSWORD.getPropertyName()),
                    properties.getProperty(ConfigManager.ConfigProperties.SCHEMAS_DIRECTORY_PATH.getPropertyName())
            );
        } catch (ConfigurationException | IOException e) {
            if(e instanceof ConfigurationException)
                System.err.println(e.getMessage());
            else
                System.err.println("## Błąd krytyczny startu aplikacji: " + ((IOException) e).getMessage());

            System.exit(1);
            return null; // Zadowala kompilator, proces i tak zginie linijkę wyżej
        }
    }
}
