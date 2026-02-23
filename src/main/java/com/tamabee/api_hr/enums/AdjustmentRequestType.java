package com.tamabee.api_hr.enums;

/**
 * Loại yêu cầu điều chỉnh chấm công.
 * DELETE_BREAK đã được thay thế bằng breakItems với actionType=DELETE.
 */
public enum AdjustmentRequestType {
    // Điều chỉnh thời gian check-in/check-out và/hoặc nhiều break records
    ADJUST,
    
    // Xóa toàn bộ attendance record của ngày
    DELETE_RECORD
}
