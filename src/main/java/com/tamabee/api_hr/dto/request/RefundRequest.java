package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO để hoàn tiền vào ví công ty
 */
@Data
public class RefundRequest {

    @NotNull(message = "Số tiền hoàn không được để trống")
    @Positive(message = "Số tiền hoàn phải lớn hơn 0")
    private BigDecimal amount;

    @NotBlank(message = "Lý do hoàn tiền không được để trống")
    private String reason;
}
