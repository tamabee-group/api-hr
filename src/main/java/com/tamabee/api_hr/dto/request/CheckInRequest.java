package com.tamabee.api_hr.dto.request;

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

    // ID thiết bị (nếu yêu cầu đăng ký thiết bị)
    private String deviceId;

    // Vị trí check-in (nếu yêu cầu geo-location)
    private Double latitude;
    private Double longitude;
}
