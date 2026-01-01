package com.tamabee.api_hr.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO chứa dữ liệu chi tiết của lịch làm việc.
 * Được serialize thành JSON và lưu vào scheduleData column.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkScheduleData {

    // Giờ làm việc mặc định (cho FIXED schedule)
    private LocalTime defaultStartTime;
    private LocalTime defaultEndTime;
    private Integer defaultBreakMinutes;

    // Giờ làm việc theo ngày trong tuần (cho FLEXIBLE schedule)
    // Key: DayOfWeek name (MONDAY, TUESDAY, ...)
    private Map<String, DailySchedule> dailySchedules;

    // Danh sách ca làm việc (cho SHIFT schedule)
    private List<ShiftSchedule> shifts;

    // Danh sách các khoảng giải lao trong ngày
    @Builder.Default
    private List<BreakPeriod> breakPeriods = new ArrayList<>();

    // Tổng thời gian giải lao (phút) - tính từ breakPeriods
    private Integer totalBreakMinutes;

    /**
     * Lịch làm việc theo ngày
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySchedule {
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer breakMinutes;
        private Boolean isWorkingDay;
    }

    /**
     * Ca làm việc
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftSchedule {
        private String shiftName;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer breakMinutes;
    }
}
