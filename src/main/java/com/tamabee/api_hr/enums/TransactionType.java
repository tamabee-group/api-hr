package com.tamabee.api_hr.enums;

/**
 * Loại giao dịch trong ví
 */
public enum TransactionType {
    DEPOSIT, // Nạp tiền
    BILLING, // Trừ tiền subscription hàng tháng
    REFUND, // Hoàn tiền
    COMMISSION // Hoa hồng giới thiệu
}
