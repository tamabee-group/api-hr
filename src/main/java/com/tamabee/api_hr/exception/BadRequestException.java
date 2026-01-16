package com.tamabee.api_hr.exception;

import org.springframework.http.HttpStatus;

import com.tamabee.api_hr.enums.ErrorCode;

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

    /**
     * Factory method cho lỗi plan đang được sử dụng
     */
    public static BadRequestException planInUse(Long planId) {
        return new BadRequestException("Gói dịch vụ đang được sử dụng bởi công ty, không thể xóa",
                ErrorCode.PLAN_IN_USE);
    }

    /**
     * Factory method cho lỗi số tiền không hợp lệ
     */
    public static BadRequestException invalidAmount() {
        return new BadRequestException("Số tiền phải lớn hơn 0", ErrorCode.INVALID_AMOUNT);
    }

    /**
     * Factory method cho lỗi số dư không đủ
     */
    public static BadRequestException insufficientBalance() {
        return new BadRequestException("Số dư không đủ để thực hiện giao dịch", ErrorCode.INSUFFICIENT_BALANCE);
    }

    /**
     * Factory method cho lỗi deposit đã được xử lý
     */
    public static BadRequestException depositAlreadyProcessed() {
        return new BadRequestException("Yêu cầu nạp tiền đã được xử lý", ErrorCode.DEPOSIT_ALREADY_PROCESSED);
    }

    /**
     * Factory method cho lỗi lý do từ chối rỗng
     */
    public static BadRequestException invalidRejectionReason() {
        return new BadRequestException("Lý do từ chối không được để trống", ErrorCode.INVALID_REJECTION_REASON);
    }

    /**
     * Factory method cho lỗi commission chưa đủ điều kiện
     */
    public static BadRequestException commissionNotEligible() {
        return new BadRequestException("Hoa hồng chưa đủ điều kiện thanh toán", ErrorCode.COMMISSION_NOT_ELIGIBLE);
    }

    /**
     * Factory method cho lỗi số tiền nạp dưới mức tối thiểu
     */
    public static BadRequestException minDepositAmount(int minAmount) {
        return new BadRequestException("Số tiền nạp tối thiểu là ¥" + minAmount, ErrorCode.MIN_DEPOSIT_AMOUNT);
    }

    /**
     * Factory method cho lỗi plan không active
     */
    public static BadRequestException planNotActive() {
        return new BadRequestException("Gói dịch vụ không còn hoạt động", ErrorCode.PLAN_NOT_ACTIVE);
    }

    /**
     * Factory method cho lỗi vượt quá giới hạn nhân viên của plan
     */
    public static BadRequestException planExceedsEmployeeLimit(int currentCount, int maxAllowed) {
        return new BadRequestException(
                "Số nhân viên hiện tại (" + currentCount + ") vượt quá giới hạn của gói (" + maxAllowed + ")",
                ErrorCode.PLAN_EXCEEDS_EMPLOYEE_LIMIT);
    }

    /**
     * Factory method cho lỗi công ty đang hoạt động (không cần reactivate)
     */
    public static BadRequestException companyAlreadyActive() {
        return new BadRequestException("Công ty đang hoạt động, không cần kích hoạt lại",
                ErrorCode.COMPANY_ALREADY_ACTIVE);
    }

    /**
     * Factory method cho lỗi tùy chỉnh với errorCode và message
     */
    public static BadRequestException custom(String errorCode, String message) {
        return new BadRequestException(message, errorCode);
    }

    /**
     * Factory method cho lỗi file không hợp lệ
     */
    public static BadRequestException invalidFile(String reason) {
        return new BadRequestException("File không hợp lệ: " + reason, ErrorCode.INVALID_FILE);
    }

    /**
     * Factory method cho lỗi file quá lớn
     */
    public static BadRequestException fileTooLarge(long maxSize) {
        long maxSizeMB = maxSize / (1024 * 1024);
        return new BadRequestException("File vượt quá kích thước cho phép (" + maxSizeMB + "MB)", ErrorCode.FILE_TOO_LARGE);
    }

    /**
     * Factory method cho lỗi loại file không được phép
     */
    public static BadRequestException invalidFileType(String allowedTypes) {
        return new BadRequestException("Loại file không được phép. Các loại cho phép: " + allowedTypes, ErrorCode.INVALID_FILE_TYPE);
    }
}
