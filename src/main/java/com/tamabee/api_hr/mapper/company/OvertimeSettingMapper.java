package com.tamabee.api_hr.mapper.company;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.request.payroll.OvertimeConfigRequest;
import com.tamabee.api_hr.entity.company.OvertimeSettingEntity;

/**
 * Mapper chuyển đổi giữa OvertimeSettingEntity và OvertimeConfig DTO.
 * Xử lý mapping giữa entity và DTO cho cấu hình tăng ca.
 */
@Component
public class OvertimeSettingMapper {

    /**
     * Tạo entity mới với default values
     */
    public OvertimeSettingEntity toEntity() {
        OvertimeSettingEntity entity = new OvertimeSettingEntity();
        entity.setOvertimeEnabled(true);
        entity.setStandardWorkingHours(8);
        entity.setNightStartTime(LocalTime.of(22, 0));
        entity.setNightEndTime(LocalTime.of(5, 0));
        entity.setRegularOvertimeRate(new BigDecimal("1.25"));
        entity.setNightWorkRate(new BigDecimal("1.25"));
        entity.setNightOvertimeRate(new BigDecimal("1.50"));
        entity.setHolidayOvertimeRate(new BigDecimal("1.35"));
        entity.setHolidayNightOvertimeRate(new BigDecimal("1.60"));
        entity.setWeekendOvertimeRate(new BigDecimal("1.35"));
        entity.setUseLegalMinimum(true);
        entity.setRegion("ja");
        entity.setMaxOvertimeHoursPerDay(4);
        entity.setMaxOvertimeHoursPerMonth(45);
        return entity;
    }

    /**
     * Chuyển OvertimeSettingEntity sang OvertimeConfig DTO
     */
    public OvertimeConfig toResponse(OvertimeSettingEntity entity) {
        if (entity == null) {
            return OvertimeConfig.builder().build();
        }

        return OvertimeConfig.builder()
                .overtimeEnabled(entity.getOvertimeEnabled())
                .standardWorkingHours(entity.getStandardWorkingHours())
                .nightStartTime(entity.getNightStartTime())
                .nightEndTime(entity.getNightEndTime())
                .regularOvertimeRate(entity.getRegularOvertimeRate())
                .nightWorkRate(entity.getNightWorkRate())
                .nightOvertimeRate(entity.getNightOvertimeRate())
                .holidayOvertimeRate(entity.getHolidayOvertimeRate())
                .holidayNightOvertimeRate(entity.getHolidayNightOvertimeRate())
                .weekendOvertimeRate(entity.getWeekendOvertimeRate())
                .useLegalMinimum(entity.getUseLegalMinimum())
                .region(entity.getRegion())
                .maxOvertimeHoursPerDay(entity.getMaxOvertimeHoursPerDay())
                .maxOvertimeHoursPerMonth(entity.getMaxOvertimeHoursPerMonth())
                .build();
    }

    /**
     * Cập nhật entity từ request (chỉ cập nhật các field không null)
     */
    public void updateEntity(OvertimeSettingEntity entity, OvertimeConfigRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getEnableOvertime() != null) {
            entity.setOvertimeEnabled(request.getEnableOvertime());
        }
        if (request.getRegularOvertimeRate() != null) {
            entity.setRegularOvertimeRate(request.getRegularOvertimeRate());
        }
        if (request.getNightOvertimeRate() != null) {
            entity.setNightOvertimeRate(request.getNightOvertimeRate());
        }
        if (request.getHolidayOvertimeRate() != null) {
            entity.setHolidayOvertimeRate(request.getHolidayOvertimeRate());
        }
        if (request.getWeekendOvertimeRate() != null) {
            entity.setWeekendOvertimeRate(request.getWeekendOvertimeRate());
        }
        if (request.getNightStartTime() != null) {
            entity.setNightStartTime(request.getNightStartTime());
        }
        if (request.getNightEndTime() != null) {
            entity.setNightEndTime(request.getNightEndTime());
        }
        if (request.getMaxOvertimeHoursPerDay() != null) {
            entity.setMaxOvertimeHoursPerDay(request.getMaxOvertimeHoursPerDay());
        }
        if (request.getMaxOvertimeHoursPerMonth() != null) {
            entity.setMaxOvertimeHoursPerMonth(request.getMaxOvertimeHoursPerMonth());
        }
    }
}
