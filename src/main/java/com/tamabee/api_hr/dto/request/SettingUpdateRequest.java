package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO để cập nhật cấu hình hệ thống Tamabee
 */
@Data
public class SettingUpdateRequest {

    @NotBlank(message = "Giá trị cấu hình không được để trống")
    private String settingValue;

    private String description;
}
