package cc.felixzoe.exception;

public class NotLoginException extends TokenException {

    public NotLoginException() {
    }

    public NotLoginException(String msg) {
        super(msg);
    }
}
