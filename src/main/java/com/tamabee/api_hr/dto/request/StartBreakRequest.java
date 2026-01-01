package com.tamabee.api_hr.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để bắt đầu giờ giải lao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartBreakRequest {

    // Ghi chú (tùy chọn)
    private String notes;
}
