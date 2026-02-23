package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.FeedbackStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO để cập nhật trạng thái feedback.
 */
@Data
public class UpdateFeedbackStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private FeedbackStatus status;
}
