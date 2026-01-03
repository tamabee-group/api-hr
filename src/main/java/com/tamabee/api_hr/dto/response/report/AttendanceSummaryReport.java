package com.tamabee.api_hr.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo tổng hợp chấm công
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryReport {

    private Long companyId;
    private LocalDate startDate;
    private LocalDate endDate;

    // Tổng quan
    private Integer totalEmployees;
    private Integer totalWorkingDays;
    private Integer totalPresentDays;
    private Integer totalAbsentDays;
    private Integer totalLateCount;
    private Integer totalEarlyLeaveCount;

    // Tỷ lệ
    private Double attendanceRate; // % có mặt
    private Double punctualityRate; // % đúng giờ

    // Chi tiết theo nhân viên
    private List<EmployeeAttendanceSummary> employeeSummaries;
}
