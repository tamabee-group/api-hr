package com.tamabee.api_hr.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tổng hợp chấm công trong kỳ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummary {

    // Số ngày làm việc
    @Builder.Default
    private Integer workingDays = 0;

    // Tổng số giờ làm việc
    @Builder.Default
    private Integer workingHours = 0;

    // Số ca làm việc (cho SHIFT_BASED)
    @Builder.Default
    private Integer numberOfShifts = 0;

    // Số ngày vắng mặt
    @Builder.Default
    private Integer absenceDays = 0;

    // Số lần đi muộn
    @Builder.Default
    private Integer lateCount = 0;

    // Tổng số phút đi muộn
    @Builder.Default
    private Integer totalLateMinutes = 0;

    // Số lần về sớm
    @Builder.Default
    private Integer earlyLeaveCount = 0;

    // Tổng số phút về sớm
    @Builder.Default
    private Integer totalEarlyLeaveMinutes = 0;

    // Tổng số phút tăng ca
    @Builder.Default
    private Integer totalOvertimeMinutes = 0;

    // Tổng số phút giải lao trong kỳ
    @Builder.Default
    private Integer totalBreakMinutes = 0;
}
