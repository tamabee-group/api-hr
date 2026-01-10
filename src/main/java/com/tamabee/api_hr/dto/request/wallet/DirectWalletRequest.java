package com.tamabee.api_hr.dto.request.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO cho thao tác wallet trực tiếp (chỉ Admin Tamabee)
 * Dùng cho addBalanceDirect và deductBalanceDirect
 * Requirements: 1.3, 1.4
 */
@Data
public class DirectWalletRequest {

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotBlank(message = "Mô tả giao dịch không được để trống")
    private String description;
}
