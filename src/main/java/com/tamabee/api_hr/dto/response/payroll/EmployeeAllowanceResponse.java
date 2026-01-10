package com.tamabee.api_hr.dto.response.payroll;

import com.tamabee.api_hr.enums.AllowanceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO cho phụ cấp nhân viên
 */
@Data
public class EmployeeAllowanceResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long companyId;
    private String allowanceCode;
    private String allowanceName;
    private AllowanceType allowanceType;
    private BigDecimal amount;
    private Boolean taxable;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
