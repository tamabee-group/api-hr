package com.tamabee.api_hr.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO cho lịch sử thay đổi plan
 */
@Data
@Builder
public class PlanChangeHistoryResponse {
    private Long id;
    private String fromPlanName;
    private String toPlanName;
    private BigDecimal fromPlanPrice;
    private BigDecimal toPlanPrice;
    private String changeType;
    private LocalDate effectiveDate;
    private LocalDateTime createdAt;
}
