package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.TargetAudience;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO để tạo thông báo hệ thống mới.
 * Nội dung 3 ngôn ngữ (vi, en, ja) và đối tượng nhận.
 */
@Data
public class CreateSystemNotificationRequest {

    @NotBlank(message = "Tiêu đề tiếng Việt không được để trống")
    private String titleVi;

    @NotBlank(message = "Tiêu đề tiếng Anh không được để trống")
    private String titleEn;

    @NotBlank(message = "Tiêu đề tiếng Nhật không được để trống")
    private String titleJa;

    @NotBlank(message = "Nội dung tiếng Việt không được để trống")
    private String contentVi;

    @NotBlank(message = "Nội dung tiếng Anh không được để trống")
    private String contentEn;

    @NotBlank(message = "Nội dung tiếng Nhật không được để trống")
    private String contentJa;

    @NotNull(message = "Đối tượng nhận không được để trống")
    private TargetAudience targetAudience;
}
