package pl.kacperjy.etl.exceptions;

public class WrongFormatFileException extends RuntimeException {
    public WrongFormatFileException(String message) {
        super(message);
    }

    public WrongFormatFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
