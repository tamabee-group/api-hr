package com.tamabee.api_hr.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    // Tên gói dịch vụ hiện tại
    private String planName;

    // Đang trong thời gian miễn phí
    private Boolean isFreeTrialActive;
}
