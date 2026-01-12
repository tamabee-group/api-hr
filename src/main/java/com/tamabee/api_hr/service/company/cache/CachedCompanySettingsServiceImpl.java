package com.tamabee.api_hr.service.company.cache;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.config.AllowanceConfig;
import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.DeductionConfig;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.PayrollConfig;
import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import com.tamabee.api_hr.exception.InternalServerException;
import com.tamabee.api_hr.repository.company.CompanySettingsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service cung cấp company settings với caching.
 * Sử dụng request-scoped cache để tránh truy vấn database nhiều lần trong cùng request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachedCompanySettingsServiceImpl implements ICachedCompanySettingsService {

    private final CompanySettingsRepository companySettingsRepository;
    private final ObjectProvider<CompanySettingsCache> settingsCacheProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public AttendanceConfig getAttendanceConfig() {
        CompanySettingEntity entity = getOrLoadEntity();
        if (entity != null && entity.getAttendanceConfig() != null) {
            return deserializeConfig(entity.getAttendanceConfig(), AttendanceConfig.class);
        }
        return AttendanceConfig.builder().build();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollConfig getPayrollConfig() {
        CompanySettingEntity entity = getOrLoadEntity();
        if (entity != null && entity.getPayrollConfig() != null) {
            return deserializeConfig(entity.getPayrollConfig(), PayrollConfig.class);
        }
        return PayrollConfig.builder().build();
    }


    @Override
    @Transactional(readOnly = true)
    public OvertimeConfig getOvertimeConfig() {
        CompanySettingEntity entity = getOrLoadEntity();
        if (entity != null && entity.getOvertimeConfig() != null) {
            return deserializeConfig(entity.getOvertimeConfig(), OvertimeConfig.class);
        }
        return OvertimeConfig.builder().build();
    }

    @Override
    @Transactional(readOnly = true)
    public AllowanceConfig getAllowanceConfig() {
        CompanySettingEntity entity = getOrLoadEntity();
        if (entity != null && entity.getAllowanceConfig() != null) {
            return deserializeConfig(entity.getAllowanceConfig(), AllowanceConfig.class);
        }
        return AllowanceConfig.builder().build();
    }

    @Override
    @Transactional(readOnly = true)
    public DeductionConfig getDeductionConfig() {
        CompanySettingEntity entity = getOrLoadEntity();
        if (entity != null && entity.getDeductionConfig() != null) {
            return deserializeConfig(entity.getDeductionConfig(), DeductionConfig.class);
        }
        return DeductionConfig.builder().build();
    }

    @Override
    @Transactional(readOnly = true)
    public BreakConfig getBreakConfig() {
        CompanySettingEntity entity = getOrLoadEntity();
        if (entity != null && entity.getBreakConfig() != null) {
            return deserializeConfig(entity.getBreakConfig(), BreakConfig.class);
        }
        return BreakConfig.builder().build();
    }

    @Override
    public void invalidateCache() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidate();
            log.info("[CACHE] Invalidated company settings cache");
        }
    }

    /**
     * Lấy cache instance, trả về null nếu không có request context
     */
    private CompanySettingsCache getCache() {
        try {
            return settingsCacheProvider.getIfAvailable();
        } catch (Exception e) {
            log.debug("No request context available for cache");
            return null;
        }
    }

    /**
     * Lấy entity từ cache hoặc load từ database
     */
    private CompanySettingEntity getOrLoadEntity() {
        CompanySettingsCache cache = getCache();
        
        // Kiểm tra cache trước
        if (cache != null && cache.isEntityQueried()) {
            log.info("[CACHE HIT] CompanySettings entity");
            return cache.getEntity();
        }

        // Load từ database
        log.info("[DATABASE] Loading CompanySettings entity");
        CompanySettingEntity entity = companySettingsRepository
                .findFirstByDeletedFalse()
                .orElse(null);

        // Lưu vào cache (kể cả null để tránh query lại)
        if (cache != null) {
            cache.putEntity(entity);
        }

        return entity;
    }

    /**
     * Deserialize JSON string thành config object
     */
    private <T> T deserializeConfig(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Lỗi deserialize config: {}", e.getMessage());
            throw new InternalServerException("Lỗi deserialize config", e);
        }
    }
}
