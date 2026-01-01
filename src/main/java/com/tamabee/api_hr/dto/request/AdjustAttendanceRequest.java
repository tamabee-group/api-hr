package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request cho việc điều chỉnh bản ghi chấm công (bởi admin)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustAttendanceRequest {

    // Thời gian check-in mới (null = không thay đổi)
    private LocalDateTime checkInTime;

    // Thời gian check-out mới (null = không thay đổi)
    private LocalDateTime checkOutTime;

    // ID của break record cần điều chỉnh (cho multiple breaks support)
    private Long breakRecordId;

    // Thời gian break-in mới (null = không thay đổi)
    private LocalDateTime breakStartTime;

    // Thời gian break-out mới (null = không thay đổi)
    private LocalDateTime breakEndTime;

    // Lý do điều chỉnh (bắt buộc)
    @NotBlank(message = "Lý do điều chỉnh không được để trống")
    private String reason;
}
