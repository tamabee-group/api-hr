package com.tamabee.api_hr.service.core.interfaces;

import java.math.BigDecimal;

public interface IEmailService {
    void sendTemporaryPassword(String email, String employeeCode, String temporaryPassword, String language);

    /**
     * Gửi email thông báo nạp tiền thành công
     *
     * @param email       email của company
     * @param companyName tên company
     * @param amount      số tiền nạp
     * @param balance     số dư sau khi nạp
     * @param language    ngôn ngữ (vi, en, ja)
     */
    void sendDepositApproved(String email, String companyName, BigDecimal amount, BigDecimal balance, String language);

    /**
     * Gửi email thông báo trừ tiền subscription thành công
     *
     * @param email       email của company
     * @param companyName tên company
     * @param planName    tên gói dịch vụ
     * @param amount      số tiền trừ
     * @param balance     số dư còn lại
     * @param language    ngôn ngữ (vi, en, ja)
     */
    void sendBillingNotification(String email, String companyName, String planName, BigDecimal amount,
            BigDecimal balance, String language);

    /**
     * Gửi email thông báo số dư không đủ để billing
     *
     * @param email       email của company
     * @param companyName tên company
     * @param planName    tên gói dịch vụ
     * @param amount      số tiền cần thanh toán
     * @param balance     số dư hiện tại
     * @param language    ngôn ngữ (vi, en, ja)
     */
    void sendInsufficientBalance(String email, String companyName, String planName, BigDecimal amount,
            BigDecimal balance, String language);
}
