package com.tamabee.api_hr.exception;

import org.springframework.http.HttpStatus;

import com.tamabee.api_hr.enums.ErrorCode;

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

    public static NotFoundException company(String tenantDomain) {
        return new NotFoundException("Không tìm thấy công ty với tenant: " + tenantDomain, ErrorCode.COMPANY_NOT_FOUND);
    }

    public static NotFoundException email(String email) {
        return new NotFoundException("Email không tồn tại trong hệ thống: " + email, ErrorCode.EMAIL_NOT_FOUND);
    }

    public static NotFoundException plan(Long id) {
        return new NotFoundException("Không tìm thấy gói dịch vụ với id: " + id, ErrorCode.PLAN_NOT_FOUND);
    }

    public static NotFoundException setting(String key) {
        return new NotFoundException("Không tìm thấy cấu hình với key: " + key, ErrorCode.SETTING_NOT_FOUND);
    }

    public static NotFoundException wallet(Long companyId) {
        return new NotFoundException("Không tìm thấy ví của công ty với id: " + companyId, ErrorCode.WALLET_NOT_FOUND);
    }

    public static NotFoundException deposit(Long id) {
        return new NotFoundException("Không tìm thấy yêu cầu nạp tiền với id: " + id, ErrorCode.DEPOSIT_NOT_FOUND);
    }

    public static NotFoundException commission(Long id) {
        return new NotFoundException("Không tìm thấy hoa hồng với id: " + id, ErrorCode.COMMISSION_NOT_FOUND);
    }

    public static NotFoundException companySettings(Long companyId) {
        return new NotFoundException("Không tìm thấy cấu hình của công ty với id: " + companyId,
                ErrorCode.SETTINGS_NOT_FOUND);
    }

    public static NotFoundException payrollRecord(Long id) {
        return new NotFoundException("Không tìm thấy bản ghi lương với id: " + id,
                ErrorCode.PAYROLL_NOT_FOUND);
    }

    public static NotFoundException document(Long id) {
        return new NotFoundException("Không tìm thấy tài liệu với id: " + id, ErrorCode.DOCUMENT_NOT_FOUND);
    }

    public static NotFoundException contract(Long id) {
        return new NotFoundException("Không tìm thấy hợp đồng với id: " + id, ErrorCode.CONTRACT_NOT_FOUND);
    }

    public static NotFoundException notification(Long id) {
        return new NotFoundException("Không tìm thấy thông báo với id: " + id, ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    public static NotFoundException systemNotification(Long id) {
        return new NotFoundException("Không tìm thấy thông báo hệ thống với id: " + id, ErrorCode.SYSTEM_NOTIFICATION_NOT_FOUND);
    }

    public static NotFoundException location(Long id) {
        return new NotFoundException("Không tìm thấy vị trí chấm công với id: " + id, ErrorCode.LOCATION_NOT_FOUND);
    }

    public static NotFoundException feedback(Long id) {
        return new NotFoundException("Không tìm thấy feedback với id: " + id, ErrorCode.FEEDBACK_NOT_FOUND);
    }

    public static NotFoundException kiosk(Long id) {
        return new NotFoundException("Không tìm thấy máy chấm công với id: " + id, ErrorCode.KIOSK_NOT_FOUND);
    }
}
