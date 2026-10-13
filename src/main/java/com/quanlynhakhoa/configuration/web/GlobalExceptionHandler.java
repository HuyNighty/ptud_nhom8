
package com.quanlynhakhoa.configuration.web;

import com.quanlynhakhoa.shared.exception.ErrorCode;
import com.quanlynhakhoa.shared.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {

        log.error("Unexpected system exception", ex);

        ErrorCode error = ErrorCode.UNCATEGORIZED_EXCEPTION;

        String message = messageSource.getMessage(
                error.getMessageKey(),
                null,
                "Hệ thống gặp lỗi không xác định.",
                LocaleContextHolder.getLocale()
        );

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(error.getErrorCode())
                .message(message)
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
