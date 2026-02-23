package com.tamabee.api_hr.enums;

/**
 * Loại thao tác với break record trong yêu cầu điều chỉnh
 */
public enum BreakActionType {
    ADJUST,  // Điều chỉnh thời gian break
    DELETE,  // Xóa break record
    CREATE   // Tạo mới break record (khi chưa có attendance record)
}
