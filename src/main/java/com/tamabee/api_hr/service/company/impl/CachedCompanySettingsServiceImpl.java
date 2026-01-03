package com.tamabee.api_hr.service.company.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import com.tamabee.api_hr.exception.InternalServerException;
import com.tamabee.api_hr.repository.CompanySettingsRepository;
import com.tamabee.api_hr.service.company.ICachedCompanySettingsService;
import com.tamabee.api_hr.service.company.cache.CompanySettingsCache;
import com.tamabee.api_hr.service.company.cache.DefaultSettingsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service cung cấp company settings với caching và fallback to defaults.
 * Sử dụng request-scoped cache để tránh truy vấn database nhiều lần trong cùng
 * request.
 * Khi không có request context (ví dụ: scheduled tasks), sẽ query trực tiếp
 * database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachedCompanySettingsServiceImpl implements ICachedCompanySettingsService {

    private final CompanySettingsRepository companySettingsRepository;
    private final ObjectProvider<CompanySettingsCache> settingsCacheProvider;
    private final DefaultSettingsProvider defaultSettingsProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public AttendanceConfig getAttendanceConfig(Long companyId) {
        CompanySettingsCache cache = getCache();

        // Kiểm tra cache trước (nếu có)
        if (cache != null) {
            AttendanceConfig cached = cache.getAttendanceConfig(companyId);
            if (cached != null) {
                log.debug("Cache hit: AttendanceConfig for companyId: {}", companyId);
                return cached;
            }
        }

        // Lấy từ database
        CompanySettingEntity entity = getOrLoadEntity(companyId, cache);
        AttendanceConfig config = null;

        if (entity != null && entity.getAttendanceConfig() != null) {
            config = deserializeConfig(entity.getAttendanceConfig(), AttendanceConfig.class);
        }

        // Merge với defaults để đảm bảo không có null fields
        config = defaultSettingsProvider.mergeWithDefaults(config, companyId);

        // Lưu vào cache (nếu có)
        if (cache != null) {
            cache.putAttendanceConfig(companyId, config);
            log.debug("Loaded and cached AttendanceConfig for companyId: {}", companyId);
        }

        return config;
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollConfig getPayrollConfig(Long companyId) {
        CompanySettingsCache cache = getCache();

        // Kiểm tra cache trước
        if (cache != null) {
            PayrollConfig cached = cache.getPayrollConfig(companyId);
            if (cached != null) {
                log.debug("Cache hit: PayrollConfig for companyId: {}", companyId);
                return cached;
            }
        }

        // Lấy từ database
        CompanySettingEntity entity = getOrLoadEntity(companyId, cache);
        PayrollConfig config = null;

        if (entity != null && entity.getPayrollConfig() != null) {
            config = deserializeConfig(entity.getPayrollConfig(), PayrollConfig.class);
        }

        // Merge với defaults
        config = defaultSettingsProvider.mergeWithDefaults(config, companyId);

        // Lưu vào cache
        if (cache != null) {
            cache.putPayrollConfig(companyId, config);
            log.debug("Loaded and cached PayrollConfig for companyId: {}", companyId);
        }

        return config;
    }

    @Override
    @Transactional(readOnly = true)
    public OvertimeConfig getOvertimeConfig(Long companyId) {
        CompanySettingsCache cache = getCache();

        // Kiểm tra cache trước
        if (cache != null) {
            OvertimeConfig cached = cache.getOvertimeConfig(companyId);
            if (cached != null) {
                log.debug("Cache hit: OvertimeConfig for companyId: {}", companyId);
                return cached;
            }
        }

        // Lấy từ database
        CompanySettingEntity entity = getOrLoadEntity(companyId, cache);
        OvertimeConfig config = null;

        if (entity != null && entity.getOvertimeConfig() != null) {
            config = deserializeConfig(entity.getOvertimeConfig(), OvertimeConfig.class);
        }

        // Merge với defaults
        config = defaultSettingsProvider.mergeWithDefaults(config, companyId);

        // Lưu vào cache
        if (cache != null) {
            cache.putOvertimeConfig(companyId, config);
            log.debug("Loaded and cached OvertimeConfig for companyId: {}", companyId);
        }

        return config;
    }

    @Override
    @Transactional(readOnly = true)
    public AllowanceConfig getAllowanceConfig(Long companyId) {
        CompanySettingsCache cache = getCache();

        // Kiểm tra cache trước
        if (cache != null) {
            AllowanceConfig cached = cache.getAllowanceConfig(companyId);
            if (cached != null) {
                log.debug("Cache hit: AllowanceConfig for companyId: {}", companyId);
                return cached;
            }
        }

        // Lấy từ database
        CompanySettingEntity entity = getOrLoadEntity(companyId, cache);
        AllowanceConfig config = null;

        if (entity != null && entity.getAllowanceConfig() != null) {
            config = deserializeConfig(entity.getAllowanceConfig(), AllowanceConfig.class);
        }

        // Merge với defaults
        config = defaultSettingsProvider.mergeWithDefaults(config, companyId);

        // Lưu vào cache
        if (cache != null) {
            cache.putAllowanceConfig(companyId, config);
            log.debug("Loaded and cached AllowanceConfig for companyId: {}", companyId);
        }

        return config;
    }

    @Override
    @Transactional(readOnly = true)
    public DeductionConfig getDeductionConfig(Long companyId) {
        CompanySettingsCache cache = getCache();

        // Kiểm tra cache trước
        if (cache != null) {
            DeductionConfig cached = cache.getDeductionConfig(companyId);
            if (cached != null) {
                log.debug("Cache hit: DeductionConfig for companyId: {}", companyId);
                return cached;
            }
        }

        // Lấy từ database
        CompanySettingEntity entity = getOrLoadEntity(companyId, cache);
        DeductionConfig config = null;

        if (entity != null && entity.getDeductionConfig() != null) {
            config = deserializeConfig(entity.getDeductionConfig(), DeductionConfig.class);
        }

        // Merge với defaults
        config = defaultSettingsProvider.mergeWithDefaults(config, companyId);

        // Lưu vào cache
        if (cache != null) {
            cache.putDeductionConfig(companyId, config);
            log.debug("Loaded and cached DeductionConfig for companyId: {}", companyId);
        }

        return config;
    }

    @Override
    @Transactional(readOnly = true)
    public BreakConfig getBreakConfig(Long companyId) {
        CompanySettingsCache cache = getCache();

        // Kiểm tra cache trước
        if (cache != null) {
            BreakConfig cached = cache.getBreakConfig(companyId);
            if (cached != null) {
                log.debug("Cache hit: BreakConfig for companyId: {}", companyId);
                return cached;
            }
        }

        // Lấy từ database
        CompanySettingEntity entity = getOrLoadEntity(companyId, cache);
        BreakConfig config = null;

        if (entity != null && entity.getBreakConfig() != null) {
            config = deserializeConfig(entity.getBreakConfig(), BreakConfig.class);
        }

        // Merge với defaults
        config = defaultSettingsProvider.mergeWithDefaults(config, companyId);

        // Lưu vào cache
        if (cache != null) {
            cache.putBreakConfig(companyId, config);
            log.debug("Loaded and cached BreakConfig for companyId: {}", companyId);
        }

        return config;
    }

    @Override
    public void invalidateCache(Long companyId) {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidate(companyId);
            log.info("Invalidated settings cache for companyId: {}", companyId);
        }
    }

    /**
     * Lấy cache instance, trả về null nếu không có request context
     */
    private CompanySettingsCache getCache() {
        try {
            return settingsCacheProvider.getIfAvailable();
        } catch (Exception e) {
            log.debug("No request context available for cache, will query database directly");
            return null;
        }
    }

    /**
     * Lấy entity từ cache hoặc load từ database
     */
    private CompanySettingEntity getOrLoadEntity(Long companyId, CompanySettingsCache cache) {
        // Kiểm tra entity cache trước
        if (cache != null && cache.isEntityQueried(companyId)) {
            return cache.getEntity(companyId);
        }

        // Load từ database
        CompanySettingEntity entity = companySettingsRepository
                .findByCompanyIdAndDeletedFalse(companyId)
                .orElse(null);

        if (entity == null) {
            log.warn("Không tìm thấy CompanySettings cho companyId: {}. Sử dụng default values.", companyId);
        }

        // Lưu vào cache (kể cả null để tránh query lại)
        if (cache != null) {
            cache.putEntity(companyId, entity);
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
