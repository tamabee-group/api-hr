package com.tamabee.api_hr.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo làm thêm giờ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeReport {

    private Long companyId;
    private LocalDate startDate;
    private LocalDate endDate;

    // Tổng quan
    private Integer totalEmployeesWithOvertime;
    private Integer totalRegularOvertimeMinutes;
    private Integer totalNightOvertimeMinutes;
    private Integer totalHolidayOvertimeMinutes;
    private Integer totalWeekendOvertimeMinutes;
    private Integer totalOvertimeMinutes;

    // Tổng chi phí overtime
    private BigDecimal totalRegularOvertimePay;
    private BigDecimal totalNightOvertimePay;
    private BigDecimal totalHolidayOvertimePay;
    private BigDecimal totalWeekendOvertimePay;
    private BigDecimal totalOvertimePay;

    // Chi tiết theo nhân viên
    private List<EmployeeOvertimeSummary> employeeSummaries;
}
