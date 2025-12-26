package com.tamabee.api_hr.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum quản lý tất cả error codes trong hệ thống
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // === COMMON ===
    BAD_REQUEST("BAD_REQUEST", "Yêu cầu không hợp lệ"),
    VALIDATION_ERROR("VALIDATION_ERROR", "Dữ liệu không hợp lệ"),
    NOT_FOUND("NOT_FOUND", "Không tìm thấy"),
    CONFLICT("CONFLICT", "Xung đột dữ liệu"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Lỗi hệ thống"),
    INVALID_ARGUMENT("INVALID_ARGUMENT", "Tham số không hợp lệ"),

    // === AUTH ===
    UNAUTHORIZED("UNAUTHORIZED", "Chưa xác thực"),
    FORBIDDEN("FORBIDDEN", "Không có quyền truy cập"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng"),
    NOT_AUTHENTICATED("NOT_AUTHENTICATED", "Chưa đăng nhập"),
    INVALID_TOKEN("INVALID_TOKEN", "Token không hợp lệ"),
    INVALID_REFRESH_TOKEN("INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ"),
    NO_REFRESH_TOKEN("NO_REFRESH_TOKEN", "Refresh token không tồn tại"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token đã hết hạn"),
    INVALID_CODE("INVALID_CODE", "Mã xác thực không hợp lệ hoặc đã hết hạn"),
    EMAIL_NOT_VERIFIED("EMAIL_NOT_VERIFIED", "Email chưa được xác thực"),

    // === USER ===
    USER_NOT_FOUND("USER_NOT_FOUND", "Không tìm thấy người dùng"),
    EMAIL_EXISTS("EMAIL_EXISTS", "Email đã tồn tại"),
    EMAIL_NOT_FOUND("EMAIL_NOT_FOUND", "Email không tồn tại trong hệ thống"),
    EMPLOYEE_CODE_EXISTS("EMPLOYEE_CODE_EXISTS", "Mã nhân viên đã tồn tại"),

    // === COMPANY ===
    COMPANY_NOT_FOUND("COMPANY_NOT_FOUND", "Không tìm thấy công ty"),
    COMPANY_NAME_EXISTS("COMPANY_NAME_EXISTS", "Tên công ty đã tồn tại"),

    // === FILE ===
    FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "Lỗi khi tải file lên"),
    FILE_TOO_LARGE("FILE_TOO_LARGE", "File tải lên quá lớn"),
    FILE_NOT_FOUND("FILE_NOT_FOUND", "Không tìm thấy file"),
    INVALID_FILE_TYPE("INVALID_FILE_TYPE", "Loại file không hợp lệ"),

    // === EMAIL ===
    EMAIL_SEND_FAILED("EMAIL_SEND_FAILED", "Không thể gửi email"),
    EMAIL_TEMPLATE_NOT_FOUND("EMAIL_TEMPLATE_NOT_FOUND", "Không tìm thấy template email");

    private final String code;
    private final String message;
}
