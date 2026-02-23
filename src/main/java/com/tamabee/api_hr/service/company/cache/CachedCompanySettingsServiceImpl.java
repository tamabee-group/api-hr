package com.tamabee.api_hr.service.company.cache;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.PayrollConfig;
import com.tamabee.api_hr.entity.company.AttendanceSettingEntity;
import com.tamabee.api_hr.entity.company.BreakSettingEntity;
import com.tamabee.api_hr.entity.company.OvertimeSettingEntity;
import com.tamabee.api_hr.entity.company.PayrollSettingEntity;
import com.tamabee.api_hr.mapper.company.AttendanceSettingMapper;
import com.tamabee.api_hr.mapper.company.BreakSettingMapper;
import com.tamabee.api_hr.mapper.company.OvertimeSettingMapper;
import com.tamabee.api_hr.mapper.company.PayrollSettingMapper;
import com.tamabee.api_hr.repository.company.AttendanceSettingRepository;
import com.tamabee.api_hr.repository.company.BreakSettingRepository;
import com.tamabee.api_hr.repository.company.OvertimeSettingRepository;
import com.tamabee.api_hr.repository.company.PayrollSettingRepository;
import com.tamabee.api_hr.service.company.interfaces.ICompanySettingsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service cung cấp company settings với per-entity caching.
 * Cache từng setting entity riêng biệt trong request scope.
 * Khi đọc: kiểm tra cache trước, nếu chưa có thì query DB và lưu cache.
 * Khi invalidate: chỉ xóa cache của entity type tương ứng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachedCompanySettingsServiceImpl implements ICachedCompanySettingsService {

    private final ICompanySettingsService companySettingsService;
    private final ObjectProvider<CompanySettingsCache> settingsCacheProvider;
    private final AttendanceSettingRepository attendanceSettingRepository;
    private final BreakSettingRepository breakSettingRepository;
    private final PayrollSettingRepository payrollSettingRepository;
    private final OvertimeSettingRepository overtimeSettingRepository;
    private final AttendanceSettingMapper attendanceSettingMapper;
    private final BreakSettingMapper breakSettingMapper;
    private final PayrollSettingMapper payrollSettingMapper;
    private final OvertimeSettingMapper overtimeSettingMapper;

    /**
     * Lấy cache instance, trả về null nếu không có request context
     */
    private CompanySettingsCache getCache() {
        try {
            return settingsCacheProvider.getIfAvailable();
        } catch (Exception e) {
            log.debug("Không có request context cho cache");
            return null;
        }
    }

    @Override
    @Transactional
    public AttendanceConfig getAttendanceConfig() {
        CompanySettingsCache cache = getCache();

        // Kiểm tra cache
        if (cache != null && cache.isAttendanceQueried()) {
            AttendanceSettingEntity cached = cache.getAttendanceSetting();
            if (cached != null) {
                log.debug("[CACHE HIT] AttendanceConfig");
                return attendanceSettingMapper.toResponse(cached);
            }
        }

        // Cache miss - delegate sang service để query DB
        AttendanceConfig config = companySettingsService.getAttendanceConfig();

        // Lưu entity vào cache nếu có
        if (cache != null) {
            AttendanceSettingEntity entity = attendanceSettingRepository.findFirstByDeletedFalse().orElse(null);
            if (entity != null) {
                cache.putAttendanceSetting(entity);
            }
            log.debug("[CACHE MISS] AttendanceConfig - đã cache");
        }

        return config;
    }

    @Override
    @Transactional
    public BreakConfig getBreakConfig() {
        CompanySettingsCache cache = getCache();

        // Kiểm tra cache
        if (cache != null && cache.isBreakQueried()) {
            BreakSettingEntity cached = cache.getBreakSetting();
            if (cached != null) {
                log.debug("[CACHE HIT] BreakConfig");
                return breakSettingMapper.toResponse(cached);
            }
        }

        // Cache miss - delegate sang service để query DB
        BreakConfig config = companySettingsService.getBreakConfig();

        // Lưu entity vào cache nếu có
        if (cache != null) {
            BreakSettingEntity entity = breakSettingRepository.findFirstByDeletedFalse().orElse(null);
            if (entity != null) {
                cache.putBreakSetting(entity);
            }
            log.debug("[CACHE MISS] BreakConfig - đã cache");
        }

        return config;
    }

    @Override
    @Transactional
    public PayrollConfig getPayrollConfig() {
        CompanySettingsCache cache = getCache();

        // Kiểm tra cache
        if (cache != null && cache.isPayrollQueried()) {
            PayrollSettingEntity cached = cache.getPayrollSetting();
            if (cached != null) {
                log.debug("[CACHE HIT] PayrollConfig");
                return payrollSettingMapper.toResponse(cached);
            }
        }

        // Cache miss - delegate sang service để query DB
        PayrollConfig config = companySettingsService.getPayrollConfig();

        // Lưu entity vào cache nếu có
        if (cache != null) {
            PayrollSettingEntity entity = payrollSettingRepository.findFirstByDeletedFalse().orElse(null);
            if (entity != null) {
                cache.putPayrollSetting(entity);
            }
            log.debug("[CACHE MISS] PayrollConfig - đã cache");
        }

        return config;
    }

    @Override
    @Transactional
    public OvertimeConfig getOvertimeConfig() {
        CompanySettingsCache cache = getCache();

        // Kiểm tra cache
        if (cache != null && cache.isOvertimeQueried()) {
            OvertimeSettingEntity cached = cache.getOvertimeSetting();
            if (cached != null) {
                log.debug("[CACHE HIT] OvertimeConfig");
                return overtimeSettingMapper.toResponse(cached);
            }
        }

        // Cache miss - delegate sang service để query DB
        OvertimeConfig config = companySettingsService.getOvertimeConfig();

        // Lưu entity vào cache nếu có
        if (cache != null) {
            OvertimeSettingEntity entity = overtimeSettingRepository.findFirstByDeletedFalse().orElse(null);
            if (entity != null) {
                cache.putOvertimeSetting(entity);
            }
            log.debug("[CACHE MISS] OvertimeConfig - đã cache");
        }

        return config;
    }


    // ==================== Invalidate methods ====================

    @Override
    public void invalidateCache() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidateAll();
        }
        log.info("[CACHE] Invalidated toàn bộ company settings cache");
    }

    @Override
    public void invalidateAttendanceCache() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidateAttendance();
        }
        log.info("[CACHE] Invalidated attendance settings cache");
    }

    @Override
    public void invalidateBreakCache() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidateBreak();
        }
        log.info("[CACHE] Invalidated break settings cache");
    }

    @Override
    public void invalidatePayrollCache() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidatePayroll();
        }
        log.info("[CACHE] Invalidated payroll settings cache");
    }

    @Override
    public void invalidateOvertimeCache() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidateOvertime();
        }
        log.info("[CACHE] Invalidated overtime settings cache");
    }
}
