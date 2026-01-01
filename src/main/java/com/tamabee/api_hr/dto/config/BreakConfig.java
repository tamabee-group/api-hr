package com.tamabee.api_hr.dto.config;

import com.tamabee.api_hr.enums.BreakType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình giờ giải lao của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakConfig {

    // Bật/tắt giờ giải lao
    @Builder.Default
    private Boolean breakEnabled = true;

    // Loại giải lao: PAID hoặc UNPAID
    @Builder.Default
    private BreakType breakType = BreakType.UNPAID;

    // Thời gian giải lao mặc định (phút) - dùng khi không tracking
    @Builder.Default
    private Integer defaultBreakMinutes = 60;

    // Thời gian giải lao tối thiểu (phút)
    @Builder.Default
    private Integer minimumBreakMinutes = 45;

    // Thời gian giải lao tối đa (phút)
    @Builder.Default
    private Integer maximumBreakMinutes = 90;

    // Sử dụng legal minimum hay custom
    @Builder.Default
    private Boolean useLegalMinimum = true;

    // Bật/tắt tracking giờ giải lao
    @Builder.Default
    private Boolean breakTrackingEnabled = false;

    // Locale cho legal requirements (vi, ja, en)
    @Builder.Default
    private String locale = "ja";

    // Fixed break mode - tự động áp dụng break mà không cần tracking
    @Builder.Default
    private Boolean fixedBreakMode = false;

    // Số lần giải lao trong 1 lần chấm công (1, 2, 3...)
    @Builder.Default
    private Integer breakPeriodsPerAttendance = 1;

    // Số lần giải lao tối đa trong ngày
    @Builder.Default
    private Integer maxBreaksPerDay = 3;

    // Danh sách các khoảng giải lao cố định (dùng khi fixedBreakMode = true)
    @Builder.Default
    private List<BreakPeriod> fixedBreakPeriods = new ArrayList<>();

    // Night shift configuration
    @Builder.Default
    private LocalTime nightShiftStartTime = LocalTime.of(22, 0); // 22:00

    @Builder.Default
    private LocalTime nightShiftEndTime = LocalTime.of(5, 0); // 05:00

    // Night shift break requirements (có thể khác với day shift)
    @Builder.Default
    private Integer nightShiftMinimumBreakMinutes = 45;

    @Builder.Default
    private Integer nightShiftDefaultBreakMinutes = 60;
}
