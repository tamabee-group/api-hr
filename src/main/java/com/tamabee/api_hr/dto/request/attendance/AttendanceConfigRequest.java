package com.tamabee.api_hr.dto.request.attendance;

import com.tamabee.api_hr.dto.config.RoundingConfig;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Request cập nhật cấu hình chấm công
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceConfigRequest {

    private LocalTime defaultWorkStartTime;
    private LocalTime defaultWorkEndTime;

    @Min(0)
    @Max(180)
    private Integer defaultBreakMinutes;

    // Rounding toggles
    private Boolean enableRounding;
    private Boolean enableCheckInRounding;
    private Boolean enableCheckOutRounding;
    private Boolean enableBreakStartRounding;
    private Boolean enableBreakEndRounding;

    // Rounding configs
    private RoundingConfig checkInRounding;
    private RoundingConfig checkOutRounding;
    private RoundingConfig breakStartRounding;
    private RoundingConfig breakEndRounding;

    @Min(0)
    @Max(60)
    private Integer lateGraceMinutes;

    @Min(0)
    @Max(60)
    private Integer earlyLeaveGraceMinutes;

    private Boolean requireDeviceRegistration;
    private Boolean requireGeoLocation;

    @Min(10)
    @Max(1000)
    private Integer geoFenceRadiusMeters;

    private Boolean allowMobileCheckIn;
    private Boolean allowWebCheckIn;
}
