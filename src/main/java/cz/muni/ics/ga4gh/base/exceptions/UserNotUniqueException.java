package cz.muni.ics.ga4gh.base.exceptions;

public class UserNotUniqueException extends Exception {

    public UserNotUniqueException() {
        super();
    }

    public UserNotUniqueException(String s) {
        super(s);
    }

    public UserNotUniqueException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public UserNotUniqueException(Throwable throwable) {
        super(throwable);
    }

    protected UserNotUniqueException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
