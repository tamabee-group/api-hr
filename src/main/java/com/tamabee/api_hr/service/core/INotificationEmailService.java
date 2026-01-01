package com.tamabee.api_hr.service.core;

import com.tamabee.api_hr.entity.payroll.PayrollRecordEntity;

/**
 * Service gửi email thông báo cho nhân viên.
 * Hỗ trợ đa ngôn ngữ (vi, en, ja) với fallback sang English.
 */
public interface INotificationEmailService {

    // ==================== Salary Notification ====================

    /**
     * Gửi thông báo lương cho một nhân viên
     *
     * @param employeeId ID nhân viên
     * @param payroll    bản ghi lương
     */
    void sendSalaryNotification(Long employeeId, PayrollRecordEntity payroll);

    /**
     * Gửi thông báo lương cho tất cả nhân viên trong kỳ
     *
     * @param companyId ID công ty
     * @param year      năm
     * @param month     tháng
     */
    void sendBulkSalaryNotifications(Long companyId, Integer year, Integer month);

    // ==================== Adjustment Notification ====================

    /**
     * Gửi thông báo yêu cầu điều chỉnh được duyệt
     *
     * @param employeeId ID nhân viên
     * @param requestId  ID yêu cầu điều chỉnh
     */
    void sendAdjustmentApprovedNotification(Long employeeId, Long requestId);

    /**
     * Gửi thông báo yêu cầu điều chỉnh bị từ chối
     *
     * @param employeeId ID nhân viên
     * @param requestId  ID yêu cầu điều chỉnh
     */
    void sendAdjustmentRejectedNotification(Long employeeId, Long requestId);

    // ==================== Leave Notification ====================

    /**
     * Gửi thông báo nghỉ phép được duyệt
     *
     * @param employeeId ID nhân viên
     * @param requestId  ID yêu cầu nghỉ phép
     */
    void sendLeaveApprovedNotification(Long employeeId, Long requestId);

    /**
     * Gửi thông báo nghỉ phép bị từ chối
     *
     * @param employeeId ID nhân viên
     * @param requestId  ID yêu cầu nghỉ phép
     */
    void sendLeaveRejectedNotification(Long employeeId, Long requestId);
}
