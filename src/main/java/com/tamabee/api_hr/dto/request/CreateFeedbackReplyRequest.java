package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO để gửi phản hồi cho feedback.
 * Thông tin người phản hồi được lấy từ JWT token.
 */
@Data
public class CreateFeedbackReplyRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String content;
}
