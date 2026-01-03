package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO để điều chỉnh payroll item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollAdjustmentRequest {

    @NotNull(message = "Số tiền điều chỉnh không được để trống")
    private BigDecimal adjustmentAmount;

    @NotBlank(message = "Lý do điều chỉnh không được để trống")
    @Size(max = 500, message = "Lý do điều chỉnh không được vượt quá 500 ký tự")
    private String adjustmentReason;
}
