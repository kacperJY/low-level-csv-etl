package pl.kacperjy.etl.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final AppConfig appConfig;

    public Application(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void start(){

    }
}
