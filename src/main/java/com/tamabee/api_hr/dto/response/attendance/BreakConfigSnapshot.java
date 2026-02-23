package com.tamabee.api_hr.dto.response.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO snapshot cho cấu hình giải lao.
 * Lưu lại cấu hình giải lao đã áp dụng tại thời điểm chấm công.
 * Đơn giản: giải lao luôn bị trừ khỏi giờ làm việc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakConfigSnapshot {

    // Số phút giải lao mặc định
    private Integer defaultBreakMinutes;

    // Số lần giải lao tối đa trong ngày
    private Integer maxBreaksPerDay;
}
