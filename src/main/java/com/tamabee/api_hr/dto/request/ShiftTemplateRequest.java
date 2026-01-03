package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Request DTO để tạo hoặc cập nhật mẫu ca làm việc.
 */
@Data
public class ShiftTemplateRequest {

    @NotBlank(message = "Tên ca làm việc không được để trống")
    @Size(max = 100, message = "Tên ca làm việc không được vượt quá 100 ký tự")
    private String name;

    @NotNull(message = "Thời gian bắt đầu ca không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Thời gian kết thúc ca không được để trống")
    private LocalTime endTime;

    private Integer breakMinutes;

    private BigDecimal multiplier;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    private Boolean isActive;
}
