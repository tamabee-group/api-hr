package com.tamabee.api_hr.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * Response DTO cho thông tin ví của công ty
 */
@Data
public class WalletResponse {

    private Long id;

    private Long companyId;

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
}
