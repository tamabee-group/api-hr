package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.RoundingDirection;
import com.tamabee.api_hr.enums.SalaryType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cập nhật cấu hình tính lương
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollConfigRequest {

    private SalaryType defaultSalaryType;

    @Min(1)
    @Max(31)
    private Integer payDay;

    @Min(1)
    @Max(31)
    private Integer cutoffDay;

    private RoundingDirection salaryRounding;

    @Min(1)
    @Max(31)
    private Integer standardWorkingDaysPerMonth;

    @Min(1)
    @Max(24)
    private Integer standardWorkingHoursPerDay;
}
