package com.tamabee.api_hr.entity.wallet;

import com.tamabee.api_hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Entity cho gói dịch vụ subscription
 * Hỗ trợ đa ngôn ngữ (vi, en, ja)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "plans", indexes = {
        @Index(name = "idx_plans_deleted", columnList = "deleted"),
        @Index(name = "idx_plans_is_active", columnList = "isActive"),
        @Index(name = "idx_plans_deleted_is_active", columnList = "deleted, isActive")
})
public class PlanEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    // Vietnamese
    @Column(name = "name_vi", nullable = false)
    private String nameVi;

    @Column(name = "description_vi", columnDefinition = "TEXT")
    private String descriptionVi;

    // English
    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    // Japanese
    @Column(name = "name_ja", nullable = false)
    private String nameJa;

    @Column(name = "description_ja", columnDefinition = "TEXT")
    private String descriptionJa;

    // Common fields
    @Column(name = "monthly_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "max_employees", nullable = false)
    private Integer maxEmployees;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
