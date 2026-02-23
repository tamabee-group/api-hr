package com.tamabee.api_hr.dto.response.attendance;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chứa tất cả dữ liệu lịch làm việc của nhân viên
 * Bao gồm: danh sách ca làm việc và lịch sử yêu cầu đổi ca
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeScheduleDataResponse {

    private List<ShiftAssignmentResponse> shifts;
    private List<ShiftSwapRequestResponse> swapRequests;
}
