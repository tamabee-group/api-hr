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
        INVALID_REQUEST("INVALID_REQUEST", "Yêu cầu không hợp lệ"),
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

        // === TENANT DOMAIN ===
        INVALID_TENANT_DOMAIN("INVALID_TENANT_DOMAIN",
                        "Địa chỉ tên miền không hợp lệ. Chỉ chấp nhận chữ thường, số và dấu gạch ngang, độ dài 3-30 ký tự"),
        TENANT_DOMAIN_EXISTS("TENANT_DOMAIN_EXISTS", "Địa chỉ tên miền đã được sử dụng"),
        TENANT_DOMAIN_RESERVED("TENANT_DOMAIN_RESERVED", "Địa chỉ tên miền này đã được đặt trước"),
        TENANT_PROVISIONING_FAILED("TENANT_PROVISIONING_FAILED", "Không thể tạo cơ sở dữ liệu cho công ty"),

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
        DEPOSIT_NOT_REJECTED("DEPOSIT_NOT_REJECTED", "Chỉ có thể chỉnh sửa yêu cầu bị từ chối"),
        INVALID_TRANSFER_PROOF("INVALID_TRANSFER_PROOF", "Ảnh chứng minh chuyển khoản không hợp lệ"),
        INVALID_REJECTION_REASON("INVALID_REJECTION_REASON", "Lý do từ chối không được để trống"),

        // === COMMISSION ===
        COMMISSION_NOT_FOUND("COMMISSION_NOT_FOUND", "Không tìm thấy hoa hồng"),
        COMMISSION_NOT_ELIGIBLE("COMMISSION_NOT_ELIGIBLE", "Hoa hồng chưa đủ điều kiện thanh toán"),

        // === COMPANY SETTINGS ===
        SETTINGS_NOT_FOUND("SETTINGS_NOT_FOUND", "Không tìm thấy cấu hình công ty"),
        SETTINGS_ALREADY_EXISTS("SETTINGS_ALREADY_EXISTS", "Cấu hình công ty đã tồn tại"),
        INVALID_WORK_TIME("INVALID_WORK_TIME", "Giờ bắt đầu phải trước giờ kết thúc"),
        INVALID_CONFIG("INVALID_CONFIG", "Cấu hình không hợp lệ"),

        // === BREAK CONFIG ===
        INVALID_BREAK_CONFIG("INVALID_BREAK_CONFIG", "Cấu hình giờ giải lao không hợp lệ"),
        BREAK_MINIMUM_EXCEEDS_MAXIMUM("BREAK_MINIMUM_EXCEEDS_MAXIMUM",
                        "Thời gian giải lao tối thiểu không được vượt quá tối đa"),
        BREAK_BELOW_LEGAL_MINIMUM("BREAK_BELOW_LEGAL_MINIMUM", "Thời gian giải lao không đạt yêu cầu pháp luật"),
        BREAK_ALREADY_STARTED("BREAK_ALREADY_STARTED", "Đã có giờ giải lao đang diễn ra"),
        BREAK_ALREADY_ACTIVE("BREAK_ALREADY_ACTIVE", "Đã có giờ giải lao đang diễn ra"),
        NO_ACTIVE_BREAK("NO_ACTIVE_BREAK", "Không có giờ giải lao đang diễn ra"),
        BREAK_OUTSIDE_WORKING_HOURS("BREAK_OUTSIDE_WORKING_HOURS", "Giờ giải lao nằm ngoài giờ làm việc"),
        BREAK_RECORD_NOT_FOUND("BREAK_RECORD_NOT_FOUND", "Không tìm thấy bản ghi giờ giải lao"),
        INVALID_BREAK_DURATION("INVALID_BREAK_DURATION", "Thời gian giải lao không hợp lệ"),
        BREAK_START_AFTER_END("BREAK_START_AFTER_END", "Giờ bắt đầu giải lao phải trước giờ kết thúc"),
        MAX_BREAKS_REACHED("MAX_BREAKS_REACHED", "Đã đạt số lần giải lao tối đa trong ngày"),
        BREAK_OVERLAP("BREAK_OVERLAP", "Giờ giải lao bị trùng với giờ giải lao khác"),
        INVALID_BREAK_RECORD("INVALID_BREAK_RECORD", "Bản ghi giờ giải lao không thuộc về bản ghi chấm công này"),
        BREAK_RECORD_ID_REQUIRED("BREAK_RECORD_ID_REQUIRED", "Phải chỉ định breakRecordId khi điều chỉnh giờ giải lao"),

        // === OVERTIME CONFIG ===
        INVALID_OVERTIME_CONFIG("INVALID_OVERTIME_CONFIG", "Cấu hình tăng ca không hợp lệ"),
        OVERTIME_RATE_BELOW_LEGAL_MINIMUM("OVERTIME_RATE_BELOW_LEGAL_MINIMUM",
                        "Hệ số tăng ca không đạt yêu cầu pháp luật"),
        INVALID_NIGHT_HOURS_CONFIG("INVALID_NIGHT_HOURS_CONFIG", "Cấu hình giờ đêm không hợp lệ"),

        // === WORK SCHEDULE ===
        SCHEDULE_NOT_FOUND("SCHEDULE_NOT_FOUND", "Không tìm thấy lịch làm việc"),
        SCHEDULE_NAME_EXISTS("SCHEDULE_NAME_EXISTS", "Tên lịch làm việc đã tồn tại"),
        INVALID_SCHEDULE_TIME("INVALID_SCHEDULE_TIME", "Giờ bắt đầu phải trước giờ kết thúc"),
        SCHEDULE_OVERLAP("SCHEDULE_OVERLAP", "Lịch làm việc bị trùng với lịch hiện có"),
        SCHEDULE_ASSIGNMENT_NOT_FOUND("SCHEDULE_ASSIGNMENT_NOT_FOUND", "Không tìm thấy assignment"),
        SCHEDULE_IN_USE("SCHEDULE_IN_USE", "Lịch làm việc đang được sử dụng, không thể xóa"),
        DEFAULT_SCHEDULE_NOT_FOUND("DEFAULT_SCHEDULE_NOT_FOUND", "Không tìm thấy lịch làm việc mặc định"),

        // === WORK MODE ===
        WORK_MODE_CHANGE_FAILED("WORK_MODE_CHANGE_FAILED", "Không thể thay đổi chế độ làm việc"),
        FLEXIBLE_MODE_NO_DEFAULT_SCHEDULE("FLEXIBLE_MODE_NO_DEFAULT_SCHEDULE",
                        "Chế độ linh hoạt yêu cầu ít nhất một lịch làm việc mặc định"),
        FIXED_HOURS_MISSING_CONFIG("FIXED_HOURS_MISSING_CONFIG",
                        "Chế độ giờ cố định yêu cầu cấu hình giờ làm việc mặc định"),
        INVALID_WORK_MODE("INVALID_WORK_MODE", "Chế độ làm việc không hợp lệ"),

        // === ATTENDANCE ===
        ATTENDANCE_RECORD_NOT_FOUND("ATTENDANCE_RECORD_NOT_FOUND", "Không tìm thấy bản ghi chấm công"),
        ATTENDANCE_ALREADY_EXISTS("ATTENDANCE_ALREADY_EXISTS", "Đã có bản ghi chấm công hôm nay"),
        ALREADY_CHECKED_IN("ALREADY_CHECKED_IN", "Đã check-in hôm nay"),
        NOT_CHECKED_IN("NOT_CHECKED_IN", "Chưa check-in, không thể check-out"),
        ALREADY_CHECKED_OUT("ALREADY_CHECKED_OUT", "Đã check-out hôm nay"),
        DEVICE_REGISTRATION_REQUIRED("DEVICE_REGISTRATION_REQUIRED", "Yêu cầu đăng ký thiết bị"),
        GEO_LOCATION_REQUIRED("GEO_LOCATION_REQUIRED", "Yêu cầu vị trí địa lý"),
        GEO_FENCE_VIOLATION("GEO_FENCE_VIOLATION", "Vị trí nằm ngoài khu vực cho phép"),
        INVALID_DEVICE("INVALID_DEVICE", "Thiết bị chưa được đăng ký"),
        OUTSIDE_GEOFENCE("OUTSIDE_GEOFENCE", "Vị trí nằm ngoài khu vực cho phép"),
        INVALID_ATTENDANCE_TIME("INVALID_ATTENDANCE_TIME", "Thời gian chấm công không hợp lệ"),

        // === ADJUSTMENT REQUEST ===
        ADJUSTMENT_NOT_FOUND("ADJUSTMENT_NOT_FOUND", "Không tìm thấy yêu cầu điều chỉnh"),
        ADJUSTMENT_ALREADY_PROCESSED("ADJUSTMENT_ALREADY_PROCESSED", "Yêu cầu điều chỉnh đã được xử lý"),
        ADJUSTMENT_PENDING_EXISTS("ADJUSTMENT_PENDING_EXISTS",
                        "Đã có yêu cầu điều chỉnh đang chờ duyệt cho bản ghi này"),
        INVALID_ADJUSTMENT_TIME("INVALID_ADJUSTMENT_TIME", "Thời gian điều chỉnh không hợp lệ"),
        ADJUSTMENT_NO_CHANGES("ADJUSTMENT_NO_CHANGES", "Phải thay đổi ít nhất một thời gian check-in hoặc check-out"),
        REJECTION_REASON_REQUIRED("REJECTION_REASON_REQUIRED", "Lý do từ chối không được để trống"),

        // === FILE ===
        FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "Lỗi khi tải file lên"),
        FILE_TOO_LARGE("FILE_TOO_LARGE", "File tải lên quá lớn"),
        FILE_NOT_FOUND("FILE_NOT_FOUND", "Không tìm thấy file"),
        INVALID_FILE_TYPE("INVALID_FILE_TYPE", "Loại file không hợp lệ"),

        // === EMAIL ===
        EMAIL_SEND_FAILED("EMAIL_SEND_FAILED", "Không thể gửi email"),
        EMAIL_TEMPLATE_NOT_FOUND("EMAIL_TEMPLATE_NOT_FOUND", "Không tìm thấy template email"),

        // === PAYROLL ===
        PAYROLL_NOT_FOUND("PAYROLL_NOT_FOUND", "Không tìm thấy bản ghi lương"),
        PAYROLL_ALREADY_FINALIZED("PAYROLL_ALREADY_FINALIZED", "Lương kỳ này đã được finalize"),
        PAYROLL_NOT_FINALIZED("PAYROLL_NOT_FINALIZED", "Lương chưa được finalize"),
        INVALID_PAYROLL_PERIOD("INVALID_PAYROLL_PERIOD", "Kỳ lương không hợp lệ"),
        ATTENDANCE_NOT_COMPLETE("ATTENDANCE_NOT_COMPLETE", "Dữ liệu chấm công chưa đầy đủ"),
        PAYROLL_PERIOD_NOT_FOUND("PAYROLL_PERIOD_NOT_FOUND", "Không tìm thấy kỳ lương"),
        PAYROLL_PERIOD_EXISTS("PAYROLL_PERIOD_EXISTS", "Kỳ lương đã tồn tại cho tháng này"),
        PAYROLL_ALREADY_APPROVED("PAYROLL_ALREADY_APPROVED", "Kỳ lương đã được duyệt, không thể chỉnh sửa"),
        PAYROLL_ALREADY_PAID("PAYROLL_ALREADY_PAID", "Kỳ lương đã được thanh toán, không thể chỉnh sửa"),
        PAYROLL_INVALID_STATUS_TRANSITION("PAYROLL_INVALID_STATUS_TRANSITION",
                        "Chuyển trạng thái kỳ lương không hợp lệ"),
        PAYROLL_ITEM_NOT_FOUND("PAYROLL_ITEM_NOT_FOUND", "Không tìm thấy chi tiết lương"),
        PAYROLL_CALCULATION_FAILED("PAYROLL_CALCULATION_FAILED", "Tính lương thất bại"),

        // === PAYMENT ===
        PAYMENT_FAILED("PAYMENT_FAILED", "Thanh toán thất bại"),
        INVALID_PAYMENT_STATUS("INVALID_PAYMENT_STATUS", "Trạng thái thanh toán không hợp lệ"),

        // === SCHEDULE SELECTION ===
        SELECTION_NOT_FOUND("SELECTION_NOT_FOUND", "Không tìm thấy yêu cầu chọn lịch"),
        SELECTION_ALREADY_PROCESSED("SELECTION_ALREADY_PROCESSED", "Yêu cầu chọn lịch đã được xử lý"),
        SELECTION_PENDING_EXISTS("SELECTION_PENDING_EXISTS", "Đã có yêu cầu chọn lịch đang chờ duyệt"),
        INVALID_SELECTION_DATE("INVALID_SELECTION_DATE", "Ngày bắt đầu phải trước ngày kết thúc"),
        SCHEDULE_NOT_AVAILABLE("SCHEDULE_NOT_AVAILABLE", "Lịch làm việc không khả dụng"),

        // === PLAN FEATURE ===
        FEATURE_NOT_AVAILABLE("FEATURE_NOT_AVAILABLE", "Tính năng không khả dụng trong gói dịch vụ hiện tại"),
        COMPANY_NO_PLAN("COMPANY_NO_PLAN", "Công ty chưa đăng ký gói dịch vụ"),

        // === HOLIDAY ===
        HOLIDAY_NOT_FOUND("HOLIDAY_NOT_FOUND", "Không tìm thấy ngày nghỉ lễ"),
        HOLIDAY_DATE_EXISTS("HOLIDAY_DATE_EXISTS", "Ngày nghỉ lễ đã tồn tại"),

        // === LEAVE ===
        LEAVE_REQUEST_NOT_FOUND("LEAVE_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu nghỉ phép"),
        LEAVE_ALREADY_PROCESSED("LEAVE_ALREADY_PROCESSED", "Yêu cầu nghỉ phép đã được xử lý"),
        LEAVE_OVERLAPPING("LEAVE_OVERLAPPING", "Đã có yêu cầu nghỉ phép trùng thời gian"),
        LEAVE_INVALID_DATE_RANGE("LEAVE_INVALID_DATE_RANGE", "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc"),
        LEAVE_INSUFFICIENT_BALANCE("LEAVE_INSUFFICIENT_BALANCE", "Số ngày phép còn lại không đủ"),
        LEAVE_BALANCE_NOT_FOUND("LEAVE_BALANCE_NOT_FOUND", "Không tìm thấy thông tin số ngày phép"),
        LEAVE_NOT_OWNER("LEAVE_NOT_OWNER", "Không có quyền hủy yêu cầu nghỉ phép này"),
        LEAVE_CANNOT_CANCEL("LEAVE_CANNOT_CANCEL", "Chỉ có thể hủy yêu cầu đang chờ duyệt"),

        // === AUDIT LOG ===
        AUDIT_LOG_NOT_FOUND("AUDIT_LOG_NOT_FOUND", "Không tìm thấy audit log"),

        // === SHIFT ===
        SHIFT_TEMPLATE_NOT_FOUND("SHIFT_TEMPLATE_NOT_FOUND", "Không tìm thấy mẫu ca làm việc"),
        SHIFT_ASSIGNMENT_NOT_FOUND("SHIFT_ASSIGNMENT_NOT_FOUND", "Không tìm thấy phân ca"),
        SHIFT_OVERLAP_EXISTS("SHIFT_OVERLAP_EXISTS", "Ca làm việc bị trùng với ca hiện có"),
        SHIFT_SWAP_NOT_ALLOWED("SHIFT_SWAP_NOT_ALLOWED", "Không thể đổi ca"),
        SHIFT_SWAP_REQUEST_NOT_FOUND("SHIFT_SWAP_REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu đổi ca"),
        SHIFT_SWAP_ALREADY_PROCESSED("SHIFT_SWAP_ALREADY_PROCESSED", "Yêu cầu đổi ca đã được xử lý"),
        SHIFT_TEMPLATE_IN_USE("SHIFT_TEMPLATE_IN_USE", "Mẫu ca đang được sử dụng, không thể xóa"),

        // === SALARY CONFIG ===
        SALARY_CONFIG_NOT_FOUND("SALARY_CONFIG_NOT_FOUND", "Không tìm thấy cấu hình lương"),
        INVALID_SALARY_TYPE("INVALID_SALARY_TYPE", "Loại lương không hợp lệ"),
        SALARY_CONFIG_OVERLAP("SALARY_CONFIG_OVERLAP", "Cấu hình lương bị trùng thời gian"),
        SALARY_AMOUNT_REQUIRED("SALARY_AMOUNT_REQUIRED", "Phải nhập mức lương tương ứng với loại lương"),

        // === ALLOWANCE & DEDUCTION ===
        ALLOWANCE_NOT_FOUND("ALLOWANCE_NOT_FOUND", "Không tìm thấy phụ cấp"),
        DEDUCTION_NOT_FOUND("DEDUCTION_NOT_FOUND", "Không tìm thấy khấu trừ"),
        INVALID_EFFECTIVE_DATE("INVALID_EFFECTIVE_DATE", "Ngày hiệu lực không hợp lệ"),
        DEDUCTION_AMOUNT_OR_PERCENTAGE_REQUIRED("DEDUCTION_AMOUNT_OR_PERCENTAGE_REQUIRED",
                        "Phải nhập số tiền hoặc phần trăm khấu trừ"),

        // === CONTRACT ===
        CONTRACT_NOT_FOUND("CONTRACT_NOT_FOUND", "Không tìm thấy hợp đồng"),
        CONTRACT_OVERLAP_EXISTS("CONTRACT_OVERLAP_EXISTS", "Hợp đồng bị trùng thời gian với hợp đồng hiện có"),
        CONTRACT_ALREADY_TERMINATED("CONTRACT_ALREADY_TERMINATED", "Hợp đồng đã được chấm dứt"),
        CONTRACT_NUMBER_EXISTS("CONTRACT_NUMBER_EXISTS", "Số hợp đồng đã tồn tại"),
        CONTRACT_INVALID_DATE_RANGE("CONTRACT_INVALID_DATE_RANGE", "Ngày bắt đầu phải trước ngày kết thúc"),

        // === FEATURE ===
        FEATURE_NOT_SUPPORTED("FEATURE_NOT_SUPPORTED", "Tính năng chưa được hỗ trợ"),

        // === LOCATION ===
        INVALID_LOCATION("INVALID_LOCATION", "Tọa độ vị trí không hợp lệ");

        private final String code;
        private final String message;
}
