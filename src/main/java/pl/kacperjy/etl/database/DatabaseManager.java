package pl.kacperjy.etl.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pl.kacperjy.etl.app.AppConfig;

import javax.sql.DataSource;

public class DatabaseManager {

    private static HikariDataSource hikariDataSource;

    public static void initializeDatasource(AppConfig appConfig){
        if(hikariDataSource != null)
            throw new IllegalStateException("Cannot initialize more than one instance of DatabaseManager");

        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(appConfig.dbUrl());
        hikariConfig.setUsername(appConfig.dbUser());
        hikariConfig.setPassword(appConfig.dbPassword());

        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setAutoCommit(false);
        hikariConfig.setInitializationFailTimeout(200);
        hikariDataSource = new HikariDataSource(hikariConfig);
    }

    private DatabaseManager(){}


    public static DataSource getDatasource() {
        return hikariDataSource;
    }

    public static void shutdown(){
        hikariDataSource.close();
    }
}
