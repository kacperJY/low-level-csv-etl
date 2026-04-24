package pl.kacperjy.etl.exceptions;

public class SQLCreateSchemaException extends RuntimeException{

    public SQLCreateSchemaException(String message) {
        super(message);
    }

    public SQLCreateSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
