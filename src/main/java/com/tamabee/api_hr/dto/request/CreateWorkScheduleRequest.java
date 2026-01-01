package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.enums.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request tạo lịch làm việc mới
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkScheduleRequest {

    @NotBlank(message = "Tên lịch làm việc không được để trống")
    private String name;

    @NotNull(message = "Loại lịch không được để trống")
    private ScheduleType type;

    // Đánh dấu là lịch mặc định
    private Boolean isDefault;

    // Dữ liệu chi tiết lịch làm việc
    private WorkScheduleData scheduleData;

    // Mô tả
    private String description;
}
