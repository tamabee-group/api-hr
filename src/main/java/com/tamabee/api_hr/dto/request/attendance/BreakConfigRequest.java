package com.tamabee.api_hr.dto.request.attendance;

import com.tamabee.api_hr.dto.config.BreakPeriod;
import com.tamabee.api_hr.enums.BreakType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * Request cập nhật cấu hình giờ giải lao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakConfigRequest {

    private Boolean breakEnabled;

    private BreakType breakType;

    @Min(0)
    @Max(480)
    private Integer defaultBreakMinutes;

    @Min(0)
    @Max(480)
    private Integer minimumBreakMinutes;

    @Min(0)
    @Max(480)
    private Integer maximumBreakMinutes;

    private Boolean useLegalMinimum;

    private Boolean breakTrackingEnabled;

    private String locale;

    private Boolean fixedBreakMode;

    @Min(1)
    @Max(5)
    private Integer breakPeriodsPerAttendance;

    @Min(1)
    @Max(10)
    private Integer maxBreaksPerDay;

    private List<BreakPeriod> fixedBreakPeriods;

    private LocalTime nightShiftStartTime;

    private LocalTime nightShiftEndTime;

    @Min(0)
    @Max(480)
    private Integer nightShiftMinimumBreakMinutes;

    @Min(0)
    @Max(480)
    private Integer nightShiftDefaultBreakMinutes;
}
