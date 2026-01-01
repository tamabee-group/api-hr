package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.dto.config.BreakPeriod;
import com.tamabee.api_hr.enums.BreakType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * Response chứa cấu hình giờ giải lao của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakConfigResponse {

    // Bật/tắt giờ giải lao
    private Boolean breakEnabled;

    // Loại giải lao: PAID hoặc UNPAID
    private BreakType breakType;

    // Thời gian giải lao mặc định (phút)
    private Integer defaultBreakMinutes;

    // Thời gian giải lao tối thiểu (phút)
    private Integer minimumBreakMinutes;

    // Thời gian giải lao tối đa (phút)
    private Integer maximumBreakMinutes;

    // Sử dụng legal minimum hay custom
    private Boolean useLegalMinimum;

    // Bật/tắt tracking giờ giải lao
    private Boolean breakTrackingEnabled;

    // Locale cho legal requirements
    private String locale;

    // Fixed break mode
    private Boolean fixedBreakMode;

    // Số lần giải lao trong 1 lần chấm công
    private Integer breakPeriodsPerAttendance;

    // Số lần giải lao tối đa trong ngày
    private Integer maxBreaksPerDay;

    // Danh sách các khoảng giải lao cố định
    private List<BreakPeriod> fixedBreakPeriods;

    // Night shift configuration
    private LocalTime nightShiftStartTime;
    private LocalTime nightShiftEndTime;

    // Night shift break requirements
    private Integer nightShiftMinimumBreakMinutes;
    private Integer nightShiftDefaultBreakMinutes;
}
