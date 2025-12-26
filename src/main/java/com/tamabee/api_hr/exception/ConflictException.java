package com.tamabee.api_hr.exception;

import com.tamabee.api_hr.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Exception cho lỗi 409 Conflict
 * Sử dụng khi có xung đột dữ liệu (ví dụ: email đã tồn tại)
 */
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, ErrorCode.CONFLICT);
    }

    public ConflictException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }

    public ConflictException(String message, ErrorCode errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }

    public ConflictException(ErrorCode errorCode) {
        super(HttpStatus.CONFLICT, errorCode);
    }

    public static ConflictException emailExists(String email) {
        return new ConflictException("Email đã tồn tại: " + email, ErrorCode.EMAIL_EXISTS);
    }

    public static ConflictException companyNameExists(String name) {
        return new ConflictException("Tên công ty đã tồn tại: " + name, ErrorCode.COMPANY_NAME_EXISTS);
    }

    public static ConflictException employeeCodeExists(String code) {
        return new ConflictException("Mã nhân viên đã tồn tại: " + code, ErrorCode.EMPLOYEE_CODE_EXISTS);
    }
}
