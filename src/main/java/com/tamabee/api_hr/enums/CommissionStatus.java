package com.tamabee.api_hr.enums;

/**
 * Trạng thái hoa hồng giới thiệu
 * Flow: PENDING → ELIGIBLE → PAID
 */
public enum CommissionStatus {
    PENDING, // Chưa đủ điều kiện (billing <= commission)
    ELIGIBLE, // Đủ điều kiện (billing > commission)
    PAID // Đã thanh toán
}
