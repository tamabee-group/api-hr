package com.tamabee.api_hr.dto.response.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response cho preview lương trước khi finalize
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPreviewResponse {

    private Long companyId;
    private String companyName;
    private Integer year;
    private Integer month;
    private String period; // Format: "2025-01"

    // Tổng hợp
    private Integer totalEmployees;
    private BigDecimal totalBaseSalary;
    private BigDecimal totalOvertimePay;
    private BigDecimal totalAllowances;
    private BigDecimal totalDeductions;
    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;

    // Chi tiết từng nhân viên
    private List<PayrollRecordResponse> records;
}
