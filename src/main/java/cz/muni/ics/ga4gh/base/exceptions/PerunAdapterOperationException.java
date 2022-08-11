package cz.muni.ics.ga4gh.base.exceptions;

public class PerunAdapterOperationException extends RuntimeException {

    public PerunAdapterOperationException() {
        super();
    }

    public PerunAdapterOperationException(String message) {
        super(message);
    }

    public PerunAdapterOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PerunAdapterOperationException(Throwable cause) {
        super(cause);
    }

    protected PerunAdapterOperationException(String message, Throwable cause,
                                             boolean enableSuppression,
                                             boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
