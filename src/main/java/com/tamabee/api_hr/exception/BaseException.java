package com.tamabee.api_hr.exception;

import com.tamabee.api_hr.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception cho toàn hệ thống
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected BaseException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    protected BaseException(String message, HttpStatus status, ErrorCode errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode.getCode();
    }

    protected BaseException(HttpStatus status, ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = status;
        this.errorCode = errorCode.getCode();
    }

    protected BaseException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    protected BaseException(String message, HttpStatus status, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode.getCode();
    }
}
