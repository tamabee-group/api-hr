package com.tamabee.api_hr.dto.response.schedule;

import com.tamabee.api_hr.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response chứa cấu hình chế độ làm việc của công ty.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkModeConfigResponse {

    // Chế độ làm việc hiện tại
    private WorkMode mode;

    // Giờ bắt đầu làm việc mặc định (dùng cho FIXED_HOURS mode)
    private LocalTime defaultWorkStartTime;

    // Giờ kết thúc làm việc mặc định (dùng cho FIXED_HOURS mode)
    private LocalTime defaultWorkEndTime;

    // Thời gian nghỉ giải lao mặc định (phút) (dùng cho FIXED_HOURS mode)
    private Integer defaultBreakMinutes;

    // Thời gian thay đổi mode gần nhất
    private LocalDateTime lastModeChangeAt;

    // Người thay đổi mode gần nhất
    private String lastModeChangeBy;
}
