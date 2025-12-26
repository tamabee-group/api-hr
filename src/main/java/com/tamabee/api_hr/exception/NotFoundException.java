package com.tamabee.api_hr.exception;

import com.tamabee.api_hr.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Exception cho lỗi 404 Not Found
 * Sử dụng khi không tìm thấy resource
 */
public class NotFoundException extends BaseException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND);
    }

    public NotFoundException(String message, String errorCode) {
        super(message, HttpStatus.NOT_FOUND, errorCode);
    }

    public NotFoundException(String message, ErrorCode errorCode) {
        super(message, HttpStatus.NOT_FOUND, errorCode);
    }

    public NotFoundException(ErrorCode errorCode) {
        super(HttpStatus.NOT_FOUND, errorCode);
    }

    public static NotFoundException user(Long id) {
        return new NotFoundException("Không tìm thấy user với id: " + id, ErrorCode.USER_NOT_FOUND);
    }

    public static NotFoundException user(String email) {
        return new NotFoundException("Không tìm thấy user với email: " + email, ErrorCode.USER_NOT_FOUND);
    }

    public static NotFoundException company(Long id) {
        return new NotFoundException("Không tìm thấy công ty với id: " + id, ErrorCode.COMPANY_NOT_FOUND);
    }

    public static NotFoundException email(String email) {
        return new NotFoundException("Email không tồn tại trong hệ thống: " + email, ErrorCode.EMAIL_NOT_FOUND);
    }
}
