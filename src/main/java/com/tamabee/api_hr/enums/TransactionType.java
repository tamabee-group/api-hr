package com.tamabee.api_hr.enums;

/**
 * Loại giao dịch trong ví
 */
public enum TransactionType {
    DEPOSIT, // Nạp tiền
    BILLING, // Trừ tiền subscription hàng tháng
    BILLING_FAILED, // Thanh toán thất bại do số dư không đủ
    REFUND, // Hoàn tiền
    COMMISSION // Hoa hồng giới thiệu
}
