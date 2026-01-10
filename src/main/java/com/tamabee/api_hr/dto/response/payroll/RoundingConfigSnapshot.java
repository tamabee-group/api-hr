package com.tamabee.api_hr.dto.response.payroll;

import com.tamabee.api_hr.enums.RoundingDirection;
import com.tamabee.api_hr.enums.RoundingInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO snapshot cho cấu hình làm tròn.
 * Lưu lại cấu hình làm tròn đã áp dụng tại thời điểm chấm công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundingConfigSnapshot {

    // Khoảng thời gian làm tròn (5, 10, 15, 30 phút)
    private RoundingInterval interval;

    // Hướng làm tròn (UP, DOWN, NEAREST)
    private RoundingDirection direction;
}
