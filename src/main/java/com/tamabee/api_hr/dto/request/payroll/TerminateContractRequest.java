package com.tamabee.api_hr.dto.request.payroll;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO cho chấm dứt hợp đồng lao động
 */
@Data
public class TerminateContractRequest {

    @NotBlank(message = "Lý do chấm dứt không được để trống")
    private String reason;
}
