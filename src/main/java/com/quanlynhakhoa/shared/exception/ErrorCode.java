package com.quanlynhakhoa.shared.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    ACCOUNT_INVALID_STATE("error.account.invalid_state", 1101, ErrorHttpStatus.CONFLICT),

    UNCATEGORIZED_EXCEPTION("error.uncategorized", 9999, ErrorHttpStatus.INTERNAL_SERVER_ERROR)
    ;

    private final String messageKey;
    private final int errorCode;
    private final ErrorHttpStatus httpStatus;

    ErrorCode(String messageKey, int errorCode, ErrorHttpStatus httpStatus) {
        this.messageKey = messageKey;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
