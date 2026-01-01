package com.tamabee.api_hr.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả tính toán khấu trừ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductionResult {

    // Chi tiết từng khoản khấu trừ
    @Builder.Default
    private List<DeductionItem> items = new ArrayList<>();

    // Tổng khấu trừ
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    // Khấu trừ do đi muộn
    @Builder.Default
    private BigDecimal latePenalty = BigDecimal.ZERO;

    // Khấu trừ do về sớm
    @Builder.Default
    private BigDecimal earlyLeavePenalty = BigDecimal.ZERO;
}
