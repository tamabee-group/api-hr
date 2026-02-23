package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.tamabee.api_hr.enums.PreferencePriority;
import com.tamabee.api_hr.enums.PreferenceStatus;

import lombok.Data;

/**
 * Response DTO cho nguyện vọng ca làm việc.
 */
@Data
public class ShiftPreferenceResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Integer year;
    private Integer weekNumber;
    private Integer dayOfWeek;
    private ShiftTemplateResponse shiftTemplate; // nullable
    private LocalTime customStartTime; // nullable
    private LocalTime customEndTime; // nullable
    private String reason;
    private PreferencePriority priority;
    private PreferenceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
