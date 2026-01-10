package com.tamabee.api_hr.dto.response.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * Response DTO cho tổng quan wallet của công ty (Admin Dashboard)
 */
@Data
public class WalletOverviewResponse {

    private Long id;

    private Long companyId;

    private String companyName;

    private BigDecimal balance;

    private LocalDateTime lastBillingDate;

    private LocalDateTime nextBillingDate;

    private LocalDateTime freeTrialEndDate;

    // Tên gói dịch vụ theo các ngôn ngữ
    private String planNameVi;
    private String planNameEn;
    private String planNameJa;

    // Đang trong thời gian miễn phí
    private Boolean isFreeTrialActive;

    // Tổng số tiền đã nạp
    private BigDecimal totalDeposits;

    // Tổng số tiền đã billing
    private BigDecimal totalBillings;
}
