package com.quanlynhakhoa.shared.exception;

import java.util.Objects;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(Objects.requireNonNull(errorCode).getMessageKey());

        this.errorCode = errorCode;
        this.args = args == null
                ? new Object[0]
                : args.clone();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args.clone();
    }
}