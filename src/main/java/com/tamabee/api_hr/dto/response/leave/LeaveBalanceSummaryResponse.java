package com.tamabee.api_hr.dto.response.leave;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response tổng hợp số ngày phép của một nhân viên.
 * Bao gồm thông tin nhân viên và danh sách balance theo từng loại phép.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceSummaryResponse {

    // ID nhân viên
    private Long employeeId;

    // Tên nhân viên
    private String employeeName;

    // Mã nhân viên
    private String employeeCode;

    // Danh sách số ngày phép theo từng loại
    private List<LeaveBalanceResponse> balances;
}
