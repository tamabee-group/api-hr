package com.tamabee.api_hr.dto.request.leave;

import java.util.List;

import com.tamabee.api_hr.enums.LeaveType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cấp phát số ngày phép hàng loạt cho nhiều nhân viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAllocateLeaveRequest {

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

    /**
     * Danh sách ID nhân viên cần cấp phát.
     * Nếu null hoặc rỗng thì áp dụng cho tất cả nhân viên.
     */
    private List<Long> employeeIds;
}
