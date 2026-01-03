package com.tamabee.api_hr.service.company.cache;

import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.enums.BreakType;
import com.tamabee.api_hr.enums.SalaryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Cung cấp default values cho các config khi chưa được cấu hình.
 * Log warning khi sử dụng default values để admin biết cần cấu hình.
 */
@Slf4j
@Component
public class DefaultSettingsProvider {

    /**
     * Lấy AttendanceConfig mặc định
     */
    public AttendanceConfig getDefaultAttendanceConfig(Long companyId) {
        log.warn("Sử dụng AttendanceConfig mặc định cho companyId: {}. Vui lòng cấu hình trong settings.", companyId);
        return AttendanceConfig.builder()
                .defaultWorkStartTime(LocalTime.of(9, 0))
                .defaultWorkEndTime(LocalTime.of(18, 0))
                .defaultBreakMinutes(60)
                .enableRounding(false)
                .enableCheckInRounding(false)
                .enableCheckOutRounding(false)
                .enableBreakStartRounding(false)
                .enableBreakEndRounding(false)
                .lateGraceMinutes(0)
                .earlyLeaveGraceMinutes(0)
                .requireDeviceRegistration(false)
                .requireGeoLocation(false)
                .geoFenceRadiusMeters(100)
                .allowMobileCheckIn(true)
                .allowWebCheckIn(true)
                .build();
    }

    /**
     * Lấy PayrollConfig mặc định
     */
    public PayrollConfig getDefaultPayrollConfig(Long companyId) {
        log.warn("Sử dụng PayrollConfig mặc định cho companyId: {}. Vui lòng cấu hình trong settings.", companyId);
        return PayrollConfig.builder()
                .defaultSalaryType(SalaryType.MONTHLY)
                .payDay(25)
                .cutoffDay(20)
                .standardWorkingDaysPerMonth(22)
                .standardWorkingHoursPerDay(8)
                .build();
    }

    /**
     * Lấy OvertimeConfig mặc định
     */
    public OvertimeConfig getDefaultOvertimeConfig(Long companyId) {
        log.warn("Sử dụng OvertimeConfig mặc định cho companyId: {}. Vui lòng cấu hình trong settings.", companyId);
        return OvertimeConfig.builder()
                .overtimeEnabled(false)
                .requireApproval(true)
                .regularOvertimeRate(BigDecimal.valueOf(1.25))
                .nightOvertimeRate(BigDecimal.valueOf(1.5))
                .holidayOvertimeRate(BigDecimal.valueOf(1.35))
                .weekendOvertimeRate(BigDecimal.valueOf(1.35))
                .nightStartTime(LocalTime.of(22, 0))
                .nightEndTime(LocalTime.of(5, 0))
                .maxOvertimeHoursPerDay(4)
                .maxOvertimeHoursPerMonth(45)
                .build();
    }

    /**
     * Lấy AllowanceConfig mặc định
     */
    public AllowanceConfig getDefaultAllowanceConfig(Long companyId) {
        log.warn("Sử dụng AllowanceConfig mặc định cho companyId: {}. Vui lòng cấu hình trong settings.", companyId);
        return AllowanceConfig.builder()
                .build();
    }

    /**
     * Lấy DeductionConfig mặc định
     */
    public DeductionConfig getDefaultDeductionConfig(Long companyId) {
        log.warn("Sử dụng DeductionConfig mặc định cho companyId: {}. Vui lòng cấu hình trong settings.", companyId);
        return DeductionConfig.builder()
                .enableLatePenalty(false)
                .latePenaltyPerMinute(BigDecimal.ZERO)
                .enableEarlyLeavePenalty(false)
                .earlyLeavePenaltyPerMinute(BigDecimal.ZERO)
                .enableAbsenceDeduction(false)
                .build();
    }

    /**
     * Lấy BreakConfig mặc định
     */
    public BreakConfig getDefaultBreakConfig(Long companyId) {
        log.warn("Sử dụng BreakConfig mặc định cho companyId: {}. Vui lòng cấu hình trong settings.", companyId);
        return BreakConfig.builder()
                .breakEnabled(true)
                .breakType(BreakType.PAID)
                .defaultBreakMinutes(60)
                .minimumBreakMinutes(45)
                .maximumBreakMinutes(90)
                .useLegalMinimum(true)
                .breakTrackingEnabled(false)
                .locale("ja")
                .fixedBreakMode(false)
                .maxBreaksPerDay(3)
                .build();
    }

    /**
     * Merge config với default values - điền các field null bằng default
     */
    public AttendanceConfig mergeWithDefaults(AttendanceConfig config, Long companyId) {
        if (config == null) {
            return getDefaultAttendanceConfig(companyId);
        }

        AttendanceConfig defaults = getDefaultAttendanceConfig(companyId);
        boolean hasNullFields = false;

        if (config.getDefaultWorkStartTime() == null) {
            config.setDefaultWorkStartTime(defaults.getDefaultWorkStartTime());
            hasNullFields = true;
        }
        if (config.getDefaultWorkEndTime() == null) {
            config.setDefaultWorkEndTime(defaults.getDefaultWorkEndTime());
            hasNullFields = true;
        }
        if (config.getDefaultBreakMinutes() == null) {
            config.setDefaultBreakMinutes(defaults.getDefaultBreakMinutes());
            hasNullFields = true;
        }
        if (config.getEnableRounding() == null) {
            config.setEnableRounding(defaults.getEnableRounding());
            hasNullFields = true;
        }
        if (config.getLateGraceMinutes() == null) {
            config.setLateGraceMinutes(defaults.getLateGraceMinutes());
            hasNullFields = true;
        }
        if (config.getEarlyLeaveGraceMinutes() == null) {
            config.setEarlyLeaveGraceMinutes(defaults.getEarlyLeaveGraceMinutes());
            hasNullFields = true;
        }
        if (config.getRequireDeviceRegistration() == null) {
            config.setRequireDeviceRegistration(defaults.getRequireDeviceRegistration());
            hasNullFields = true;
        }
        if (config.getRequireGeoLocation() == null) {
            config.setRequireGeoLocation(defaults.getRequireGeoLocation());
            hasNullFields = true;
        }
        if (config.getGeoFenceRadiusMeters() == null) {
            config.setGeoFenceRadiusMeters(defaults.getGeoFenceRadiusMeters());
            hasNullFields = true;
        }
        if (config.getAllowMobileCheckIn() == null) {
            config.setAllowMobileCheckIn(defaults.getAllowMobileCheckIn());
            hasNullFields = true;
        }
        if (config.getAllowWebCheckIn() == null) {
            config.setAllowWebCheckIn(defaults.getAllowWebCheckIn());
            hasNullFields = true;
        }

        if (hasNullFields) {
            log.debug("Đã merge AttendanceConfig với default values cho companyId: {}", companyId);
        }

        return config;
    }

    /**
     * Merge PayrollConfig với default values
     */
    public PayrollConfig mergeWithDefaults(PayrollConfig config, Long companyId) {
        if (config == null) {
            return getDefaultPayrollConfig(companyId);
        }

        PayrollConfig defaults = getDefaultPayrollConfig(companyId);
        boolean hasNullFields = false;

        if (config.getDefaultSalaryType() == null) {
            config.setDefaultSalaryType(defaults.getDefaultSalaryType());
            hasNullFields = true;
        }
        if (config.getPayDay() == null) {
            config.setPayDay(defaults.getPayDay());
            hasNullFields = true;
        }
        if (config.getCutoffDay() == null) {
            config.setCutoffDay(defaults.getCutoffDay());
            hasNullFields = true;
        }
        if (config.getStandardWorkingDaysPerMonth() == null) {
            config.setStandardWorkingDaysPerMonth(defaults.getStandardWorkingDaysPerMonth());
            hasNullFields = true;
        }
        if (config.getStandardWorkingHoursPerDay() == null) {
            config.setStandardWorkingHoursPerDay(defaults.getStandardWorkingHoursPerDay());
            hasNullFields = true;
        }

        if (hasNullFields) {
            log.debug("Đã merge PayrollConfig với default values cho companyId: {}", companyId);
        }

        return config;
    }

    /**
     * Merge OvertimeConfig với default values
     */
    public OvertimeConfig mergeWithDefaults(OvertimeConfig config, Long companyId) {
        if (config == null) {
            return getDefaultOvertimeConfig(companyId);
        }

        OvertimeConfig defaults = getDefaultOvertimeConfig(companyId);
        boolean hasNullFields = false;

        if (config.getOvertimeEnabled() == null) {
            config.setOvertimeEnabled(defaults.getOvertimeEnabled());
            hasNullFields = true;
        }
        if (config.getRequireApproval() == null) {
            config.setRequireApproval(defaults.getRequireApproval());
            hasNullFields = true;
        }
        if (config.getRegularOvertimeRate() == null) {
            config.setRegularOvertimeRate(defaults.getRegularOvertimeRate());
            hasNullFields = true;
        }
        if (config.getNightOvertimeRate() == null) {
            config.setNightOvertimeRate(defaults.getNightOvertimeRate());
            hasNullFields = true;
        }
        if (config.getHolidayOvertimeRate() == null) {
            config.setHolidayOvertimeRate(defaults.getHolidayOvertimeRate());
            hasNullFields = true;
        }
        if (config.getWeekendOvertimeRate() == null) {
            config.setWeekendOvertimeRate(defaults.getWeekendOvertimeRate());
            hasNullFields = true;
        }
        if (config.getNightStartTime() == null) {
            config.setNightStartTime(defaults.getNightStartTime());
            hasNullFields = true;
        }
        if (config.getNightEndTime() == null) {
            config.setNightEndTime(defaults.getNightEndTime());
            hasNullFields = true;
        }
        if (config.getMaxOvertimeHoursPerDay() == null) {
            config.setMaxOvertimeHoursPerDay(defaults.getMaxOvertimeHoursPerDay());
            hasNullFields = true;
        }
        if (config.getMaxOvertimeHoursPerMonth() == null) {
            config.setMaxOvertimeHoursPerMonth(defaults.getMaxOvertimeHoursPerMonth());
            hasNullFields = true;
        }

        if (hasNullFields) {
            log.debug("Đã merge OvertimeConfig với default values cho companyId: {}", companyId);
        }

        return config;
    }

    /**
     * Merge BreakConfig với default values
     */
    public BreakConfig mergeWithDefaults(BreakConfig config, Long companyId) {
        if (config == null) {
            return getDefaultBreakConfig(companyId);
        }

        BreakConfig defaults = getDefaultBreakConfig(companyId);
        boolean hasNullFields = false;

        if (config.getBreakEnabled() == null) {
            config.setBreakEnabled(defaults.getBreakEnabled());
            hasNullFields = true;
        }
        if (config.getBreakType() == null) {
            config.setBreakType(defaults.getBreakType());
            hasNullFields = true;
        }
        if (config.getDefaultBreakMinutes() == null) {
            config.setDefaultBreakMinutes(defaults.getDefaultBreakMinutes());
            hasNullFields = true;
        }
        if (config.getMinimumBreakMinutes() == null) {
            config.setMinimumBreakMinutes(defaults.getMinimumBreakMinutes());
            hasNullFields = true;
        }
        if (config.getMaximumBreakMinutes() == null) {
            config.setMaximumBreakMinutes(defaults.getMaximumBreakMinutes());
            hasNullFields = true;
        }
        if (config.getUseLegalMinimum() == null) {
            config.setUseLegalMinimum(defaults.getUseLegalMinimum());
            hasNullFields = true;
        }
        if (config.getBreakTrackingEnabled() == null) {
            config.setBreakTrackingEnabled(defaults.getBreakTrackingEnabled());
            hasNullFields = true;
        }
        if (config.getLocale() == null) {
            config.setLocale(defaults.getLocale());
            hasNullFields = true;
        }
        if (config.getFixedBreakMode() == null) {
            config.setFixedBreakMode(defaults.getFixedBreakMode());
            hasNullFields = true;
        }
        if (config.getMaxBreaksPerDay() == null) {
            config.setMaxBreaksPerDay(defaults.getMaxBreaksPerDay());
            hasNullFields = true;
        }

        if (hasNullFields) {
            log.debug("Đã merge BreakConfig với default values cho companyId: {}", companyId);
        }

        return config;
    }

    /**
     * Merge DeductionConfig với default values
     */
    public DeductionConfig mergeWithDefaults(DeductionConfig config, Long companyId) {
        if (config == null) {
            return getDefaultDeductionConfig(companyId);
        }

        DeductionConfig defaults = getDefaultDeductionConfig(companyId);
        boolean hasNullFields = false;

        if (config.getEnableLatePenalty() == null) {
            config.setEnableLatePenalty(defaults.getEnableLatePenalty());
            hasNullFields = true;
        }
        if (config.getLatePenaltyPerMinute() == null) {
            config.setLatePenaltyPerMinute(defaults.getLatePenaltyPerMinute());
            hasNullFields = true;
        }
        if (config.getEnableEarlyLeavePenalty() == null) {
            config.setEnableEarlyLeavePenalty(defaults.getEnableEarlyLeavePenalty());
            hasNullFields = true;
        }
        if (config.getEarlyLeavePenaltyPerMinute() == null) {
            config.setEarlyLeavePenaltyPerMinute(defaults.getEarlyLeavePenaltyPerMinute());
            hasNullFields = true;
        }
        if (config.getEnableAbsenceDeduction() == null) {
            config.setEnableAbsenceDeduction(defaults.getEnableAbsenceDeduction());
            hasNullFields = true;
        }

        if (hasNullFields) {
            log.debug("Đã merge DeductionConfig với default values cho companyId: {}", companyId);
        }

        return config;
    }

    /**
     * Merge AllowanceConfig với default values
     */
    public AllowanceConfig mergeWithDefaults(AllowanceConfig config, Long companyId) {
        if (config == null) {
            return getDefaultAllowanceConfig(companyId);
        }
        // AllowanceConfig chủ yếu là list allowances, không có nhiều default fields
        return config;
    }
}
