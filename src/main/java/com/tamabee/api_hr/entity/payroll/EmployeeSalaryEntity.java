package com.tamabee.api_hr.entity.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.SalaryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ thông tin lương của nhân viên.
 * Mỗi nhân viên có thể có nhiều bản ghi lương theo thời gian (effective date).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employee_salaries", indexes = {
        @Index(name = "idx_emp_salary_employee_id", columnList = "employeeId"),
        @Index(name = "idx_emp_salary_deleted", columnList = "deleted"),
        @Index(name = "idx_emp_salary_effective", columnList = "employeeId, effectiveFrom"),
        @Index(name = "idx_emp_salary_active", columnList = "employeeId, active")
})
public class EmployeeSalaryEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(nullable = false)
    private Long employeeId;

    // Loại lương
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalaryType salaryType;

    // Lương tháng (cho MONTHLY)
    @Column(precision = 15, scale = 2)
    private BigDecimal monthlySalary;

    // Lương ngày (cho DAILY)
    @Column(precision = 15, scale = 2)
    private BigDecimal dailyRate;

    // Lương giờ (cho HOURLY)
    @Column(precision = 15, scale = 2)
    private BigDecimal hourlyRate;

    // Lương theo ca (cho SHIFT_BASED)
    @Column(precision = 15, scale = 2)
    private BigDecimal shiftRate;

    // Ngày bắt đầu hiệu lực
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    // Ngày kết thúc hiệu lực (null = vẫn còn hiệu lực)
    private LocalDate effectiveTo;

    // Đánh dấu config đã được sử dụng để tính lương (không cho phép sửa/xóa)
    @Column(nullable = false)
    private Boolean usedInPayroll = false;

    // Đánh dấu config đang được áp dụng (chỉ 1 config active cho mỗi employee)
    @Column(nullable = false)
    private Boolean active = false;

    // Ghi chú
    private String note;
}
