package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cập nhật lịch làm việc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkScheduleRequest {

    private String name;

    private ScheduleType type;

    private Boolean isDefault;

    private WorkScheduleData scheduleData;

    private String description;
}
