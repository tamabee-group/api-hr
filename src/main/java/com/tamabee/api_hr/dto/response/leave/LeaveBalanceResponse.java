package com.tamabee.api_hr.dto.response.leave;

import com.tamabee.api_hr.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chứa thông tin số ngày phép còn lại.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {

    private Long id;

    // Thông tin nhân viên
    private Long employeeId;

    // Năm
    private Integer year;

    // Loại nghỉ phép
    private LeaveType leaveType;

    // Số ngày phép
    private Integer totalDays;
    private Integer usedDays;
    private Integer remainingDays;
}
