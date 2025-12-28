package com.tamabee.api_hr.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO cho cấu hình hệ thống Tamabee
 */
@Data
public class SettingResponse {

    private Long id;

    private String settingKey;

    private String settingValue;

    private String description;

    private String valueType;

    private LocalDateTime updatedAt;
}
