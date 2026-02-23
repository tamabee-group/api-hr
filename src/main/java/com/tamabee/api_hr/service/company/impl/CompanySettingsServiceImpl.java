package com.tamabee.api_hr.service.company.impl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.OvertimeMultipliers;
import com.tamabee.api_hr.dto.config.PayrollConfig;
import com.tamabee.api_hr.dto.request.attendance.AttendanceConfigRequest;
import com.tamabee.api_hr.dto.request.attendance.BreakConfigRequest;
import com.tamabee.api_hr.dto.request.payroll.OvertimeConfigRequest;
import com.tamabee.api_hr.dto.request.payroll.PayrollConfigRequest;
import com.tamabee.api_hr.dto.response.company.CompanySettingsResponse;
import com.tamabee.api_hr.entity.company.AttendanceSettingEntity;
import com.tamabee.api_hr.entity.company.BreakSettingEntity;
import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import com.tamabee.api_hr.entity.company.OvertimeSettingEntity;
import com.tamabee.api_hr.entity.company.PayrollSettingEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.mapper.company.AttendanceSettingMapper;
import com.tamabee.api_hr.mapper.company.BreakSettingMapper;
import com.tamabee.api_hr.mapper.company.CompanySettingMapper;
import com.tamabee.api_hr.mapper.company.OvertimeSettingMapper;
import com.tamabee.api_hr.mapper.company.PayrollSettingMapper;
import com.tamabee.api_hr.repository.company.AttendanceSettingRepository;
import com.tamabee.api_hr.repository.company.BreakSettingRepository;
import com.tamabee.api_hr.repository.company.CompanySettingsRepository;
import com.tamabee.api_hr.repository.company.OvertimeSettingRepository;
import com.tamabee.api_hr.repository.company.PayrollSettingRepository;
import com.tamabee.api_hr.service.calculator.LegalOvertimeRequirements;
import com.tamabee.api_hr.service.company.cache.CompanySettingsCache;
import com.tamabee.api_hr.service.company.interfaces.ICompanySettingsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation quản lý cấu hình công ty.
 * Đọc/ghi từ các bảng riêng biệt (attendance_settings, break_settings, payroll_settings, overtime_settings).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanySettingsServiceImpl implements ICompanySettingsService {

    private final CompanySettingsRepository companySettingsRepository;
    private final AttendanceSettingRepository attendanceSettingRepository;
    private final BreakSettingRepository breakSettingRepository;
    private final PayrollSettingRepository payrollSettingRepository;
    private final OvertimeSettingRepository overtimeSettingRepository;
    private final LegalOvertimeRequirements legalOvertimeRequirements;
    private final ObjectProvider<CompanySettingsCache> settingsCacheProvider;
    private final AttendanceSettingMapper attendanceSettingMapper;
    private final BreakSettingMapper breakSettingMapper;
    private final PayrollSettingMapper payrollSettingMapper;
    private final OvertimeSettingMapper overtimeSettingMapper;
    private final CompanySettingMapper companySettingMapper;
    private final DefaultSettingsInitializer defaultSettingsInitializer;

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

    /**
     * Invalidate attendance cache
     */
    private void invalidateAttendanceCache() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidateAttendance();
        }
    }

    /**
     * Invalidate break cache
     */
    private void invalidateBreakCache() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidateBreak();
        }
    }

    /**
     * Invalidate payroll cache
     */
    private void invalidatePayrollCache() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidatePayroll();
        }
    }

    /**
     * Invalidate overtime cache
     */
    private void invalidateOvertimeCache() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidateOvertime();
        }
    }

    @Override
    @Transactional
    public CompanySettingsResponse getSettings() {
        AttendanceConfig attendanceConfig = getAttendanceConfig();
        BreakConfig breakConfig = getBreakConfig();
        PayrollConfig payrollConfig = getPayrollConfig();
        OvertimeConfig overtimeConfig = getOvertimeConfig();

        return CompanySettingsResponse.builder()
                .attendanceConfig(attendanceConfig)
                .breakConfig(breakConfig)
                .payrollConfig(payrollConfig)
                .overtimeConfig(overtimeConfig)
                .build();
    }

    @Override
    @Transactional
    public AttendanceConfig updateAttendanceConfig(AttendanceConfigRequest request) {
        AttendanceSettingEntity entity = findAttendanceSetting();

        // Cập nhật entity từ request qua mapper
        attendanceSettingMapper.updateEntity(entity, request);

        // Validate config
        AttendanceConfig config = attendanceSettingMapper.toResponse(entity);
        validateAttendanceConfig(config);

        // Lưu entity
        attendanceSettingRepository.save(entity);

        // Invalidate chỉ attendance cache
        invalidateAttendanceCache();

        return config;
    }

    @Override
    @Transactional
    public PayrollConfig updatePayrollConfig(PayrollConfigRequest request) {
        PayrollSettingEntity entity = findPayrollSetting();

        // Cập nhật entity từ request qua mapper
        payrollSettingMapper.updateEntity(entity, request);

        // Lưu entity
        payrollSettingRepository.save(entity);

        // Invalidate chỉ payroll cache
        invalidatePayrollCache();

        return payrollSettingMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public OvertimeConfig updateOvertimeConfig(OvertimeConfigRequest request) {
        OvertimeSettingEntity entity = findOvertimeSetting();

        // Cập nhật entity từ request qua mapper
        overtimeSettingMapper.updateEntity(entity, request);

        // Validate config
        OvertimeConfig config = overtimeSettingMapper.toResponse(entity);
        validateOvertimeConfig(config);

        // Lưu entity
        overtimeSettingRepository.save(entity);

        // Invalidate chỉ overtime cache
        invalidateOvertimeCache();

        return config;
    }


    @Override
    @Transactional
    public void initializeDefaultSettings() {
        // Kiểm tra đã tồn tại chưa
        if (companySettingsRepository.existsByDeletedFalse()) {
            throw new ConflictException("Cấu hình công ty đã tồn tại", ErrorCode.SETTINGS_ALREADY_EXISTS);
        }

        // Tạo company_settings (chỉ còn deduction_config)
        CompanySettingEntity companySettings = companySettingMapper.toEntity();
        companySettingsRepository.save(companySettings);

        // Tạo default attendance settings
        createDefaultAttendanceSetting();

        // Tạo default break settings
        createDefaultBreakSetting();

        // Tạo default payroll settings
        createDefaultPayrollSetting();

        // Tạo default overtime settings
        createDefaultOvertimeSetting();

        log.info("Đã khởi tạo cấu hình mặc định cho tenant");
    }

    @Override
    @Transactional
    public AttendanceConfig getAttendanceConfig() {
        AttendanceSettingEntity entity = findAttendanceSetting();
        return attendanceSettingMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public PayrollConfig getPayrollConfig() {
        PayrollSettingEntity entity = findPayrollSetting();
        return payrollSettingMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public OvertimeConfig getOvertimeConfig() {
        OvertimeSettingEntity entity = findOvertimeSetting();
        return overtimeSettingMapper.toResponse(entity);
    }


    @Override
    @Transactional
    public BreakConfig getBreakConfig() {
        BreakSettingEntity entity = findBreakSetting();
        return breakSettingMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public BreakConfig updateBreakConfig(BreakConfigRequest request) {
        BreakSettingEntity entity = findBreakSetting();

        // Cập nhật entity từ request qua mapper
        breakSettingMapper.updateEntity(entity, request);

        // Validate config
        BreakConfig config = breakSettingMapper.toResponse(entity);
        validateBreakConfig(config);

        // Lưu entity
        breakSettingRepository.save(entity);

        // Invalidate chỉ break cache
        invalidateBreakCache();

        return config;
    }


    // ==================== Tìm/tạo setting entities ====================

    /**
     * Tìm attendance setting, tự động tạo nếu chưa có
     */
    private AttendanceSettingEntity findAttendanceSetting() {
        return attendanceSettingRepository.findFirstByDeletedFalse()
                .orElseGet(this::createDefaultAttendanceSetting);
    }

    /**
     * Tìm break setting, tự động tạo nếu chưa có
     */
    private BreakSettingEntity findBreakSetting() {
        return breakSettingRepository.findFirstByDeletedFalse()
                .orElseGet(this::createDefaultBreakSetting);
    }

    /**
     * Tìm payroll setting, tự động tạo nếu chưa có
     */
    private PayrollSettingEntity findPayrollSetting() {
        return payrollSettingRepository.findFirstByDeletedFalse()
                .orElseGet(this::createDefaultPayrollSetting);
    }

    /**
     * Tìm overtime setting, tự động tạo nếu chưa có
     */
    private OvertimeSettingEntity findOvertimeSetting() {
        return overtimeSettingRepository.findFirstByDeletedFalse()
                .orElseGet(this::createDefaultOvertimeSetting);
    }

    // ==================== Tạo default settings ====================

    /**
     * Tạo default attendance setting (delegate sang REQUIRES_NEW transaction)
     */
    private AttendanceSettingEntity createDefaultAttendanceSetting() {
        return defaultSettingsInitializer.createDefaultAttendanceSetting();
    }

    /**
     * Tạo default break setting (delegate sang REQUIRES_NEW transaction)
     */
    private BreakSettingEntity createDefaultBreakSetting() {
        return defaultSettingsInitializer.createDefaultBreakSetting();
    }

    /**
     * Tạo default payroll setting (delegate sang REQUIRES_NEW transaction)
     */
    private PayrollSettingEntity createDefaultPayrollSetting() {
        return defaultSettingsInitializer.createDefaultPayrollSetting();
    }

    /**
     * Tạo default overtime setting (delegate sang REQUIRES_NEW transaction)
     */
    private OvertimeSettingEntity createDefaultOvertimeSetting() {
        return defaultSettingsInitializer.createDefaultOvertimeSetting();
    }

    /**
     * Tạo default company settings (delegate sang REQUIRES_NEW transaction)
     */
    private CompanySettingEntity createDefaultCompanySettings() {
        return defaultSettingsInitializer.createDefaultCompanySettings();
    }


    // ==================== Validation ====================

    /**
     * Validate attendance config
     */
    private void validateAttendanceConfig(AttendanceConfig config) {
        if (config.getDefaultWorkStartTime() != null && config.getDefaultWorkEndTime() != null) {
            if (!config.getDefaultWorkStartTime().isBefore(config.getDefaultWorkEndTime())) {
                throw new BadRequestException("Giờ bắt đầu phải trước giờ kết thúc", ErrorCode.INVALID_WORK_TIME);
            }
        }
    }

    /**
     * Validate break config
     */
    private void validateBreakConfig(BreakConfig config) {
        // Đơn giản: chỉ validate defaultBreakMinutes > 0
        if (config.getDefaultBreakMinutes() != null && config.getDefaultBreakMinutes() < 0) {
            throw new BadRequestException(
                    "Thời gian giải lao mặc định không hợp lệ",
                    ErrorCode.INVALID_BREAK_CONFIG);
        }
    }

    /**
     * Validate overtime config
     */
    private void validateOvertimeConfig(OvertimeConfig config) {
        // Validate legal minimum compliance nếu useLegalMinimum = true
        if (Boolean.TRUE.equals(config.getUseLegalMinimum())) {
            String region = config.getRegion() != null ? config.getRegion() : "ja";
            OvertimeMultipliers legalMinimum = legalOvertimeRequirements.getMinimumMultipliers(region);

            if (config.getRegularOvertimeRate() != null &&
                    config.getRegularOvertimeRate().compareTo(legalMinimum.getRegularOvertime()) < 0) {
                throw new BadRequestException(
                        "Hệ số tăng ca thường không đạt yêu cầu pháp luật (" + legalMinimum.getRegularOvertime() + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }

            if (config.getNightWorkRate() != null &&
                    config.getNightWorkRate().compareTo(legalMinimum.getNightWork()) < 0) {
                throw new BadRequestException(
                        "Hệ số làm đêm không đạt yêu cầu pháp luật (" + legalMinimum.getNightWork() + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }

            if (config.getNightOvertimeRate() != null &&
                    config.getNightOvertimeRate().compareTo(legalMinimum.getNightOvertime()) < 0) {
                throw new BadRequestException(
                        "Hệ số tăng ca đêm không đạt yêu cầu pháp luật (" + legalMinimum.getNightOvertime() + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }

            if (config.getHolidayOvertimeRate() != null &&
                    config.getHolidayOvertimeRate().compareTo(legalMinimum.getHolidayOvertime()) < 0) {
                throw new BadRequestException(
                        "Hệ số tăng ca ngày lễ không đạt yêu cầu pháp luật (" + legalMinimum.getHolidayOvertime() + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }

            if (config.getHolidayNightOvertimeRate() != null &&
                    config.getHolidayNightOvertimeRate().compareTo(legalMinimum.getHolidayNightOvertime()) < 0) {
                throw new BadRequestException(
                        "Hệ số tăng ca đêm ngày lễ không đạt yêu cầu pháp luật ("
                                + legalMinimum.getHolidayNightOvertime() + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }

            if (config.getWeekendOvertimeRate() != null &&
                    config.getWeekendOvertimeRate().compareTo(legalMinimum.getWeekendOvertime()) < 0) {
                throw new BadRequestException(
                        "Hệ số tăng ca cuối tuần không đạt yêu cầu pháp luật (" + legalMinimum.getWeekendOvertime()
                                + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }
        }
    }

}
