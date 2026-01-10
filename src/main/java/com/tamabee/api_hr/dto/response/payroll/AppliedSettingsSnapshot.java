package com.tamabee.api_hr.dto.response.payroll;

import com.tamabee.api_hr.dto.response.attendance.BreakConfigSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO snapshot cho các cấu hình đã áp dụng.
 * Lưu lại tất cả các settings đã được áp dụng tại thời điểm chấm công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedSettingsSnapshot {

    // Cấu hình làm tròn check-in
    private RoundingConfigSnapshot checkInRounding;

    // Cấu hình làm tròn check-out
    private RoundingConfigSnapshot checkOutRounding;

    // Số phút ân hạn đi muộn
    private Integer lateGraceMinutes;

    // Số phút ân hạn về sớm
    private Integer earlyLeaveGraceMinutes;

    // Cấu hình giải lao
    private BreakConfigSnapshot breakConfig;
}
