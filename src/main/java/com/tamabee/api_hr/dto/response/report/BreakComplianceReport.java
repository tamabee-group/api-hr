package com.tamabee.api_hr.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo tuân thủ nghỉ giải lao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakComplianceReport {

    private Long companyId;
    private LocalDate startDate;
    private LocalDate endDate;

    // Tổng quan
    private Integer totalEmployees;
    private Integer totalBreakCount;
    private Integer totalBreakMinutes;
    private Integer averageBreakMinutesPerEmployee;

    // Tuân thủ
    private Integer compliantBreakCount;
    private Integer nonCompliantBreakCount;
    private Double overallComplianceRate;

    // Chi tiết theo nhân viên
    private List<EmployeeBreakSummary> employeeSummaries;
}
