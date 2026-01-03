package com.tamabee.api_hr.entity.payroll;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.DeductionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity lưu trữ khấu trừ cá nhân của nhân viên.
 * Ghi nhận các khoản khấu trừ được gán riêng cho từng nhân viên.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employee_deductions", indexes = {
        @Index(name = "idx_emp_deduction_employee_id", columnList = "employeeId"),
        @Index(name = "idx_emp_deduction_company_id", columnList = "companyId"),
        @Index(name = "idx_emp_deduction_deleted", columnList = "deleted"),
        @Index(name = "idx_emp_deduction_active", columnList = "employeeId, isActive"),
        @Index(name = "idx_emp_deduction_effective", columnList = "employeeId, effectiveFrom, effectiveTo")
})
public class EmployeeDeductionEntity extends BaseEntity {

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // ID công ty
    @Column(nullable = false)
    private Long companyId;

    // Mã khấu trừ
    @Column(nullable = false, length = 50)
    private String deductionCode;

    // Tên khấu trừ
    @Column(nullable = false, length = 200)
    private String deductionName;

    // Loại khấu trừ
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeductionType deductionType;

    // Số tiền khấu trừ cố định
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    // Phần trăm khấu trừ
    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    // Ngày bắt đầu hiệu lực
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    // Ngày kết thúc hiệu lực (null = vẫn còn hiệu lực)
    private LocalDate effectiveTo;

    // Trạng thái hoạt động
    @Column(nullable = false)
    private Boolean isActive = true;
}
