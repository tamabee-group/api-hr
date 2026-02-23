package com.tamabee.api_hr.entity.payroll;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.SalaryItemType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ template phụ cấp/khấu trừ của công ty.
 * Mỗi template có tên và loại (ALLOWANCE hoặc DEDUCTION).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "salary_item_templates", indexes = {
        @Index(name = "idx_salary_item_template_type", columnList = "type"),
        @Index(name = "idx_salary_item_template_deleted", columnList = "deleted")
})
public class SalaryItemTemplateEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // Tên template
    @Column(nullable = false, length = 200)
    private String name;

    // Loại: ALLOWANCE hoặc DEDUCTION
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SalaryItemType type;
}
