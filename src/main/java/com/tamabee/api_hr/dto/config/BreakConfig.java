package com.tamabee.api_hr.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cấu hình giờ giải lao của công ty.
 * Đơn giản: khi chấm giải lao, thời gian đó bị trừ khỏi giờ làm việc, không tính lương.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakConfig {

    // Bật/tắt giờ giải lao
    @Builder.Default
    private Boolean breakEnabled = true;

    // Thời gian giải lao mặc định (phút)
    @Builder.Default
    private Integer defaultBreakMinutes = 60;

    // Số lần giải lao tối đa trong ngày
    @Builder.Default
    private Integer maxBreaksPerDay = 3;
}
