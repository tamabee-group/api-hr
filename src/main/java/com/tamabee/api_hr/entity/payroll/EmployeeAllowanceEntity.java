package com.tamabee.api_hr.entity.payroll;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.AllowanceType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity lưu trữ phụ cấp cá nhân của nhân viên.
 * Ghi nhận các khoản phụ cấp được gán riêng cho từng nhân viên.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employee_allowances", indexes = {
        @Index(name = "idx_emp_allowance_employee_id", columnList = "employeeId"),
        @Index(name = "idx_emp_allowance_deleted", columnList = "deleted"),
        @Index(name = "idx_emp_allowance_active", columnList = "employeeId, isActive"),
        @Index(name = "idx_emp_allowance_effective", columnList = "employeeId, effectiveFrom, effectiveTo")
})
public class EmployeeAllowanceEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // Mã phụ cấp
    @Column(nullable = false, length = 50)
    private String allowanceCode;

    // Tên phụ cấp
    @Column(nullable = false, length = 200)
    private String allowanceName;

    // Loại phụ cấp
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllowanceType allowanceType;

    // Số tiền phụ cấp
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Có tính thuế không
    @Column(nullable = false)
    private Boolean taxable = true;

    // Ngày bắt đầu hiệu lực
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    // Ngày kết thúc hiệu lực (null = vẫn còn hiệu lực)
    private LocalDate effectiveTo;

    // Trạng thái hoạt động
    @Column(nullable = false)
    private Boolean isActive = true;
}
