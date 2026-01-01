package com.tamabee.api_hr.enums;

/**
 * Enum định nghĩa các loại hành động audit.
 */
public enum AuditAction {
    CREATE, // Tạo mới
    UPDATE, // Cập nhật
    DELETE, // Xóa
    APPROVE, // Duyệt
    REJECT, // Từ chối
    FINALIZE, // Chốt (payroll)
    PAYMENT // Thanh toán
}
