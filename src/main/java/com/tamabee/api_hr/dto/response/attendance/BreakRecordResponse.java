package com.tamabee.api_hr.dto.response.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO response cho break record.
 * Chứa thông tin chi tiết về một lần giải lao.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakRecordResponse {

    // ID của break record
    private Long id;

    // Số thứ tự break trong ngày (1, 2, 3...)
    private Integer breakNumber;

    // Thời gian bắt đầu giải lao
    private LocalDateTime breakStart;

    // Thời gian kết thúc giải lao
    private LocalDateTime breakEnd;

    // Thời gian giải lao thực tế (phút)
    private Integer actualBreakMinutes;

    // Thời gian giải lao hiệu lực sau khi áp dụng min/max (phút)
    private Integer effectiveBreakMinutes;

    // Ghi chú
    private String notes;

    // Break đang diễn ra (chưa kết thúc)
    private Boolean isActive;
}
