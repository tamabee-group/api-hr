package com.tamabee.api_hr.dto.request.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cho việc check-out chấm công
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckOutRequest {

    // Vị trí check-out (nếu yêu cầu geo-location)
    private Double latitude;
    private Double longitude;

    // Chấm công ngoài phạm vi cho phép
    private Boolean outOfRange;
}
