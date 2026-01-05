package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.dto.request.SettingUpdateRequest;
import com.tamabee.api_hr.dto.response.SettingResponse;
import com.tamabee.api_hr.entity.wallet.TamabeeSettingEntity;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.admin.TamabeeSettingMapper;
import com.tamabee.api_hr.repository.TamabeeSettingRepository;
import com.tamabee.api_hr.service.admin.ISettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service quản lý cấu hình hệ thống Tamabee
 * Sử dụng in-memory cache để tối ưu performance cho các giá trị thường xuyên
 * truy cập
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettingServiceImpl implements ISettingService {

    private final TamabeeSettingRepository settingRepository;
    private final TamabeeSettingMapper settingMapper;

    // Setting keys
    private static final String FREE_TRIAL_MONTHS = "FREE_TRIAL_MONTHS";
    private static final String REFERRAL_BONUS_MONTHS = "REFERRAL_BONUS_MONTHS";
    private static final String COMMISSION_RATE = "COMMISSION_RATE";

    // Default values
    private static final int DEFAULT_FREE_TRIAL_MONTHS = 2;
    private static final int DEFAULT_REFERRAL_BONUS_MONTHS = 1;
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.10");

    // In-memory cache cho các giá trị thường xuyên truy cập
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public SettingResponse get(String key) {
        TamabeeSettingEntity entity = settingRepository.findBySettingKeyAndDeletedFalse(key)
                .orElseThrow(() -> NotFoundException.setting(key));
        return settingMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public SettingResponse update(String key, SettingUpdateRequest request) {
        TamabeeSettingEntity entity = settingRepository.findBySettingKeyAndDeletedFalse(key)
                .orElseThrow(() -> NotFoundException.setting(key));

        settingMapper.updateEntity(entity, request);
        TamabeeSettingEntity savedEntity = settingRepository.save(entity);

        // Invalidate cache cho key này
        invalidateCache(key);

        log.info("Cập nhật setting {} thành công, giá trị mới: {}", key, request.getSettingValue());

        return settingMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettingResponse> getAll() {
        List<TamabeeSettingEntity> entities = settingRepository.findByDeletedFalseOrderByIdAsc();
        return entities.stream()
                .map(settingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public int getFreeTrialMonths() {
        return getCachedIntValue(FREE_TRIAL_MONTHS, DEFAULT_FREE_TRIAL_MONTHS);
    }

    @Override
    public int getReferralBonusMonths() {
        return getCachedIntValue(REFERRAL_BONUS_MONTHS, DEFAULT_REFERRAL_BONUS_MONTHS);
    }

    @Override
    public BigDecimal getCommissionRate() {
        return getCachedDecimalValue(COMMISSION_RATE, DEFAULT_COMMISSION_RATE);
    }

    /**
     * Lấy giá trị integer từ cache, nếu không có thì load từ database
     */
    private int getCachedIntValue(String key, int defaultValue) {
        return (int) cache.computeIfAbsent(key, k -> loadIntValue(k, defaultValue));
    }

    /**
     * Lấy giá trị BigDecimal từ cache, nếu không có thì load từ database
     */
    private BigDecimal getCachedDecimalValue(String key, BigDecimal defaultValue) {
        return (BigDecimal) cache.computeIfAbsent(key, k -> loadDecimalValue(k, defaultValue));
    }

    /**
     * Load giá trị integer từ database
     */
    private int loadIntValue(String key, int defaultValue) {
        return settingRepository.findBySettingKeyAndDeletedFalse(key)
                .map(entity -> {
                    try {
                        return Integer.parseInt(entity.getSettingValue());
                    } catch (NumberFormatException e) {
                        log.warn("Không thể parse giá trị {} cho key {}, sử dụng default: {}",
                                entity.getSettingValue(), key, defaultValue);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * Load giá trị BigDecimal từ database
     */
    private BigDecimal loadDecimalValue(String key, BigDecimal defaultValue) {
        return settingRepository.findBySettingKeyAndDeletedFalse(key)
                .map(entity -> {
                    try {
                        return new BigDecimal(entity.getSettingValue());
                    } catch (NumberFormatException e) {
                        log.warn("Không thể parse giá trị {} cho key {}, sử dụng default: {}",
                                entity.getSettingValue(), key, defaultValue);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * Invalidate cache cho một key cụ thể
     */
    private void invalidateCache(String key) {
        cache.remove(key);
        log.debug("Đã invalidate cache cho key: {}", key);
    }
}
