package com.tamabee.api_hr.dto.response.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chứa cấu hình giờ giải lao của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakConfigResponse {

    // Bật/tắt giờ giải lao
    private Boolean breakEnabled;

    // Thời gian giải lao mặc định (phút)
    private Integer defaultBreakMinutes;

    // Số lần giải lao tối đa trong ngày
    private Integer maxBreaksPerDay;
}
