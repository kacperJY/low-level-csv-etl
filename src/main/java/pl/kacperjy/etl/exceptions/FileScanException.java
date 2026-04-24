package pl.kacperjy.etl.exceptions;

public class FileScanException extends RuntimeException{

    public FileScanException(String message) {
        super(message);
    }

    public FileScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
