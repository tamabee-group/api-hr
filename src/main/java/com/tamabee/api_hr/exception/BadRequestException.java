package com.tamabee.api_hr.exception;

import com.tamabee.api_hr.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Exception cho lỗi 400 Bad Request
 * Sử dụng khi request không hợp lệ hoặc dữ liệu không đúng
 */
public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST);
    }

    public BadRequestException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }

    public BadRequestException(String message, ErrorCode errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }

    public BadRequestException(ErrorCode errorCode) {
        super(HttpStatus.BAD_REQUEST, errorCode);
    }

    /**
     * Factory method cho lỗi role không hợp lệ
     */
    public static BadRequestException invalidRole(String role) {
        return new BadRequestException("Role không hợp lệ: " + role, ErrorCode.INVALID_ROLE);
    }
}
