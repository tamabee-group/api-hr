package com.tamabee.api_hr.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Cấu hình tăng ca của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeConfig {

    // Bật/tắt tính overtime
    @Builder.Default
    private Boolean overtimeEnabled = true;

    @Builder.Default
    private Boolean requireApproval = false;

    // Số giờ làm việc tiêu chuẩn (mặc định 8 tiếng)
    @Builder.Default
    private Integer standardWorkingHours = 8;

    // Giờ bắt đầu/kết thúc ca đêm
    @Builder.Default
    private LocalTime nightStartTime = LocalTime.of(22, 0); // 22:00

    @Builder.Default
    private LocalTime nightEndTime = LocalTime.of(5, 0); // 05:00

    // Overtime multipliers - có thể cấu hình linh hoạt
    @Builder.Default
    private BigDecimal regularOvertimeRate = new BigDecimal("1.25"); // Tăng ca thường

    @Builder.Default
    private BigDecimal nightWorkRate = new BigDecimal("1.25"); // Làm đêm (không tăng ca)

    @Builder.Default
    private BigDecimal nightOvertimeRate = new BigDecimal("1.50"); // Tăng ca đêm

    @Builder.Default
    private BigDecimal holidayOvertimeRate = new BigDecimal("1.35"); // Tăng ca ngày lễ

    @Builder.Default
    private BigDecimal holidayNightOvertimeRate = new BigDecimal("1.60"); // Tăng ca đêm ngày lễ

    @Builder.Default
    private BigDecimal weekendOvertimeRate = new BigDecimal("1.35"); // Tăng ca cuối tuần

    // Sử dụng legal minimum hay custom
    @Builder.Default
    private Boolean useLegalMinimum = true;

    // Locale cho legal requirements (vi, ja)
    @Builder.Default
    private String locale = "ja";

    // Giới hạn tăng ca
    @Builder.Default
    private Integer maxOvertimeHoursPerDay = 4;

    @Builder.Default
    private Integer maxOvertimeHoursPerMonth = 45;
}
