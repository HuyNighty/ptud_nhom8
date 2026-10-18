package com.quanlynhakhoa.features.examination.service;

public final class DichVuException extends RuntimeException {

    public enum Code {
        INVALID_DATA,
        CODE_ALREADY_EXISTS,
        NAME_ALREADY_EXISTS,
        NOT_FOUND
    }

    private final Code code;

    public DichVuException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
