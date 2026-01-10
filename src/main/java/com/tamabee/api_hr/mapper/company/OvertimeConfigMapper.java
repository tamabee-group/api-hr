package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.request.payroll.OvertimeConfigRequest;
import com.tamabee.api_hr.dto.response.payroll.OvertimeConfigResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Mapper chuyển đổi giữa OvertimeConfig và DTOs
 */
@Component
public class OvertimeConfigMapper {

    /**
     * Chuyển OvertimeConfig sang response
     */
    public OvertimeConfigResponse toResponse(OvertimeConfig config) {
        if (config == null) {
            return null;
        }

        return OvertimeConfigResponse.builder()
                .overtimeEnabled(config.getOvertimeEnabled())
                .requireApproval(config.getRequireApproval())
                .standardWorkingHours(config.getStandardWorkingHours())
                .nightStartTime(config.getNightStartTime())
                .nightEndTime(config.getNightEndTime())
                .regularOvertimeRate(config.getRegularOvertimeRate())
                .nightWorkRate(config.getNightWorkRate())
                .nightOvertimeRate(config.getNightOvertimeRate())
                .holidayOvertimeRate(config.getHolidayOvertimeRate())
                .holidayNightOvertimeRate(config.getHolidayNightOvertimeRate())
                .weekendOvertimeRate(config.getWeekendOvertimeRate())
                .useLegalMinimum(config.getUseLegalMinimum())
                .locale(config.getLocale())
                .maxOvertimeHoursPerDay(config.getMaxOvertimeHoursPerDay())
                .maxOvertimeHoursPerMonth(config.getMaxOvertimeHoursPerMonth())
                .build();
    }

    /**
     * Tạo OvertimeConfig mới từ request
     */
    public OvertimeConfig toConfig(OvertimeConfigRequest request) {
        if (request == null) {
            return OvertimeConfig.builder().build();
        }

        return OvertimeConfig.builder()
                .overtimeEnabled(request.getEnableOvertime() != null ? request.getEnableOvertime() : true)
                .requireApproval(request.getRequireApproval() != null ? request.getRequireApproval() : false)
                .nightStartTime(request.getNightStartTime() != null ? request.getNightStartTime() : LocalTime.of(22, 0))
                .nightEndTime(request.getNightEndTime() != null ? request.getNightEndTime() : LocalTime.of(5, 0))
                .regularOvertimeRate(request.getRegularOvertimeRate() != null ? request.getRegularOvertimeRate()
                        : new BigDecimal("1.25"))
                .nightOvertimeRate(request.getNightOvertimeRate() != null ? request.getNightOvertimeRate()
                        : new BigDecimal("1.50"))
                .holidayOvertimeRate(request.getHolidayOvertimeRate() != null ? request.getHolidayOvertimeRate()
                        : new BigDecimal("1.35"))
                .weekendOvertimeRate(request.getWeekendOvertimeRate() != null ? request.getWeekendOvertimeRate()
                        : new BigDecimal("1.35"))
                .maxOvertimeHoursPerDay(
                        request.getMaxOvertimeHoursPerDay() != null ? request.getMaxOvertimeHoursPerDay() : 4)
                .maxOvertimeHoursPerMonth(
                        request.getMaxOvertimeHoursPerMonth() != null ? request.getMaxOvertimeHoursPerMonth() : 45)
                .build();
    }

    /**
     * Cập nhật OvertimeConfig từ request (chỉ cập nhật các field không null)
     */
    public void updateConfig(OvertimeConfig config, OvertimeConfigRequest request) {
        if (config == null || request == null) {
            return;
        }

        if (request.getEnableOvertime() != null) {
            config.setOvertimeEnabled(request.getEnableOvertime());
        }
        if (request.getRequireApproval() != null) {
            config.setRequireApproval(request.getRequireApproval());
        }
        if (request.getRegularOvertimeRate() != null) {
            config.setRegularOvertimeRate(request.getRegularOvertimeRate());
        }
        if (request.getNightOvertimeRate() != null) {
            config.setNightOvertimeRate(request.getNightOvertimeRate());
        }
        if (request.getHolidayOvertimeRate() != null) {
            config.setHolidayOvertimeRate(request.getHolidayOvertimeRate());
        }
        if (request.getWeekendOvertimeRate() != null) {
            config.setWeekendOvertimeRate(request.getWeekendOvertimeRate());
        }
        if (request.getNightStartTime() != null) {
            config.setNightStartTime(request.getNightStartTime());
        }
        if (request.getNightEndTime() != null) {
            config.setNightEndTime(request.getNightEndTime());
        }
        if (request.getMaxOvertimeHoursPerDay() != null) {
            config.setMaxOvertimeHoursPerDay(request.getMaxOvertimeHoursPerDay());
        }
        if (request.getMaxOvertimeHoursPerMonth() != null) {
            config.setMaxOvertimeHoursPerMonth(request.getMaxOvertimeHoursPerMonth());
        }
    }
}
