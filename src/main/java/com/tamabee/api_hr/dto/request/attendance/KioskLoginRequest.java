package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request đăng nhập kiosk bằng PIN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KioskLoginRequest {

    @NotBlank
    private String pinCode;
}
