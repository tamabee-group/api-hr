package com.tamabee.api_hr.entity.payroll;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.BreakType;
import com.tamabee.api_hr.enums.PaymentStatus;
import com.tamabee.api_hr.enums.PayrollStatus;
import com.tamabee.api_hr.enums.SalaryType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity lưu trữ bản ghi lương của nhân viên theo kỳ (tháng).
 * Bao gồm chi tiết lương cơ bản, tăng ca, phụ cấp, khấu trừ và trạng thái thanh
 * toán.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "payroll_records", indexes = {
        @Index(name = "idx_payroll_employee_id", columnList = "employeeId"),
        @Index(name = "idx_payroll_company_id", columnList = "companyId"),
        @Index(name = "idx_payroll_period", columnList = "companyId, year, month"),
        @Index(name = "idx_payroll_employee_period", columnList = "employeeId, year, month"),
        @Index(name = "idx_payroll_status", columnList = "status")
})
public class PayrollRecordEntity extends BaseEntity {

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // Kỳ lương: năm
    @Column(nullable = false)
    private Integer year;

    // Kỳ lương: tháng
    @Column(nullable = false)
    private Integer month;

    // === Lương cơ bản ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalaryType salaryType;

    @Column(precision = 15, scale = 2)
    private BigDecimal baseSalary;

    // Số ngày/giờ làm việc thực tế
    private Integer workingDays;
    private Integer workingHours;

    // === Tăng ca ===
    @Column(precision = 15, scale = 2)
    private BigDecimal regularOvertimePay;

    @Column(precision = 15, scale = 2)
    private BigDecimal nightOvertimePay;

    @Column(precision = 15, scale = 2)
    private BigDecimal holidayOvertimePay;

    @Column(precision = 15, scale = 2)
    private BigDecimal weekendOvertimePay;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalOvertimePay;

    // Số giờ tăng ca
    private Integer regularOvertimeHours;
    private Integer nightOvertimeHours;
    private Integer holidayOvertimeHours;
    private Integer weekendOvertimeHours;

    // === Phụ cấp ===
    // Chi tiết từng loại phụ cấp (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String allowanceDetails;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAllowances;

    // === Khấu trừ ===
    // Chi tiết từng loại khấu trừ (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String deductionDetails;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalDeductions;

    // === Tổng kết ===
    @Column(precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @Column(precision = 15, scale = 2)
    private BigDecimal netSalary;

    // === Trạng thái ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollStatus status = PayrollStatus.DRAFT;

    // === Payment tracking ===
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime paidAt;

    // Mã tham chiếu thanh toán
    private String paymentReference;

    // === Notification ===
    @Column(nullable = false)
    private Boolean notificationSent = false;

    private LocalDateTime notificationSentAt;

    // === Finalization ===
    private LocalDateTime finalizedAt;
    private Long finalizedBy;

    // === Break time tracking ===
    // Tổng giờ giải lao trong kỳ (phút)
    private Integer totalBreakMinutes;

    // Loại giải lao áp dụng (PAID/UNPAID)
    @Enumerated(EnumType.STRING)
    private BreakType breakType;

    // Số tiền khấu trừ do giải lao (nếu unpaid)
    @Column(precision = 15, scale = 2)
    private BigDecimal breakDeductionAmount;
}
