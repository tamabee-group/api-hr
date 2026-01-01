package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response trả về thông tin lịch làm việc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkScheduleResponse {

    private Long id;

    private Long companyId;

    private String name;

    private ScheduleType type;

    private Boolean isDefault;

    private WorkScheduleData scheduleData;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
