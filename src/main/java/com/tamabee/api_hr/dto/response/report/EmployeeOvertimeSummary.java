package com.tamabee.api_hr.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Tổng hợp làm thêm giờ của một nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeOvertimeSummary {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    // Phân loại overtime (phút)
    private Integer regularOvertimeMinutes;
    private Integer nightOvertimeMinutes;
    private Integer holidayOvertimeMinutes;
    private Integer weekendOvertimeMinutes;
    private Integer totalOvertimeMinutes;

    // Tiền overtime
    private BigDecimal regularOvertimePay;
    private BigDecimal nightOvertimePay;
    private BigDecimal holidayOvertimePay;
    private BigDecimal weekendOvertimePay;
    private BigDecimal totalOvertimePay;
}
