package com.tamabee.api_hr.config;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.util.RegionUtil;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Custom EntityListener thay thế AuditingEntityListener cho timestamp.
 * Inject timezone từ company region (lấy từ RegionContext) vào createdAt/updatedAt.
 *
 * Fallback: nếu không có region trong context (system operation, scheduler, v.v.),
 * sử dụng UTC làm timezone mặc định.
 */
public class RegionAwareAuditListener {

    @PrePersist
    public void setCreatedAt(BaseEntity entity) {
        ZoneId zone = resolveTimezone();
        LocalDateTime now = LocalDateTime.now(zone);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
    }

    @PreUpdate
    public void setUpdatedAt(BaseEntity entity) {
        ZoneId zone = resolveTimezone();
        entity.setUpdatedAt(LocalDateTime.now(zone));
    }

    /**
     * Resolve timezone từ RegionContext.
     * Nếu không có region (system operation, scheduler), fallback về UTC.
     */
    private ZoneId resolveTimezone() {
        String region = RegionContext.getCurrentRegion();
        return RegionUtil.getTimezone(region);
    }
}
