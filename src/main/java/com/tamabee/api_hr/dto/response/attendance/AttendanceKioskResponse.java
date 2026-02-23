package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho máy chấm công cố định
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceKioskResponse {

    private Long id;
    private String name;
    private String pinCode;
    private Long locationId;
    private String locationName;
    private Boolean isActive;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
