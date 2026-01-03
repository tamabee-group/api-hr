package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.BreakType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO snapshot cho cấu hình giải lao.
 * Lưu lại cấu hình giải lao đã áp dụng tại thời điểm chấm công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakConfigSnapshot {

    // Loại giải lao (PAID/UNPAID)
    private BreakType breakType;

    // Số phút giải lao tối thiểu
    private Integer minimumBreakMinutes;

    // Số phút giải lao tối đa
    private Integer maximumBreakMinutes;

    // Số lần giải lao tối đa trong ngày
    private Integer maxBreaksPerDay;

    // Số phút giải lao tối thiểu theo luật
    private Integer legalMinimumBreakMinutes;
}
