package com.tamabee.api_hr.dto.request.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để kết thúc giờ giải lao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndBreakRequest {

    // Vị trí kết thúc giải lao (nếu yêu cầu geo-location)
    private Double latitude;
    private Double longitude;

    // Giải lao ngoài phạm vi cho phép
    private Boolean outOfRange;
}

