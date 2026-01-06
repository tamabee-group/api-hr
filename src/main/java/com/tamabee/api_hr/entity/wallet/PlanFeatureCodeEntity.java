package com.tamabee.api_hr.entity.wallet;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.FeatureCode;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity mapping giữa Plan và FeatureCode.
 * Mỗi plan có thể có nhiều feature codes.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "plan_feature_codes", indexes = {
        @Index(name = "idx_plan_feature_codes_plan_id", columnList = "planId"),
        @Index(name = "idx_plan_feature_codes_deleted", columnList = "deleted"),
        @Index(name = "idx_plan_feature_codes_plan_id_deleted", columnList = "planId, deleted"),
        @Index(name = "idx_plan_feature_codes_plan_feature", columnList = "planId, featureCode")
})
public class PlanFeatureCodeEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_code", nullable = false, length = 50)
    private FeatureCode featureCode;
}
