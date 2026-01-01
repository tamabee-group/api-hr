package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.PayrollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response cho tổng hợp lương của công ty theo kỳ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriodSummaryResponse {

    private Long companyId;
    private String companyName;
    private Integer year;
    private Integer month;
    private String period; // Format: "2025-01"

    // Trạng thái
    private PayrollStatus status;
    private LocalDateTime finalizedAt;
    private Long finalizedBy;
    private String finalizedByName;

    // Thống kê nhân viên
    private Integer totalEmployees;
    private Integer paidEmployees;
    private Integer pendingEmployees;
    private Integer failedEmployees;

    // Thống kê tài chính
    private BigDecimal totalBaseSalary;
    private BigDecimal totalOvertimePay;
    private BigDecimal totalAllowances;
    private BigDecimal totalDeductions;
    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;

    // Thống kê thanh toán
    private BigDecimal totalPaid;
    private BigDecimal totalPending;

    // Notification
    private Integer notificationsSent;
    private Integer notificationsPending;
}
