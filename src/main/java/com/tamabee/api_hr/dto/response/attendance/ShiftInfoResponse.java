package com.tamabee.api_hr.dto.response.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * DTO response cho thông tin ca làm việc.
 * Chứa thông tin về ca làm việc được gán cho nhân viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftInfoResponse {

    // ID của shift template
    private Long shiftTemplateId;

    // Tên ca làm việc
    private String shiftName;

    // Thời gian bắt đầu ca theo lịch
    private LocalTime scheduledStart;

    // Thời gian kết thúc ca theo lịch
    private LocalTime scheduledEnd;

    // Hệ số lương (1.0, 1.5, 2.0...)
    private BigDecimal multiplier;
}
