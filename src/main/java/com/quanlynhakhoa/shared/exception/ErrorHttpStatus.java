package com.quanlynhakhoa.shared.exception;

public enum ErrorHttpStatus {

    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    GONE(410),
    INTERNAL_SERVER_ERROR(500);

    private final int value;

    ErrorHttpStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
