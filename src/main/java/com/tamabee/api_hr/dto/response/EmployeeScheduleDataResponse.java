package com.tamabee.api_hr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response chứa tất cả dữ liệu lịch làm việc của nhân viên
 * Bao gồm: ca làm việc và lịch sử đổi ca
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeScheduleDataResponse {

    /**
     * Danh sách ca làm việc được phân công
     */
    private List<ShiftAssignmentResponse> shifts;

    /**
     * Danh sách yêu cầu đổi ca
     */
    private List<ShiftSwapRequestResponse> swapRequests;
}
