package com.tamabee.api_hr.dto.response.attendance;

import com.tamabee.api_hr.enums.PreferencePriority;

import lombok.Data;

/**
 * DTO tóm tắt nguyện vọng của một nhân viên.
 */
@Data
public class EmployeePreferenceSummary {

    private Long employeeId;
    private String employeeName;
    private String reason;
    private PreferencePriority priority;
    private String customStartTime;
    private String customEndTime;
}
