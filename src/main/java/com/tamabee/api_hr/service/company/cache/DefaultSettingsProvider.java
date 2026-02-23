package com.tamabee.api_hr.service.company.cache;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.PayrollConfig;
import com.tamabee.api_hr.enums.SalaryType;

import lombok.extern.slf4j.Slf4j;

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
                .requireGeoLocation(false)
                .geoFenceRadiusMeters(500)
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
     * Lấy BreakConfig mặc định
     */
    public BreakConfig getDefaultBreakConfig(Long companyId) {
        log.warn("Sử dụng BreakConfig mặc định cho companyId: {}. Vui lòng cấu hình trong settings.", companyId);
        return BreakConfig.builder()
                .breakEnabled(true)
                .defaultBreakMinutes(60)
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
        if (config.getRequireGeoLocation() == null) {
            config.setRequireGeoLocation(defaults.getRequireGeoLocation());
            hasNullFields = true;
        }
        if (config.getGeoFenceRadiusMeters() == null) {
            config.setGeoFenceRadiusMeters(defaults.getGeoFenceRadiusMeters());
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
        if (config.getDefaultBreakMinutes() == null) {
            config.setDefaultBreakMinutes(defaults.getDefaultBreakMinutes());
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


}
