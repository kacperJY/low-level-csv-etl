package pl.kacperjy.etl.app;

public record AppConfig(
        String dbUrl,
        String dbUser,
        String dbPassword,
        String directorySchemasPath
) {}
