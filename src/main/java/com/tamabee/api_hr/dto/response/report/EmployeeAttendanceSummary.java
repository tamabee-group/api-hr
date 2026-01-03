package com.tamabee.api_hr.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tổng hợp chấm công của một nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAttendanceSummary {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    // Số ngày làm việc
    private Integer totalWorkingDays;
    private Integer presentDays;
    private Integer absentDays;

    // Đi muộn / về sớm
    private Integer lateCount;
    private Integer totalLateMinutes;
    private Integer earlyLeaveCount;
    private Integer totalEarlyLeaveMinutes;

    // Tổng thời gian làm việc
    private Integer totalWorkingMinutes;
    private Integer averageWorkingMinutesPerDay;
}
