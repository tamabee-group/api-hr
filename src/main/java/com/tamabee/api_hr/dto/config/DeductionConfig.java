package com.tamabee.api_hr.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình khấu trừ của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductionConfig {

    @Builder.Default
    private List<DeductionRule> deductions = new ArrayList<>();

    // Phạt đi muộn
    @Builder.Default
    private Boolean enableLatePenalty = false;

    @Builder.Default
    private BigDecimal latePenaltyPerMinute = BigDecimal.ZERO;

    // Phạt về sớm
    @Builder.Default
    private Boolean enableEarlyLeavePenalty = false;

    @Builder.Default
    private BigDecimal earlyLeavePenaltyPerMinute = BigDecimal.ZERO;

    // Khấu trừ vắng mặt
    @Builder.Default
    private Boolean enableAbsenceDeduction = true;
}
