package com.tamabee.api_hr.entity.wallet;

import com.tamabee.api_hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity cho tính năng của gói dịch vụ
 * Hỗ trợ đa ngôn ngữ (vi, en, ja)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "plan_features", indexes = {
        @Index(name = "idx_plan_features_plan_id", columnList = "planId"),
        @Index(name = "idx_plan_features_deleted", columnList = "deleted"),
        @Index(name = "idx_plan_features_plan_id_deleted", columnList = "planId, deleted")
})
public class PlanFeatureEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    // Vietnamese
    @Column(name = "feature_vi", nullable = false, length = 500)
    private String featureVi;

    // English
    @Column(name = "feature_en", nullable = false, length = 500)
    private String featureEn;

    // Japanese
    @Column(name = "feature_ja", nullable = false, length = 500)
    private String featureJa;

    // Thứ tự hiển thị
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // Feature nổi bật
    @Column(name = "is_highlighted", nullable = false)
    private Boolean isHighlighted = false;
}
