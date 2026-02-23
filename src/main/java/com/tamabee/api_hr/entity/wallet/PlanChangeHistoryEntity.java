package com.tamabee.api_hr.entity.wallet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;
import lombok.Data;

/**
 * Entity lưu lịch sử thay đổi plan của company
 * Dùng để tính billing theo plan cao nhất trong kỳ (chống gian lận)
 */
@Data
@Entity
@Table(name = "plan_change_history", indexes = {
        @Index(name = "idx_plan_change_history_company_id", columnList = "companyId"),
        @Index(name = "idx_plan_change_history_effective_date", columnList = "effectiveDate"),
        @Index(name = "idx_plan_change_history_billing_period", columnList = "companyId, billingPeriodStart, billingPeriodEnd")
})
public class PlanChangeHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    private Long fromPlanId;

    @Column(nullable = false)
    private Long toPlanId;

    @Column(precision = 15, scale = 0)
    private BigDecimal fromPlanPrice;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal toPlanPrice;

    // INITIAL, UPGRADE, DOWNGRADE, TRIAL_CHANGE, SCHEDULED_APPLY
    @Column(nullable = false, length = 30)
    private String changeType;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    private LocalDate billingPeriodStart;

    private LocalDate billingPeriodEnd;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
    }
}
