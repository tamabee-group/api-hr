package com.tamabee.api_hr.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Chi tiết tăng ca theo ngày
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyOvertimeDetail {

    private LocalDate date;

    // Số phút tăng ca thường
    @Builder.Default
    private Integer regularMinutes = 0;

    // Số phút tăng ca đêm
    @Builder.Default
    private Integer nightMinutes = 0;

    // Có phải ngày lễ không
    @Builder.Default
    private Boolean isHoliday = false;

    // Có phải cuối tuần không
    @Builder.Default
    private Boolean isWeekend = false;
}
