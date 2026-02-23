package com.tamabee.api_hr.constants;

/**
 * Constants cho Notification codes
 * Dùng để định danh các loại thông báo, hỗ trợ i18n translation ở frontend
 */
public final class NotificationCode {

    private NotificationCode() {
    }

    // ==================== Welcome ====================
    /**
     * Thông báo chào mừng công ty mới đăng ký
     */
    public static final String WELCOME_COMPANY = "WELCOME_COMPANY";

    /**
     * Thông báo chào mừng nhân viên mới
     */
    public static final String WELCOME_EMPLOYEE = "WELCOME_EMPLOYEE";

    // ==================== Payroll ====================
    /**
     * Thông báo kỳ lương mới được tạo
     */
    public static final String PAYROLL_PERIOD_CREATED = "PAYROLL_PERIOD_CREATED";

    /**
     * Thông báo kỳ lương được gửi duyệt (gửi cho admin company)
     */
    public static final String PAYROLL_SUBMITTED = "PAYROLL_SUBMITTED";

    /**
     * Thông báo phiếu lương đã được xác nhận
     */
    public static final String PAYROLL_CONFIRMED = "PAYROLL_CONFIRMED";

    /**
     * Thông báo kỳ lương bị từ chối (gửi cho người gửi duyệt)
     */
    public static final String PAYROLL_REJECTED = "PAYROLL_REJECTED";

    /**
     * Thông báo lương đã được thanh toán
     */
    public static final String PAYROLL_PAID = "PAYROLL_PAID";

    // ==================== Wallet ====================
    /**
     * Thông báo có yêu cầu nạp tiền mới (gửi cho Tamabee admin)
     */
    public static final String DEPOSIT_SUBMITTED = "DEPOSIT_SUBMITTED";

    /**
     * Thông báo yêu cầu nạp tiền đã được duyệt
     */
    public static final String DEPOSIT_APPROVED = "DEPOSIT_APPROVED";

    /**
     * Thông báo yêu cầu nạp tiền bị từ chối
     */
    public static final String DEPOSIT_REJECTED = "DEPOSIT_REJECTED";

    /**
     * Cảnh báo số dư ví thấp
     */
    public static final String LOW_BALANCE_WARNING = "LOW_BALANCE_WARNING";

    // ==================== Leave ====================
    /**
     * Thông báo có đơn xin nghỉ phép mới (gửi cho admin/manager)
     */
    public static final String LEAVE_SUBMITTED = "LEAVE_SUBMITTED";

    /**
     * Thông báo đơn xin nghỉ phép đã được duyệt
     */
    public static final String LEAVE_APPROVED = "LEAVE_APPROVED";

    /**
     * Thông báo đơn xin nghỉ phép bị từ chối
     */
    public static final String LEAVE_REJECTED = "LEAVE_REJECTED";

    // ==================== Adjustment ====================
    /**
     * Thông báo có yêu cầu điều chỉnh chấm công mới (gửi cho admin/manager)
     */
    public static final String ADJUSTMENT_SUBMITTED = "ADJUSTMENT_SUBMITTED";

    /**
     * Thông báo yêu cầu điều chỉnh chấm công đã được duyệt
     */
    public static final String ADJUSTMENT_APPROVED = "ADJUSTMENT_APPROVED";

    /**
     * Thông báo yêu cầu điều chỉnh chấm công bị từ chối
     */
    public static final String ADJUSTMENT_REJECTED = "ADJUSTMENT_REJECTED";

    /**
     * Thông báo chấm công đã được admin/manager điều chỉnh trực tiếp
     */
    public static final String ATTENDANCE_ADJUSTED = "ATTENDANCE_ADJUSTED";

    // ==================== System ====================
    /**
     * Thông báo hệ thống chung
     */
    public static final String SYSTEM_ANNOUNCEMENT = "SYSTEM_ANNOUNCEMENT";

    // ==================== Feedback ====================
    /**
     * Thông báo có feedback mới từ khách hàng (gửi cho Tamabee staff)
     */
    public static final String FEEDBACK_SUBMITTED = "FEEDBACK_SUBMITTED";

    /**
     * Thông báo feedback đã được phản hồi (gửi cho người gửi feedback)
     */
    public static final String FEEDBACK_REPLIED = "FEEDBACK_REPLIED";

    // ==================== Shift ====================
    /**
     * Thông báo lịch phân ca đã được công bố (gửi cho employee)
     */
    public static final String SHIFT_SCHEDULE_PUBLISHED = "SHIFT_SCHEDULE_PUBLISHED";
}
