package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO để tạo yêu cầu nạp tiền
 */
@Data
public class DepositRequestCreateRequest {

    @NotNull(message = "Số tiền nạp không được để trống")
    @Positive(message = "Số tiền nạp phải lớn hơn 0")
    private BigDecimal amount;

    @NotBlank(message = "URL ảnh chứng minh chuyển khoản không được để trống")
    private String transferProofUrl;
}
