package com.tamabee.api_hr.dto.request.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cho việc tạo bản ghi chấm công mới (bởi manager)
 * Dùng khi nhân viên chưa có record cho ngày đó
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttendanceRequest {

    // ID nhân viên
    @NotNull(message = "ID nhân viên không được để trống")
    private Long employeeId;

    // Ngày làm việc
    @NotNull(message = "Ngày làm việc không được để trống")
    private LocalDate workDate;

    // Thời gian check-in
    private LocalDateTime checkInTime;

    // Thời gian check-out
    private LocalDateTime checkOutTime;

    // Danh sách break records
    private List<BreakItem> breakItems;

    // Lý do tạo
    @NotBlank(message = "Lý do không được để trống")
    private String reason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakItem {
        private LocalDateTime breakStartTime;
        private LocalDateTime breakEndTime;
    }
}
