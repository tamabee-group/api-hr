package com.tamabee.api_hr.dto.config;

import com.tamabee.api_hr.enums.AllowanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Quy tắc phụ cấp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowanceRule {

    private String code;

    private String name;

    private AllowanceType type;

    private BigDecimal amount;

    @Builder.Default
    private Boolean taxable = true;

    // Điều kiện (cho CONDITIONAL type)
    private AllowanceCondition condition;
}
