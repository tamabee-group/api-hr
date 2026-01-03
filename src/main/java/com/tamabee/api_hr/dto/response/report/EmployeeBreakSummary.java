package com.tamabee.api_hr.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tổng hợp nghỉ giải lao của một nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeBreakSummary {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    // Thống kê break
    private Integer totalBreakCount;
    private Integer totalBreakMinutes;
    private Integer averageBreakMinutesPerDay;

    // Tuân thủ
    private Integer compliantBreakCount;
    private Integer nonCompliantBreakCount;
    private Double complianceRate; // % tuân thủ
}
