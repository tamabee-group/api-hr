package com.tamabee.api_hr.mapper.company;

import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.RoundingConfig;
import com.tamabee.api_hr.dto.request.attendance.AttendanceConfigRequest;
import com.tamabee.api_hr.entity.company.AttendanceSettingEntity;
import com.tamabee.api_hr.enums.RoundingDirection;
import com.tamabee.api_hr.enums.RoundingInterval;

/**
 * Mapper chuyển đổi giữa AttendanceSettingEntity và AttendanceConfig DTO.
 * Xử lý mapping giữa entity (string fields cho rounding) và DTO (enum fields).
 */
@Component
public class AttendanceSettingMapper {

    /**
     * Tạo entity mới với default values
     */
    public AttendanceSettingEntity toEntity() {
        AttendanceSettingEntity entity = new AttendanceSettingEntity();
        entity.setDefaultWorkStartTime(LocalTime.of(9, 0));
        entity.setDefaultWorkEndTime(LocalTime.of(18, 0));
        entity.setDefaultBreakMinutes(60);
        entity.setEnableRounding(true);
        entity.setEnableCheckInRounding(false);
        entity.setEnableCheckOutRounding(false);
        entity.setEnableBreakStartRounding(false);
        entity.setEnableBreakEndRounding(false);
        entity.setLateGraceMinutes(0);
        entity.setEarlyLeaveGraceMinutes(0);
        entity.setRequireGeoLocation(false);
        entity.setGeoFenceRadiusMeters(500);
        entity.setAllowWebCheckIn(true);
        entity.setSaturdayOff(true);
        entity.setSundayOff(true);
        entity.setHolidayOff(true);
        return entity;
    }

    /**
     * Chuyển AttendanceSettingEntity sang AttendanceConfig DTO
     */
    public AttendanceConfig toResponse(AttendanceSettingEntity entity) {
        if (entity == null) {
            return AttendanceConfig.builder().build();
        }

        AttendanceConfig config = AttendanceConfig.builder()
                .defaultWorkStartTime(entity.getDefaultWorkStartTime())
                .defaultWorkEndTime(entity.getDefaultWorkEndTime())
                .defaultBreakMinutes(entity.getDefaultBreakMinutes())
                .enableRounding(entity.getEnableRounding())
                .enableCheckInRounding(entity.getEnableCheckInRounding())
                .enableCheckOutRounding(entity.getEnableCheckOutRounding())
                .enableBreakStartRounding(entity.getEnableBreakStartRounding())
                .enableBreakEndRounding(entity.getEnableBreakEndRounding())
                .lateGraceMinutes(entity.getLateGraceMinutes())
                .earlyLeaveGraceMinutes(entity.getEarlyLeaveGraceMinutes())
                .requireGeoLocation(entity.getRequireGeoLocation())
                .geoFenceRadiusMeters(entity.getGeoFenceRadiusMeters())
                .allowWebCheckIn(entity.getAllowWebCheckIn())
                .saturdayOff(entity.getSaturdayOff())
                .sundayOff(entity.getSundayOff())
                .holidayOff(entity.getHolidayOff())
                .build();

        // Map rounding configs từ entity string fields sang DTO enum fields
        config.setCheckInRounding(toRoundingConfig(
                entity.getCheckInRoundingInterval(), entity.getCheckInRoundingDirection()));
        config.setCheckOutRounding(toRoundingConfig(
                entity.getCheckOutRoundingInterval(), entity.getCheckOutRoundingDirection()));
        config.setBreakStartRounding(toRoundingConfig(
                entity.getBreakStartRoundingInterval(), entity.getBreakStartRoundingDirection()));
        config.setBreakEndRounding(toRoundingConfig(
                entity.getBreakEndRoundingInterval(), entity.getBreakEndRoundingDirection()));

        return config;
    }

    /**
     * Cập nhật entity từ request (chỉ cập nhật các field không null)
     */
    public void updateEntity(AttendanceSettingEntity entity, AttendanceConfigRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getDefaultWorkStartTime() != null) {
            entity.setDefaultWorkStartTime(request.getDefaultWorkStartTime());
        }
        if (request.getDefaultWorkEndTime() != null) {
            entity.setDefaultWorkEndTime(request.getDefaultWorkEndTime());
        }
        if (request.getDefaultBreakMinutes() != null) {
            entity.setDefaultBreakMinutes(request.getDefaultBreakMinutes());
        }
        if (request.getEnableCheckInRounding() != null) {
            entity.setEnableCheckInRounding(request.getEnableCheckInRounding());
        }
        if (request.getEnableCheckOutRounding() != null) {
            entity.setEnableCheckOutRounding(request.getEnableCheckOutRounding());
        }
        if (request.getEnableBreakStartRounding() != null) {
            entity.setEnableBreakStartRounding(request.getEnableBreakStartRounding());
        }
        if (request.getEnableBreakEndRounding() != null) {
            entity.setEnableBreakEndRounding(request.getEnableBreakEndRounding());
        }

        // Tự động đồng bộ master toggle từ individual toggles
        boolean anyRoundingEnabled = Boolean.TRUE.equals(entity.getEnableCheckInRounding())
                || Boolean.TRUE.equals(entity.getEnableCheckOutRounding())
                || Boolean.TRUE.equals(entity.getEnableBreakStartRounding())
                || Boolean.TRUE.equals(entity.getEnableBreakEndRounding());
        entity.setEnableRounding(anyRoundingEnabled);

        // Rounding configs
        if (request.getCheckInRounding() != null) {
            entity.setCheckInRoundingInterval(
                    request.getCheckInRounding().getInterval() != null
                            ? request.getCheckInRounding().getInterval().name() : null);
            entity.setCheckInRoundingDirection(
                    request.getCheckInRounding().getDirection() != null
                            ? request.getCheckInRounding().getDirection().name() : null);
        }
        if (request.getCheckOutRounding() != null) {
            entity.setCheckOutRoundingInterval(
                    request.getCheckOutRounding().getInterval() != null
                            ? request.getCheckOutRounding().getInterval().name() : null);
            entity.setCheckOutRoundingDirection(
                    request.getCheckOutRounding().getDirection() != null
                            ? request.getCheckOutRounding().getDirection().name() : null);
        }
        if (request.getBreakStartRounding() != null) {
            entity.setBreakStartRoundingInterval(
                    request.getBreakStartRounding().getInterval() != null
                            ? request.getBreakStartRounding().getInterval().name() : null);
            entity.setBreakStartRoundingDirection(
                    request.getBreakStartRounding().getDirection() != null
                            ? request.getBreakStartRounding().getDirection().name() : null);
        }
        if (request.getBreakEndRounding() != null) {
            entity.setBreakEndRoundingInterval(
                    request.getBreakEndRounding().getInterval() != null
                            ? request.getBreakEndRounding().getInterval().name() : null);
            entity.setBreakEndRoundingDirection(
                    request.getBreakEndRounding().getDirection() != null
                            ? request.getBreakEndRounding().getDirection().name() : null);
        }
        if (request.getLateGraceMinutes() != null) {
            entity.setLateGraceMinutes(request.getLateGraceMinutes());
        }
        if (request.getEarlyLeaveGraceMinutes() != null) {
            entity.setEarlyLeaveGraceMinutes(request.getEarlyLeaveGraceMinutes());
        }
        if (request.getRequireGeoLocation() != null) {
            entity.setRequireGeoLocation(request.getRequireGeoLocation());
        }
        if (request.getGeoFenceRadiusMeters() != null) {
            entity.setGeoFenceRadiusMeters(request.getGeoFenceRadiusMeters());
        }
        if (request.getAllowWebCheckIn() != null) {
            entity.setAllowWebCheckIn(request.getAllowWebCheckIn());
        }
        // Nghỉ cuối tuần & ngày lễ
        if (request.getSaturdayOff() != null) {
            entity.setSaturdayOff(request.getSaturdayOff());
        }
        if (request.getSundayOff() != null) {
            entity.setSundayOff(request.getSundayOff());
        }
        if (request.getHolidayOff() != null) {
            entity.setHolidayOff(request.getHolidayOff());
        }
    }

    // ==================== Helper methods ====================

    /**
     * Chuyển interval/direction strings sang RoundingConfig DTO
     */
    private RoundingConfig toRoundingConfig(String interval, String direction) {
        if (interval == null && direction == null) {
            return null;
        }
        return RoundingConfig.builder()
                .interval(parseRoundingInterval(interval))
                .direction(parseRoundingDirection(direction))
                .build();
    }

    /**
     * Parse string sang RoundingInterval enum, trả về default nếu null/invalid
     */
    private RoundingInterval parseRoundingInterval(String value) {
        if (value == null) {
            return RoundingInterval.MINUTES_15;
        }
        try {
            return RoundingInterval.valueOf(value);
        } catch (IllegalArgumentException e) {
            return RoundingInterval.MINUTES_15;
        }
    }

    /**
     * Parse string sang RoundingDirection enum, trả về default nếu null/invalid
     */
    private RoundingDirection parseRoundingDirection(String value) {
        if (value == null) {
            return RoundingDirection.NEAREST;
        }
        try {
            return RoundingDirection.valueOf(value);
        } catch (IllegalArgumentException e) {
            return RoundingDirection.NEAREST;
        }
    }
}
