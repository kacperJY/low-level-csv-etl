package pl.kacperjy.etl.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.exceptions.FilesTypeException;
import pl.kacperjy.etl.model.Schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JSONSchemaReader {

    private static final Logger logger = LoggerFactory.getLogger(JSONSchemaReader.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Schema read(Path path) throws IOException{
        if(!path.getFileName().toString().endsWith(".json"))
            throw new FilesTypeException("Wrong file type. Expected json file type. Provided: " + path.getFileName());

        try(
                var br = Files.newBufferedReader(path)
                ){
            return OBJECT_MAPPER.readValue(br, Schema.class);
        } catch (IOException e){
            logger.error("### Cannot read or parse json file : Path = {}",path,e);
            throw new IOException("Cannot read or parse json file : Path = " + path,e);
        }
    }
}
