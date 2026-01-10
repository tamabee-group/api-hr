package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    // Danh sách điều chỉnh break records (hỗ trợ nhiều breaks)
    private List<BreakAdjustment> breakAdjustments;

    // Lý do điều chỉnh (bắt buộc)
    @NotBlank(message = "Lý do điều chỉnh không được để trống")
    private String reason;

    /**
     * Inner class cho điều chỉnh từng break record
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakAdjustment {
        // ID của break record cần điều chỉnh
        private Long breakRecordId;
        // Thời gian break-start mới
        private LocalDateTime breakStartTime;
        // Thời gian break-end mới
        private LocalDateTime breakEndTime;
    }
}
