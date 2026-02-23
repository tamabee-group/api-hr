package com.tamabee.api_hr.dto.request.leave;

import com.tamabee.api_hr.enums.LeaveType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cập nhật số ngày phép cho nhân viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLeaveBalanceRequest {

    /**
     * Năm áp dụng
     */
    @NotNull(message = "Năm không được để trống")
    private Integer year;

    /**
     * Loại nghỉ phép (ANNUAL hoặc SICK)
     */
    @NotNull(message = "Loại nghỉ phép không được để trống")
    private LeaveType leaveType;

    /**
     * Tổng số ngày phép được cấp
     */
    @NotNull(message = "Số ngày phép không được để trống")
    @Min(value = 0, message = "Số ngày phép phải >= 0")
    private Integer totalDays;
}
