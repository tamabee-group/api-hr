package com.tamabee.api_hr.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO đại diện cho một khoảng giải lao trong ngày
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakPeriod {

    // Tên khoảng giải lao (e.g., "Morning Break", "Lunch", "Afternoon Break")
    private String name;

    // Thời gian bắt đầu giải lao
    private LocalTime startTime;

    // Thời gian kết thúc giải lao
    private LocalTime endTime;

    // Thời lượng giải lao (phút)
    private Integer durationMinutes;

    // true = nhân viên tự chọn thời điểm giải lao trong khoảng cho phép
    @Builder.Default
    private Boolean isFlexible = false;

    // Thứ tự giải lao trong ngày (1, 2, 3...)
    private Integer order;
}
