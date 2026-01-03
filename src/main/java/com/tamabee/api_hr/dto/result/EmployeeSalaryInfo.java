package com.tamabee.api_hr.dto.result;

import com.tamabee.api_hr.enums.SalaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Thông tin lương của nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSalaryInfo {

    // Loại lương
    private SalaryType salaryType;

    // Lương tháng (cho MONTHLY)
    private BigDecimal monthlySalary;

    // Lương ngày (cho DAILY)
    private BigDecimal dailyRate;

    // Lương giờ (cho HOURLY)
    private BigDecimal hourlyRate;

    // Lương theo ca (cho SHIFT_BASED)
    private BigDecimal shiftRate;
}
