package com.tamabee.api_hr.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả tính toán phụ cấp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowanceResult {

    // Chi tiết từng khoản phụ cấp
    @Builder.Default
    private List<AllowanceItem> items = new ArrayList<>();

    // Tổng phụ cấp
    @Builder.Default
    private BigDecimal totalAllowances = BigDecimal.ZERO;

    // Tổng phụ cấp chịu thuế
    @Builder.Default
    private BigDecimal taxableAllowances = BigDecimal.ZERO;

    // Tổng phụ cấp không chịu thuế
    @Builder.Default
    private BigDecimal nonTaxableAllowances = BigDecimal.ZERO;
}
