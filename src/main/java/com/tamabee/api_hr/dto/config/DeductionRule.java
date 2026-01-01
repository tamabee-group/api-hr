package com.tamabee.api_hr.dto.config;

import com.tamabee.api_hr.enums.DeductionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Quy tắc khấu trừ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductionRule {

    private String code;

    private String name;

    private DeductionType type;

    // Số tiền cố định (cho FIXED type)
    private BigDecimal amount;

    // Phần trăm (cho PERCENTAGE type)
    private BigDecimal percentage;

    // Thứ tự áp dụng
    private Integer order;
}
