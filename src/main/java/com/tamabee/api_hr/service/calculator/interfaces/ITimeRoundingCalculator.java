package com.tamabee.api_hr.service.calculator.interfaces;

import com.tamabee.api_hr.dto.config.RoundingConfig;

import java.time.LocalDateTime;

/**
 * Interface cho việc làm tròn thời gian chấm công
 */
public interface ITimeRoundingCalculator {

    /**
     * Làm tròn thời gian theo cấu hình
     *
     * @param time   Thời gian cần làm tròn
     * @param config Cấu hình làm tròn (interval và direction)
     * @return Thời gian đã được làm tròn
     */
    LocalDateTime roundTime(LocalDateTime time, RoundingConfig config);
}
