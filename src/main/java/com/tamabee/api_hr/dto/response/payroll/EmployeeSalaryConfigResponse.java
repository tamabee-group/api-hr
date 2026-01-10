package com.tamabee.api_hr.dto.response.payroll;

import com.tamabee.api_hr.enums.SalaryType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO cho thông tin cấu hình lương nhân viên
 */
@Data
@Builder
public class EmployeeSalaryConfigResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long companyId;
    private SalaryType salaryType;

    // Các mức lương theo loại
    private BigDecimal monthlySalary;
    private BigDecimal dailyRate;
    private BigDecimal hourlyRate;
    private BigDecimal shiftRate;

    // Thời gian hiệu lực
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    // Trạng thái
    private Boolean isActive;

    // Ghi chú
    private String note;

    // Audit fields
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
