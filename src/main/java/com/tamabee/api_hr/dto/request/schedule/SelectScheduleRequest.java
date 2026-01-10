package com.tamabee.api_hr.dto.request.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request để nhân viên chọn lịch làm việc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectScheduleRequest {

    // ID lịch làm việc muốn chọn
    @NotNull(message = "ID lịch làm việc không được để trống")
    private Long scheduleId;

    // Ngày bắt đầu áp dụng
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate effectiveFrom;

    // Ngày kết thúc áp dụng (null = vô thời hạn)
    private LocalDate effectiveTo;
}
