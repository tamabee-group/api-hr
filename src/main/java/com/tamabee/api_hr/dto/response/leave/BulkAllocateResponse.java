package com.tamabee.api_hr.dto.response.leave;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho API cấp phát số ngày phép hàng loạt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAllocateResponse {

    // Số lượng bản ghi đã được cập nhật
    private Integer updatedCount;
}
