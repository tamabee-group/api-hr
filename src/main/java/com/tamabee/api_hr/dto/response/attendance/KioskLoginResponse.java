package com.tamabee.api_hr.dto.response.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response sau khi đăng nhập kiosk thành công
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KioskLoginResponse {

    private Long kioskId;
    private String kioskName;
    private Long locationId;
    private String locationName;
    private String locationAddress;
}
