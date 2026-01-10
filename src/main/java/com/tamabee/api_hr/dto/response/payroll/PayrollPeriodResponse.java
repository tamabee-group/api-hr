package com.tamabee.api_hr.dto.response.payroll;

import com.tamabee.api_hr.enums.PayrollPeriodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO cho kỳ lương
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriodResponse {

    private Long id;
    private Long companyId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer year;
    private Integer month;
    private PayrollPeriodStatus status;

    // Thông tin người tạo
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;

    // Thông tin người duyệt
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;

    // Thông tin thanh toán
    private LocalDateTime paidAt;
    private String paymentReference;

    // Thông tin tổng hợp
    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;
    private Integer totalEmployees;
}
