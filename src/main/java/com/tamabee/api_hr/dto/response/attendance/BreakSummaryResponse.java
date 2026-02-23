package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chứa tổng hợp giờ giải lao của nhân viên trong ngày.
 * Đơn giản: giải lao luôn bị trừ khỏi giờ làm việc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakSummaryResponse {

    private Long employeeId;

    private String employeeName;

    private LocalDate workDate;

    // Tổng số phút giải lao thực tế
    private Integer totalActualBreakMinutes;

    // Tổng số phút giải lao hiệu lực
    private Integer totalEffectiveBreakMinutes;

    // Số lần giải lao
    private Integer breakCount;

    // Danh sách các bản ghi giải lao
    private List<BreakRecordResponse> breakRecords;
}
