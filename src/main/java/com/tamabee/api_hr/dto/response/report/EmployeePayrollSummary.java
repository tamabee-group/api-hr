package com.tamabee.api_hr.dto.response.report;

import com.tamabee.api_hr.enums.SalaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Tổng hợp lương của một nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePayrollSummary {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private SalaryType salaryType;

    // Lương cơ bản
    private BigDecimal baseSalary;
    private BigDecimal calculatedBaseSalary;

    // Overtime
    private BigDecimal overtimePay;

    // Phụ cấp & khấu trừ
    private BigDecimal totalAllowances;
    private BigDecimal totalDeductions;

    // Tổng
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
}
