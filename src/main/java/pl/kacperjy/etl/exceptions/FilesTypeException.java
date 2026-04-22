package pl.kacperjy.etl.exceptions;

public class FilesTypeException extends RuntimeException{

    public FilesTypeException(String message) {
        super(message);
    }

    public FilesTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
