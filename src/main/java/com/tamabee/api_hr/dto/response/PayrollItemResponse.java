package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.BreakType;
import com.tamabee.api_hr.enums.PayrollItemStatus;
import com.tamabee.api_hr.enums.SalaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho chi tiết lương nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollItemResponse {

    private Long id;
    private Long payrollPeriodId;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private Long companyId;

    // Thông tin lương
    private SalaryType salaryType;
    private BigDecimal baseSalary;
    private BigDecimal calculatedBaseSalary;

    // Thời gian làm việc
    private Integer workingDays;
    private Integer workingHours;
    private Integer workingMinutes;

    // Tăng ca
    private Integer regularOvertimeMinutes;
    private Integer nightOvertimeMinutes;
    private Integer holidayOvertimeMinutes;
    private Integer weekendOvertimeMinutes;
    private BigDecimal totalOvertimePay;

    // Giải lao
    private Integer totalBreakMinutes;
    private BreakType breakType;
    private BigDecimal breakDeductionAmount;

    // Phụ cấp & Khấu trừ
    private List<AllowanceDetailResponse> allowanceDetails;
    private BigDecimal totalAllowances;
    private List<DeductionDetailResponse> deductionDetails;
    private BigDecimal totalDeductions;

    // Tổng kết
    private BigDecimal grossSalary;
    private BigDecimal netSalary;

    // Điều chỉnh
    private BigDecimal adjustmentAmount;
    private String adjustmentReason;
    private Long adjustedBy;
    private String adjustedByName;
    private LocalDateTime adjustedAt;

    // Trạng thái
    private PayrollItemStatus status;

    /**
     * Chi tiết phụ cấp
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllowanceDetailResponse {
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
    public static class DeductionDetailResponse {
        private String code;
        private String name;
        private BigDecimal amount;
        private BigDecimal percentage;
        private BigDecimal calculatedAmount;
    }
}
