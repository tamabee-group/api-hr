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
    INVALID_ROLE("INVALID_ROLE", "Role không hợp lệ"),
    ACCESS_DENIED("ACCESS_DENIED", "Không có quyền truy cập tài nguyên này"),

    // === COMPANY ===
    COMPANY_NOT_FOUND("COMPANY_NOT_FOUND", "Không tìm thấy công ty"),
    COMPANY_NAME_EXISTS("COMPANY_NAME_EXISTS", "Tên công ty đã tồn tại"),
    INVALID_REFERRAL_CODE("INVALID_REFERRAL_CODE", "Mã giới thiệu không hợp lệ"),

    // === PLAN ===
    PLAN_NOT_FOUND("PLAN_NOT_FOUND", "Không tìm thấy gói dịch vụ"),
    PLAN_IN_USE("PLAN_IN_USE", "Gói dịch vụ đang được sử dụng, không thể xóa"),

    // === SETTING ===
    SETTING_NOT_FOUND("SETTING_NOT_FOUND", "Không tìm thấy cấu hình"),

    // === WALLET ===
    WALLET_NOT_FOUND("WALLET_NOT_FOUND", "Không tìm thấy ví"),
    INVALID_AMOUNT("INVALID_AMOUNT", "Số tiền không hợp lệ"),
    INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", "Số dư không đủ"),

    // === DEPOSIT ===
    DEPOSIT_NOT_FOUND("DEPOSIT_NOT_FOUND", "Không tìm thấy yêu cầu nạp tiền"),
    DEPOSIT_ALREADY_PROCESSED("DEPOSIT_ALREADY_PROCESSED", "Yêu cầu nạp tiền đã được xử lý"),
    INVALID_TRANSFER_PROOF("INVALID_TRANSFER_PROOF", "Ảnh chứng minh chuyển khoản không hợp lệ"),
    INVALID_REJECTION_REASON("INVALID_REJECTION_REASON", "Lý do từ chối không được để trống"),

    // === COMMISSION ===
    COMMISSION_NOT_FOUND("COMMISSION_NOT_FOUND", "Không tìm thấy hoa hồng"),
    COMMISSION_NOT_ELIGIBLE("COMMISSION_NOT_ELIGIBLE", "Hoa hồng chưa đủ điều kiện thanh toán"),

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
