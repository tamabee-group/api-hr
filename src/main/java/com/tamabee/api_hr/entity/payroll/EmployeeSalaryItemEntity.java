package com.tamabee.api_hr.entity.payroll;

import java.math.BigDecimal;

import com.tamabee.api_hr.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ phụ cấp/khấu trừ được gán cho nhân viên.
 * Tham chiếu đến SalaryItemTemplateEntity để lấy tên và loại.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employee_salary_items", indexes = {
        @Index(name = "idx_employee_salary_item_employee", columnList = "employeeId"),
        @Index(name = "idx_employee_salary_item_template", columnList = "templateId"),
        @Index(name = "idx_employee_salary_item_deleted", columnList = "deleted")
})
public class EmployeeSalaryItemEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // ID nhân viên
    @Column(nullable = false)
    private Long employeeId;

    // ID template
    @Column(nullable = false)
    private Long templateId;

    // Số tiền
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Relationship với template (để lấy name và type)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "templateId", insertable = false, updatable = false)
    private SalaryItemTemplateEntity template;
}
