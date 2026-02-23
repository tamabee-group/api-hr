package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request chấm công qua kiosk bằng mã nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KioskCheckInRequest {

    @NotBlank
    private String employeeCode;
}
