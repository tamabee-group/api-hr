package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cập nhật máy chấm công cố định
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAttendanceKioskRequest {

    @Size(max = 200)
    private String name;

    @Size(min = 4, max = 10)
    private String pinCode;

    private Long locationId;

    private Boolean isActive;
}
