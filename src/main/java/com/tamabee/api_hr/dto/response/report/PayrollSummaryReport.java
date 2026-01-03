package com.tamabee.api_hr.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo tổng hợp lương
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSummaryReport {

    private Long companyId;
    private LocalDate startDate;
    private LocalDate endDate;

    // Tổng quan
    private Integer totalEmployees;
    private BigDecimal totalBaseSalary;
    private BigDecimal totalOvertimePay;
    private BigDecimal totalAllowances;
    private BigDecimal totalDeductions;
    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;

    // Trung bình
    private BigDecimal averageGrossSalary;
    private BigDecimal averageNetSalary;

    // Chi tiết theo nhân viên
    private List<EmployeePayrollSummary> employeeSummaries;
}
