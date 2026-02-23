package com.tamabee.api_hr.dto.request.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.tamabee.api_hr.enums.AdjustmentRequestType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request tạo yêu cầu điều chỉnh chấm công.
 * Nhân viên có thể yêu cầu thay đổi giờ check-in, check-out và nhiều break records.
 * Hỗ trợ cả trường hợp có và không có attendance record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdjustmentRequest {

    /**
     * Loại yêu cầu: ADJUST, DELETE_RECORD
     * Mặc định là ADJUST nếu không truyền
     */
    private AdjustmentRequestType requestType;

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
     * Danh sách các break items cần điều chỉnh/xóa
     * Mỗi item chứa breakRecordId, actionType (ADJUST/DELETE), và thời gian yêu cầu
     */
    @Valid
    private List<BreakAdjustmentItem> breakItems;

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
