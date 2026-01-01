package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request tạo yêu cầu nghỉ phép mới.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeaveRequest {

    /**
     * Loại nghỉ phép
     */
    @NotNull(message = "Loại nghỉ phép không được để trống")
    private LeaveType leaveType;

    /**
     * Ngày bắt đầu nghỉ
     */
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    /**
     * Ngày kết thúc nghỉ
     */
    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    /**
     * Lý do nghỉ
     */
    @Size(max = 500, message = "Lý do nghỉ không được vượt quá 500 ký tự")
    private String reason;
}
