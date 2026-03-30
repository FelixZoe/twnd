package cc.felixzoe.exception;

import cc.felixzoe.constant.MessageConstant;

public class PasswordErrorException extends BaseException{
    public PasswordErrorException() {
    }
    public PasswordErrorException(String msg) {
        super(msg);
    }
}
