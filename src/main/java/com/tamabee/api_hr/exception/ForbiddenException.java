package com.tamabee.api_hr.exception;

import com.tamabee.api_hr.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Exception cho lỗi 403 Forbidden
 * Sử dụng khi user không có quyền truy cập resource
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
    }

    public ForbiddenException(String message, String errorCode) {
        super(message, HttpStatus.FORBIDDEN, errorCode);
    }

    public ForbiddenException(String message, ErrorCode errorCode) {
        super(message, HttpStatus.FORBIDDEN, errorCode);
    }

    public ForbiddenException(ErrorCode errorCode) {
        super(HttpStatus.FORBIDDEN, errorCode);
    }
}
