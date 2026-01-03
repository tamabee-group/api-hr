package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.dto.config.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response chứa toàn bộ cấu hình của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySettingsResponse {

    private Long id;
    private Long companyId;
    private WorkModeConfig workModeConfig;
    private AttendanceConfig attendanceConfig;
    private PayrollConfig payrollConfig;
    private OvertimeConfig overtimeConfig;
    private AllowanceConfig allowanceConfig;
    private DeductionConfig deductionConfig;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
