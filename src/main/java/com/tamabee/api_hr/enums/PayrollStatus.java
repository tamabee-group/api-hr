package com.tamabee.api_hr.enums;

/**
 * Trạng thái bảng lương
 */
public enum PayrollStatus {
    DRAFT, // Nháp (có thể sửa)
    FINALIZED, // Đã chốt (không thể sửa)
    PAID // Đã trả lương
}
