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
     * Factory method cho lỗi tùy chỉnh với errorCode và message
     */
    public static BadRequestException custom(String errorCode, String message) {
        return new BadRequestException(message, errorCode);
    }
}
