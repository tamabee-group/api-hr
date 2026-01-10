package com.tamabee.api_hr.dto.request.company;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO để từ chối yêu cầu nạp tiền
 */
@Data
public class RejectRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    private String rejectionReason;
}
