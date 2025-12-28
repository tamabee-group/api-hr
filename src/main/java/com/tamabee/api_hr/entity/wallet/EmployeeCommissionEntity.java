package com.tamabee.api_hr.entity.wallet;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity cho hoa hồng giới thiệu của nhân viên Tamabee
 * Hoa hồng được tính khi company được giới thiệu thanh toán lần đầu (sau free
 * trial)
 * Status flow: PENDING → ELIGIBLE → PAID
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employee_commissions", indexes = {
        @Index(name = "idx_employee_commissions_employee_code", columnList = "employeeCode"),
        @Index(name = "idx_employee_commissions_company_id", columnList = "companyId"),
        @Index(name = "idx_employee_commissions_status", columnList = "status"),
        @Index(name = "idx_employee_commissions_deleted", columnList = "deleted"),
        @Index(name = "idx_employee_commissions_employee_code_deleted", columnList = "employeeCode, deleted"),
        @Index(name = "idx_employee_commissions_status_deleted", columnList = "status, deleted")
})
public class EmployeeCommissionEntity extends BaseEntity {

    // Employee code của nhân viên Tamabee nhận hoa hồng
    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    // Company được giới thiệu
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // Số tiền hoa hồng cố định (JPY)
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    // Trạng thái: PENDING (chưa đủ điều kiện), ELIGIBLE (đủ điều kiện), PAID (đã
    // thanh toán)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CommissionStatus status = CommissionStatus.PENDING;

    // Tổng billing của company tại thời điểm tạo commission (dùng để tính
    // eligibility)
    @Column(name = "company_billing_at_creation", nullable = false, precision = 15, scale = 0)
    private BigDecimal companyBillingAtCreation = BigDecimal.ZERO;

    // Thời điểm thanh toán hoa hồng
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Employee code của người thanh toán hoa hồng
    @Column(name = "paid_by", length = 50)
    private String paidBy;
}
