package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request tạo máy chấm công cố định
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttendanceKioskRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(min = 4, max = 10)
    private String pinCode;

    @NotNull
    private Long locationId;

    private Boolean isActive;
}
