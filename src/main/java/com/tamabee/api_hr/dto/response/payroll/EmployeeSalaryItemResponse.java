package com.tamabee.api_hr.dto.response.payroll;

import java.math.BigDecimal;

import com.tamabee.api_hr.enums.SalaryItemType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho phụ cấp/khấu trừ của nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSalaryItemResponse {

    private Long id;
    private Long employeeId;
    private Long templateId;
    private String templateName;
    private SalaryItemType type;
    private BigDecimal amount;
}
