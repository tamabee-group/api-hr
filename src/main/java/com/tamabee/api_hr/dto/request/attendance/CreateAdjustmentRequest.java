package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Request tạo yêu cầu điều chỉnh chấm công.
 * Nhân viên có thể yêu cầu thay đổi giờ check-in, check-out hoặc cả hai.
 * Hỗ trợ cả trường hợp có và không có attendance record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdjustmentRequest {

    /**
     * ID bản ghi chấm công cần điều chỉnh (optional - có thể null khi tạo mới)
     */
    private Long attendanceRecordId;

    /**
     * Ngày làm việc cần điều chỉnh (bắt buộc khi không có attendanceRecordId)
     */
    private LocalDate workDate;

    /**
     * Giờ check-in yêu cầu (null nếu không thay đổi)
     */
    private LocalDateTime requestedCheckIn;

    /**
     * Giờ check-out yêu cầu (null nếu không thay đổi)
     */
    private LocalDateTime requestedCheckOut;

    /**
     * ID của break record cần điều chỉnh (bắt buộc khi điều chỉnh break)
     */
    private Long breakRecordId;

    /**
     * Giờ break-in yêu cầu (null nếu không thay đổi)
     */
    private LocalDateTime requestedBreakStart;

    /**
     * Giờ break-out yêu cầu (null nếu không thay đổi)
     */
    private LocalDateTime requestedBreakEnd;

    /**
     * Lý do yêu cầu điều chỉnh
     */
    @NotBlank(message = "Lý do điều chỉnh không được để trống")
    @Size(max = 500, message = "Lý do điều chỉnh không được vượt quá 500 ký tự")
    private String reason;

    /**
     * ID người được gán xử lý yêu cầu (manager/admin)
     */
    @NotNull(message = "Người nhận yêu cầu không được để trống")
    private Long assignedTo;
}
