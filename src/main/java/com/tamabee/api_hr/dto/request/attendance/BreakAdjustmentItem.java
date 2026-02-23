package com.tamabee.api_hr.dto.request.attendance;

import java.time.LocalDateTime;

import com.tamabee.api_hr.enums.BreakActionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho một item điều chỉnh break trong yêu cầu điều chỉnh chấm công.
 * Hỗ trợ 3 loại thao tác: ADJUST (điều chỉnh thời gian), DELETE (xóa break), CREATE (tạo mới).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakAdjustmentItem {

    /**
     * ID của break record cần điều chỉnh (null khi actionType = CREATE)
     */
    private Long breakRecordId;

    /**
     * Loại thao tác: ADJUST (điều chỉnh), DELETE (xóa), CREATE (tạo mới)
     * Mặc định là ADJUST nếu không truyền
     */
    private BreakActionType actionType;

    /**
     * Thời gian break start yêu cầu (null nếu không thay đổi hoặc xóa)
     */
    private LocalDateTime requestedBreakStart;

    /**
     * Thời gian break end yêu cầu (null nếu không thay đổi hoặc xóa)
     */
    private LocalDateTime requestedBreakEnd;
}
