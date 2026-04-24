package pl.kacperjy.etl.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.app.AppConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class SchemaFilesManager {

    private static final Logger logger = LoggerFactory.getLogger(SchemaFilesManager.class);

    public static boolean verifySchemaDirectoryExists(AppConfig appConfig) {
        return Files.exists(Path.of(appConfig.directorySchemasPath()));
    }

    public static List<Path> loadSchemasFromDirectory(AppConfig appConfig) throws IOException {
        Path schmeasDirectoryPath = Path.of(appConfig.directorySchemasPath());
        try (
                Stream<Path> pathStream = Files.list(schmeasDirectoryPath); // FLAT Searching
        ) {
            return pathStream.toList();
        } catch (IOException e) {
            throw new IOException("Cannot read schema files from schemas directory :: Path = " + schmeasDirectoryPath, e);
        }
    }

}
