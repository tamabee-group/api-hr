package com.tamabee.api_hr.dto.response.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho việc validate cấu hình lương
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryConfigValidationResponse {

    private boolean isValid;

    private boolean affectsCurrentPayroll;

    private String currentPayrollPeriod;

    private String message;

    // Số config cũ sẽ bị thay thế
    private int overlappingConfigsCount;

    // Có config cũ bị ảnh hưởng không
    private boolean hasOverlappingConfigs;
}
