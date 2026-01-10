package com.tamabee.api_hr.entity.payroll;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.BreakType;
import com.tamabee.api_hr.enums.PayrollItemStatus;
import com.tamabee.api_hr.enums.SalaryType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity lưu trữ chi tiết lương của từng nhân viên trong kỳ lương.
 * Bao gồm tất cả thông tin tính toán: lương cơ bản, tăng ca, phụ cấp, khấu trừ.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "payroll_items", indexes = {
        @Index(name = "idx_payroll_item_period_id", columnList = "payrollPeriodId"),
        @Index(name = "idx_payroll_item_employee_id", columnList = "employeeId"),
        @Index(name = "idx_payroll_item_period_employee", columnList = "payrollPeriodId, employeeId")
})
public class PayrollItemEntity extends BaseEntity {

    // ID kỳ lương
    @Column(nullable = false)
    private Long payrollPeriodId;

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // === Thông tin lương ===
    // Loại lương
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalaryType salaryType;

    // Lương cơ bản (từ config)
    @Column(precision = 15, scale = 2)
    private BigDecimal baseSalary;

    // Lương cơ bản đã tính toán (sau khi áp dụng công thức)
    @Column(precision = 15, scale = 2)
    private BigDecimal calculatedBaseSalary;

    // === Thời gian làm việc ===
    // Số ngày làm việc
    private Integer workingDays;

    // Số giờ làm việc
    private Integer workingHours;

    // Số phút làm việc
    private Integer workingMinutes;

    // === Tăng ca ===
    // Số phút tăng ca thường
    private Integer regularOvertimeMinutes;

    // Số phút tăng ca đêm
    private Integer nightOvertimeMinutes;

    // Số phút tăng ca ngày lễ
    private Integer holidayOvertimeMinutes;

    // Số phút tăng ca cuối tuần
    private Integer weekendOvertimeMinutes;

    // Tổng tiền tăng ca
    @Column(precision = 15, scale = 2)
    private BigDecimal totalOvertimePay;

    // === Giải lao ===
    // Tổng số phút giải lao
    private Integer totalBreakMinutes;

    // Loại giải lao
    @Enumerated(EnumType.STRING)
    private BreakType breakType;

    // Số tiền khấu trừ do giải lao (nếu UNPAID)
    @Column(precision = 15, scale = 2)
    private BigDecimal breakDeductionAmount;

    // === Phụ cấp & Khấu trừ ===
    // Chi tiết phụ cấp (JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String allowanceDetails;

    // Tổng phụ cấp
    @Column(precision = 15, scale = 2)
    private BigDecimal totalAllowances;

    // Chi tiết khấu trừ (JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String deductionDetails;

    // Tổng khấu trừ
    @Column(precision = 15, scale = 2)
    private BigDecimal totalDeductions;

    // === Tổng kết ===
    // Lương gộp
    @Column(precision = 15, scale = 2)
    private BigDecimal grossSalary;

    // Lương thực nhận
    @Column(precision = 15, scale = 2)
    private BigDecimal netSalary;

    // === Điều chỉnh ===
    // Số tiền điều chỉnh
    @Column(precision = 15, scale = 2)
    private BigDecimal adjustmentAmount;

    // Lý do điều chỉnh
    @Column(length = 500)
    private String adjustmentReason;

    // ID người điều chỉnh
    private Long adjustedBy;

    // Thời gian điều chỉnh
    private LocalDateTime adjustedAt;

    // === Trạng thái ===
    // Trạng thái chi tiết lương
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollItemStatus status = PayrollItemStatus.CALCULATED;
}
