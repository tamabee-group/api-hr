package com.tamabee.api_hr.dto.response.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho kết quả xóa phân ca hàng loạt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDeleteResult {

    private int totalRequested;
    private int successCount;
    private int failedCount;
}
