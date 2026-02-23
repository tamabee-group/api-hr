package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalDateTime;

import com.tamabee.api_hr.enums.BreakActionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chứa thông tin chi tiết điều chỉnh break trong yêu cầu điều chỉnh chấm công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakItemResponse {

    private Long id;

    // ID của break record được điều chỉnh
    private Long breakRecordId;

    // Số thứ tự break
    private Integer breakNumber;

    // Loại thao tác: ADJUST (điều chỉnh), DELETE (xóa)
    private BreakActionType actionType;

    // Thời gian break gốc
    private LocalDateTime originalBreakStart;
    private LocalDateTime originalBreakEnd;

    // Thời gian break yêu cầu thay đổi
    private LocalDateTime requestedBreakStart;
    private LocalDateTime requestedBreakEnd;

    private LocalDateTime createdAt;
}
