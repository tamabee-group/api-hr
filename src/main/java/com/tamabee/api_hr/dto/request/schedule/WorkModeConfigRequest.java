package com.tamabee.api_hr.dto.request.schedule;

import com.tamabee.api_hr.enums.WorkMode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Request cập nhật cấu hình chế độ làm việc của công ty.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkModeConfigRequest {

    // Chế độ làm việc mới
    @NotNull(message = "Chế độ làm việc không được để trống")
    private WorkMode mode;

    // Giờ bắt đầu làm việc mặc định (bắt buộc khi mode = FIXED_HOURS)
    private LocalTime defaultWorkStartTime;

    // Giờ kết thúc làm việc mặc định (bắt buộc khi mode = FIXED_HOURS)
    private LocalTime defaultWorkEndTime;

    // Thời gian nghỉ giải lao mặc định (phút) (bắt buộc khi mode = FIXED_HOURS)
    private Integer defaultBreakMinutes;

    // Lý do thay đổi (optional, dùng cho audit log)
    private String reason;
}
