package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO để đánh dấu kỳ lương đã thanh toán
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    // Mã tham chiếu thanh toán (tùy chọn)
    @Size(max = 100, message = "Mã tham chiếu không được vượt quá 100 ký tự")
    private String paymentReference;
}
