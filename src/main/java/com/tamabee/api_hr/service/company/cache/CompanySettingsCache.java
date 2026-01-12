package com.tamabee.api_hr.service.company.cache;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.tamabee.api_hr.entity.company.CompanySettingEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * Cache company settings trong phạm vi một HTTP request.
 * Tránh truy vấn database nhiều lần trong cùng request.
 * 
 * Sử dụng @RequestScope để mỗi request có một instance cache riêng,
 * tự động được dọn dẹp khi request kết thúc.
 */
@Slf4j
@Component
@RequestScope
public class CompanySettingsCache {

    private CompanySettingEntity cachedEntity;
    private boolean entityQueried = false;

    /**
     * Lấy entity từ cache
     */
    public CompanySettingEntity getEntity() {
        return cachedEntity;
    }

    /**
     * Lưu entity vào cache
     */
    public void putEntity(CompanySettingEntity entity) {
        this.cachedEntity = entity;
        this.entityQueried = true;
        log.debug("Cached CompanySettingEntity");
    }

    /**
     * Kiểm tra entity đã được query chưa (kể cả khi không tìm thấy)
     */
    public boolean isEntityQueried() {
        return entityQueried;
    }

    /**
     * Xóa toàn bộ cache
     */
    public void invalidate() {
        cachedEntity = null;
        entityQueried = false;
        log.debug("Invalidated company settings cache");
    }
}
