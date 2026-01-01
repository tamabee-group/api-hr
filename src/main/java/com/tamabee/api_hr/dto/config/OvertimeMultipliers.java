package com.tamabee.api_hr.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Các hệ số nhân lương tăng ca
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeMultipliers {

    // Tăng ca thường (>8h, không phải đêm)
    @Builder.Default
    private BigDecimal regularOvertime = new BigDecimal("1.25");

    // Làm đêm (22:00-05:00, trong 8h)
    @Builder.Default
    private BigDecimal nightWork = new BigDecimal("1.25");

    // Tăng ca đêm (>8h, trong giờ đêm)
    @Builder.Default
    private BigDecimal nightOvertime = new BigDecimal("1.50");

    // Tăng ca ngày lễ
    @Builder.Default
    private BigDecimal holidayOvertime = new BigDecimal("1.35");

    // Tăng ca đêm ngày lễ
    @Builder.Default
    private BigDecimal holidayNightOvertime = new BigDecimal("1.60");

    // Tăng ca cuối tuần
    @Builder.Default
    private BigDecimal weekendOvertime = new BigDecimal("1.35");
}
