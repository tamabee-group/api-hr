package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cập nhật cấu hình giờ giải lao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakConfigRequest {

    private Boolean breakEnabled;

    @Min(0)
    @Max(480)
    private Integer defaultBreakMinutes;

    @Min(1)
    @Max(10)
    private Integer maxBreaksPerDay;
}
