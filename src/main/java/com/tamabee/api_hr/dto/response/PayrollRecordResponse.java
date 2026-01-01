package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.BreakType;
import com.tamabee.api_hr.enums.PaymentStatus;
import com.tamabee.api_hr.enums.PayrollStatus;
import com.tamabee.api_hr.enums.SalaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response cho bản ghi lương của nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRecordResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private Long companyId;

    // Kỳ lương
    private Integer year;
    private Integer month;
    private String period; // Format: "2025-01"

    // Lương cơ bản
    private SalaryType salaryType;
    private BigDecimal baseSalary;
    private Integer workingDays;
    private Integer workingHours;

    // Tăng ca
    private BigDecimal regularOvertimePay;
    private BigDecimal nightOvertimePay;
    private BigDecimal holidayOvertimePay;
    private BigDecimal weekendOvertimePay;
    private BigDecimal totalOvertimePay;
    private Integer regularOvertimeHours;
    private Integer nightOvertimeHours;
    private Integer holidayOvertimeHours;
    private Integer weekendOvertimeHours;

    // Phụ cấp
    private List<AllowanceItemResponse> allowanceDetails;
    private BigDecimal totalAllowances;

    // Khấu trừ
    private List<DeductionItemResponse> deductionDetails;
    private BigDecimal totalDeductions;

    // Break time tracking
    private Integer totalBreakMinutes;
    private BreakType breakType;
    private BigDecimal breakDeductionAmount;

    // Tổng kết
    private BigDecimal grossSalary;
    private BigDecimal netSalary;

    // Trạng thái
    private PayrollStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
    private String paymentReference;

    // Notification
    private Boolean notificationSent;
    private LocalDateTime notificationSentAt;

    // Finalization
    private LocalDateTime finalizedAt;
    private Long finalizedBy;
    private String finalizedByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Chi tiết phụ cấp
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllowanceItemResponse {
        private String code;
        private String name;
        private BigDecimal amount;
        private Boolean taxable;
    }

    /**
     * Chi tiết khấu trừ
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeductionItemResponse {
        private String code;
        private String name;
        private BigDecimal amount;
    }
}
