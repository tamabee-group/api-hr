package com.tamabee.api_hr.dto.request.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cho việc check-in chấm công
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {

    // Vị trí check-in (nếu yêu cầu geo-location)
    private Double latitude;
    private Double longitude;

    // Chấm công ngoài phạm vi cho phép
    private Boolean outOfRange;
}
