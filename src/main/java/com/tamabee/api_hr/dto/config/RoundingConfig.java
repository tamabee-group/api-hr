package com.tamabee.api_hr.dto.config;

import com.tamabee.api_hr.enums.RoundingDirection;
import com.tamabee.api_hr.enums.RoundingInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cấu hình làm tròn thời gian
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundingConfig {

    @Builder.Default
    private RoundingInterval interval = RoundingInterval.MINUTES_15;

    @Builder.Default
    private RoundingDirection direction = RoundingDirection.NEAREST;
}
