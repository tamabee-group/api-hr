package com.tamabee.api_hr.enums;

/**
 * Enum định nghĩa các loại entity được audit.
 */
public enum AuditEntityType {
    ATTENDANCE_RECORD, // Bản ghi chấm công
    PAYROLL_RECORD, // Bản ghi lương
    COMPANY_SETTINGS, // Cài đặt công ty
    WORK_SCHEDULE, // Lịch làm việc
    LEAVE_REQUEST, // Yêu cầu nghỉ phép
    ADJUSTMENT_REQUEST, // Yêu cầu điều chỉnh
    SCHEDULE_SELECTION, // Chọn lịch làm việc
    HOLIDAY // Ngày lễ
}
