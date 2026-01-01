package com.tamabee.api_hr.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Kết quả tính toán tăng ca
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeResult {

    // Giờ làm thường (trong 8h)
    @Builder.Default
    private Integer regularMinutes = 0;

    // Giờ làm đêm (trong 8h, 22:00-05:00)
    @Builder.Default
    private Integer nightMinutes = 0;

    // Số phút tăng ca thường (ngoài giờ làm việc tiêu chuẩn, không phải đêm)
    @Builder.Default
    private Integer regularOvertimeMinutes = 0;

    // Số phút tăng ca đêm (>8h, trong giờ đêm 22:00-05:00)
    @Builder.Default
    private Integer nightOvertimeMinutes = 0;

    // Số phút tăng ca ngày lễ
    @Builder.Default
    private Integer holidayOvertimeMinutes = 0;

    // Số phút làm đêm ngày lễ
    @Builder.Default
    private Integer holidayNightMinutes = 0;

    // Số phút tăng ca cuối tuần
    @Builder.Default
    private Integer weekendOvertimeMinutes = 0;

    // Tổng số phút tăng ca
    @Builder.Default
    private Integer totalOvertimeMinutes = 0;

    // Tiền tăng ca thường
    @Builder.Default
    private BigDecimal regularOvertimeAmount = BigDecimal.ZERO;

    // Tiền làm đêm (không tăng ca)
    @Builder.Default
    private BigDecimal nightWorkAmount = BigDecimal.ZERO;

    // Tiền tăng ca đêm
    @Builder.Default
    private BigDecimal nightOvertimeAmount = BigDecimal.ZERO;

    // Tiền tăng ca ngày lễ
    @Builder.Default
    private BigDecimal holidayOvertimeAmount = BigDecimal.ZERO;

    // Tiền tăng ca đêm ngày lễ
    @Builder.Default
    private BigDecimal holidayNightOvertimeAmount = BigDecimal.ZERO;

    // Tiền tăng ca cuối tuần
    @Builder.Default
    private BigDecimal weekendOvertimeAmount = BigDecimal.ZERO;

    // Tổng tiền tăng ca
    @Builder.Default
    private BigDecimal totalOvertimeAmount = BigDecimal.ZERO;

    // Backward compatibility - giữ lại các field cũ
    @Deprecated
    public BigDecimal getRegularOvertimePay() {
        return regularOvertimeAmount;
    }

    @Deprecated
    public BigDecimal getNightOvertimePay() {
        return nightOvertimeAmount;
    }

    @Deprecated
    public BigDecimal getHolidayOvertimePay() {
        return holidayOvertimeAmount;
    }

    @Deprecated
    public BigDecimal getWeekendOvertimePay() {
        return weekendOvertimeAmount;
    }

    @Deprecated
    public BigDecimal getTotalOvertimePay() {
        return totalOvertimeAmount;
    }
}
