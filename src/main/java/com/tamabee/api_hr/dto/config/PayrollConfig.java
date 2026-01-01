package com.tamabee.api_hr.dto.config;

import com.tamabee.api_hr.enums.RoundingDirection;
import com.tamabee.api_hr.enums.SalaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cấu hình tính lương của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollConfig {

    @Builder.Default
    private SalaryType defaultSalaryType = SalaryType.MONTHLY;

    // Ngày trả lương trong tháng
    @Builder.Default
    private Integer payDay = 25;

    // Ngày chốt công trong tháng
    @Builder.Default
    private Integer cutoffDay = 20;

    // Làm tròn lương
    @Builder.Default
    private RoundingDirection salaryRounding = RoundingDirection.NEAREST;

    // Số ngày làm việc tiêu chuẩn/tháng
    @Builder.Default
    private Integer standardWorkingDaysPerMonth = 22;

    // Số giờ làm việc tiêu chuẩn/ngày
    @Builder.Default
    private Integer standardWorkingHoursPerDay = 8;
}
