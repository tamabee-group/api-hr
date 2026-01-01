package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Request cập nhật cấu hình tăng ca
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeConfigRequest {

    private Boolean enableOvertime;
    private Boolean requireApproval;

    @DecimalMin("1.0")
    private BigDecimal regularOvertimeRate;

    @DecimalMin("1.0")
    private BigDecimal nightOvertimeRate;

    @DecimalMin("1.0")
    private BigDecimal holidayOvertimeRate;

    @DecimalMin("1.0")
    private BigDecimal weekendOvertimeRate;

    private LocalTime nightStartTime;
    private LocalTime nightEndTime;

    @Min(1)
    @Max(12)
    private Integer maxOvertimeHoursPerDay;

    @Min(1)
    @Max(100)
    private Integer maxOvertimeHoursPerMonth;
}
