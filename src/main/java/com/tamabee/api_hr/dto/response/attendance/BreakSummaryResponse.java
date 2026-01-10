package com.tamabee.api_hr.dto.response.attendance;

import com.tamabee.api_hr.enums.BreakType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response chứa tổng hợp giờ giải lao của nhân viên trong ngày
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

    // Loại giải lao áp dụng
    private BreakType breakType;

    // Có tuân thủ minimum break không
    private Boolean breakCompliant;

    // Số phút giải lao tối thiểu yêu cầu
    private Integer minimumBreakRequired;

    // Danh sách các bản ghi giải lao
    private List<BreakRecordResponse> breakRecords;
}
