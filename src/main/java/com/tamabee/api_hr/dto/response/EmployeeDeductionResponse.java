package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.DeductionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO cho khấu trừ nhân viên
 */
@Data
public class EmployeeDeductionResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long companyId;
    private String deductionCode;
    private String deductionName;
    private DeductionType deductionType;
    private BigDecimal amount;
    private BigDecimal percentage;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
