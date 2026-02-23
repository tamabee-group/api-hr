package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho hoạt động gần đây trên kiosk
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KioskActivityResponse {

    private String employeeName;
    private String action; // CHECK_IN, CHECK_OUT, BREAK_START, BREAK_END
    private LocalDateTime timestamp;
}
