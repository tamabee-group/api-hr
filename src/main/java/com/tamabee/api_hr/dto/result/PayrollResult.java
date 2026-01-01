package com.tamabee.api_hr.dto.result;

import com.tamabee.api_hr.enums.BreakType;
import com.tamabee.api_hr.enums.SalaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Kết quả tính toán lương
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollResult {

    // Loại lương
    private SalaryType salaryType;

    // Lương cơ bản
    @Builder.Default
    private BigDecimal baseSalary = BigDecimal.ZERO;

    // Tổng tiền tăng ca
    @Builder.Default
    private BigDecimal totalOvertimePay = BigDecimal.ZERO;

    // Chi tiết tăng ca
    private OvertimeResult overtimeResult;

    // Tổng phụ cấp
    @Builder.Default
    private BigDecimal totalAllowances = BigDecimal.ZERO;

    // Chi tiết phụ cấp
    private AllowanceResult allowanceResult;

    // Tổng khấu trừ
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    // Chi tiết khấu trừ
    private DeductionResult deductionResult;

    // Lương gộp = baseSalary + totalOvertimePay + totalAllowances
    @Builder.Default
    private BigDecimal grossSalary = BigDecimal.ZERO;

    // Lương thực nhận = grossSalary - totalDeductions
    @Builder.Default
    private BigDecimal netSalary = BigDecimal.ZERO;

    // Break time fields
    private Integer totalBreakMinutes;
    private BreakType breakType;
    @Builder.Default
    private BigDecimal breakDeductionAmount = BigDecimal.ZERO;
}
