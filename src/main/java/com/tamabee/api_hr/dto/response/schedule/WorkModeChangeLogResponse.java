package com.tamabee.api_hr.dto.response.schedule;

import com.tamabee.api_hr.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response chứa thông tin audit log thay đổi work mode.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkModeChangeLogResponse {

    private Long id;
    private Long companyId;
    private WorkMode previousMode;
    private WorkMode newMode;
    private String changedBy;
    private LocalDateTime changedAt;
    private String reason;
}
