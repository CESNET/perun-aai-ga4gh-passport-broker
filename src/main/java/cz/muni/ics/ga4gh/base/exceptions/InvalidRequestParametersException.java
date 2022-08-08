package cz.muni.ics.ga4gh.base.exceptions;

public class InvalidRequestParametersException extends Exception {

    public InvalidRequestParametersException() {
        super();
    }

    public InvalidRequestParametersException(String message) {
        super(message);
    }

    public InvalidRequestParametersException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidRequestParametersException(Throwable cause) {
        super(cause);
    }

    protected InvalidRequestParametersException(String message, Throwable cause, boolean enableSuppression,
                                                boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
