package com.tamabee.api_hr.dto.response.company;

import java.time.LocalDateTime;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.PayrollConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private AttendanceConfig attendanceConfig;
    private BreakConfig breakConfig;
    private PayrollConfig payrollConfig;
    private OvertimeConfig overtimeConfig;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
