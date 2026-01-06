package com.tamabee.api_hr.entity.payroll;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.PayrollPeriodStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity lưu trữ kỳ lương.
 * Quản lý chu kỳ tính lương với workflow DRAFT → REVIEWING → APPROVED → PAID.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "payroll_periods", indexes = {
        @Index(name = "idx_payroll_period_company_id", columnList = "companyId"),
        @Index(name = "idx_payroll_period_status", columnList = "status"),
        @Index(name = "idx_payroll_period_year_month", columnList = "companyId, year, month"),
        @Index(name = "idx_payroll_period_company_status", columnList = "companyId, status")
})
public class PayrollPeriodEntity extends BaseEntity {

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // Ngày bắt đầu kỳ lương
    @Column(nullable = false)
    private LocalDate periodStart;

    // Ngày kết thúc kỳ lương
    @Column(nullable = false)
    private LocalDate periodEnd;

    // Năm
    @Column(nullable = false)
    private Integer year;

    // Tháng
    @Column(nullable = false)
    private Integer month;

    // Trạng thái kỳ lương
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollPeriodStatus status = PayrollPeriodStatus.DRAFT;

    // ID người tạo
    @Column(nullable = false)
    private Long createdBy;

    // ID người duyệt
    private Long approvedBy;

    // Thời gian duyệt
    private LocalDateTime approvedAt;

    // Thời gian thanh toán
    private LocalDateTime paidAt;

    // Mã tham chiếu thanh toán
    @Column(length = 100)
    private String paymentReference;

    // === Thông tin tổng hợp ===
    // Tổng lương gộp
    @Column(precision = 15, scale = 2)
    private BigDecimal totalGrossSalary;

    // Tổng lương thực nhận
    @Column(precision = 15, scale = 2)
    private BigDecimal totalNetSalary;

    // Tổng số nhân viên
    private Integer totalEmployees;
}
