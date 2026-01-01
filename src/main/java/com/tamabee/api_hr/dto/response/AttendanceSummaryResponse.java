package com.tamabee.api_hr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

/**
 * Response cho tổng hợp chấm công của nhân viên trong một kỳ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {

    private Long employeeId;
    private String employeeName;
    private YearMonth period;

    // Số ngày
    private Integer totalWorkingDays;
    private Integer presentDays;
    private Integer absentDays;
    private Integer leaveDays;
    private Integer holidayDays;

    // Tổng thời gian (phút)
    private Integer totalWorkingMinutes;
    private Integer totalOvertimeMinutes;
    private Integer totalLateMinutes;
    private Integer totalEarlyLeaveMinutes;

    // Tổng thời gian (giờ) - để hiển thị
    private Double totalWorkingHours;
    private Double totalOvertimeHours;
}
