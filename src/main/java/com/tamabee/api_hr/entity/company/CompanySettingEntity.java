package com.tamabee.api_hr.entity.company;

import com.tamabee.api_hr.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ cấu hình công ty.
 * Các config đã migrate sang bảng riêng (attendance, break, payroll, overtime).
 * Entity này vẫn giữ lại cho initializeDefaultSettings().
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "company_settings", indexes = {
        @Index(name = "idx_company_settings_deleted", columnList = "deleted")
})
public class CompanySettingEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;
}
