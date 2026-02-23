package com.tamabee.api_hr.dto.config;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cấu hình chấm công của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceConfig {

    // Giờ làm việc mặc định
    @Builder.Default
    private LocalTime defaultWorkStartTime = LocalTime.of(9, 0);

    @Builder.Default
    private LocalTime defaultWorkEndTime = LocalTime.of(18, 0);

    @Builder.Default
    private Integer defaultBreakMinutes = 60;

    // Làm tròn giờ - master toggle
    @Builder.Default
    private Boolean enableRounding = false;

    // Individual rounding toggles
    @Builder.Default
    private Boolean enableCheckInRounding = false;

    @Builder.Default
    private Boolean enableCheckOutRounding = false;

    @Builder.Default
    private Boolean enableBreakStartRounding = false;

    @Builder.Default
    private Boolean enableBreakEndRounding = false;

    // Rounding configs
    private RoundingConfig checkInRounding;

    private RoundingConfig checkOutRounding;

    private RoundingConfig breakStartRounding;

    private RoundingConfig breakEndRounding;

    // Grace period (phút)
    @Builder.Default
    private Integer lateGraceMinutes = 0;

    @Builder.Default
    private Integer earlyLeaveGraceMinutes = 0;

    // Location
    @Builder.Default
    private Boolean requireGeoLocation = false;

    @Builder.Default
    private Integer geoFenceRadiusMeters = 500;

    // Cho phép chấm công
    @Builder.Default
    private Boolean allowWebCheckIn = true;

    // Nghỉ cuối tuần & ngày lễ
    @Builder.Default
    private Boolean saturdayOff = true;

    @Builder.Default
    private Boolean sundayOff = true;

    @Builder.Default
    private Boolean holidayOff = true;
}
