package com.tamabee.api_hr.mapper.company;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.dto.request.*;
import com.tamabee.api_hr.dto.response.CompanySettingsResponse;
import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import com.tamabee.api_hr.exception.InternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa CompanySettingEntity và DTOs.
 * Xử lý serialize/deserialize JSON cho các config.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanySettingMapper {

    private final ObjectMapper objectMapper;

    /**
     * Tạo entity mới với default configs cho company
     */
    public CompanySettingEntity toEntity(Long companyId) {
        CompanySettingEntity entity = new CompanySettingEntity();
        entity.setCompanyId(companyId);
        entity.setAttendanceConfig(serializeConfig(AttendanceConfig.builder().build()));
        entity.setPayrollConfig(serializeConfig(PayrollConfig.builder().build()));
        entity.setOvertimeConfig(serializeConfig(OvertimeConfig.builder().build()));
        entity.setAllowanceConfig(serializeConfig(AllowanceConfig.builder().build()));
        entity.setDeductionConfig(serializeConfig(DeductionConfig.builder().build()));
        return entity;
    }

    /**
     * Chuyển entity sang response
     */
    public CompanySettingsResponse toResponse(CompanySettingEntity entity) {
        if (entity == null) {
            return null;
        }

        return CompanySettingsResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .attendanceConfig(deserializeAttendanceConfig(entity.getAttendanceConfig()))
                .payrollConfig(deserializePayrollConfig(entity.getPayrollConfig()))
                .overtimeConfig(deserializeOvertimeConfig(entity.getOvertimeConfig()))
                .allowanceConfig(deserializeAllowanceConfig(entity.getAllowanceConfig()))
                .deductionConfig(deserializeDeductionConfig(entity.getDeductionConfig()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Cập nhật attendance config từ request
     */
    public void updateAttendanceConfig(CompanySettingEntity entity, AttendanceConfigRequest request) {
        if (entity == null || request == null) {
            return;
        }

        AttendanceConfig config = deserializeAttendanceConfig(entity.getAttendanceConfig());
        if (config == null) {
            config = AttendanceConfig.builder().build();
        }

        if (request.getDefaultWorkStartTime() != null) {
            config.setDefaultWorkStartTime(request.getDefaultWorkStartTime());
        }
        if (request.getDefaultWorkEndTime() != null) {
            config.setDefaultWorkEndTime(request.getDefaultWorkEndTime());
        }
        if (request.getDefaultBreakMinutes() != null) {
            config.setDefaultBreakMinutes(request.getDefaultBreakMinutes());
        }
        if (request.getEnableRounding() != null) {
            config.setEnableRounding(request.getEnableRounding());
        }
        if (request.getCheckInRounding() != null) {
            config.setCheckInRounding(request.getCheckInRounding());
        }
        if (request.getCheckOutRounding() != null) {
            config.setCheckOutRounding(request.getCheckOutRounding());
        }
        if (request.getLateGraceMinutes() != null) {
            config.setLateGraceMinutes(request.getLateGraceMinutes());
        }
        if (request.getEarlyLeaveGraceMinutes() != null) {
            config.setEarlyLeaveGraceMinutes(request.getEarlyLeaveGraceMinutes());
        }
        if (request.getRequireDeviceRegistration() != null) {
            config.setRequireDeviceRegistration(request.getRequireDeviceRegistration());
        }
        if (request.getRequireGeoLocation() != null) {
            config.setRequireGeoLocation(request.getRequireGeoLocation());
        }
        if (request.getGeoFenceRadiusMeters() != null) {
            config.setGeoFenceRadiusMeters(request.getGeoFenceRadiusMeters());
        }
        if (request.getAllowMobileCheckIn() != null) {
            config.setAllowMobileCheckIn(request.getAllowMobileCheckIn());
        }
        if (request.getAllowWebCheckIn() != null) {
            config.setAllowWebCheckIn(request.getAllowWebCheckIn());
        }

        entity.setAttendanceConfig(serializeConfig(config));
    }

    /**
     * Cập nhật payroll config từ request
     */
    public void updatePayrollConfig(CompanySettingEntity entity, PayrollConfigRequest request) {
        if (entity == null || request == null) {
            return;
        }

        PayrollConfig config = deserializePayrollConfig(entity.getPayrollConfig());
        if (config == null) {
            config = PayrollConfig.builder().build();
        }

        if (request.getDefaultSalaryType() != null) {
            config.setDefaultSalaryType(request.getDefaultSalaryType());
        }
        if (request.getPayDay() != null) {
            config.setPayDay(request.getPayDay());
        }
        if (request.getCutoffDay() != null) {
            config.setCutoffDay(request.getCutoffDay());
        }
        if (request.getSalaryRounding() != null) {
            config.setSalaryRounding(request.getSalaryRounding());
        }
        if (request.getStandardWorkingDaysPerMonth() != null) {
            config.setStandardWorkingDaysPerMonth(request.getStandardWorkingDaysPerMonth());
        }
        if (request.getStandardWorkingHoursPerDay() != null) {
            config.setStandardWorkingHoursPerDay(request.getStandardWorkingHoursPerDay());
        }

        entity.setPayrollConfig(serializeConfig(config));
    }

    /**
     * Cập nhật overtime config từ request
     */
    public void updateOvertimeConfig(CompanySettingEntity entity, OvertimeConfigRequest request) {
        if (entity == null || request == null) {
            return;
        }

        OvertimeConfig config = deserializeOvertimeConfig(entity.getOvertimeConfig());
        if (config == null) {
            config = OvertimeConfig.builder().build();
        }

        if (request.getEnableOvertime() != null) {
            config.setOvertimeEnabled(request.getEnableOvertime());
        }
        if (request.getRequireApproval() != null) {
            config.setRequireApproval(request.getRequireApproval());
        }
        if (request.getRegularOvertimeRate() != null) {
            config.setRegularOvertimeRate(request.getRegularOvertimeRate());
        }
        if (request.getNightOvertimeRate() != null) {
            config.setNightOvertimeRate(request.getNightOvertimeRate());
        }
        if (request.getHolidayOvertimeRate() != null) {
            config.setHolidayOvertimeRate(request.getHolidayOvertimeRate());
        }
        if (request.getWeekendOvertimeRate() != null) {
            config.setWeekendOvertimeRate(request.getWeekendOvertimeRate());
        }
        if (request.getNightStartTime() != null) {
            config.setNightStartTime(request.getNightStartTime());
        }
        if (request.getNightEndTime() != null) {
            config.setNightEndTime(request.getNightEndTime());
        }
        if (request.getMaxOvertimeHoursPerDay() != null) {
            config.setMaxOvertimeHoursPerDay(request.getMaxOvertimeHoursPerDay());
        }
        if (request.getMaxOvertimeHoursPerMonth() != null) {
            config.setMaxOvertimeHoursPerMonth(request.getMaxOvertimeHoursPerMonth());
        }

        entity.setOvertimeConfig(serializeConfig(config));
    }

    /**
     * Cập nhật allowance config từ request
     */
    public void updateAllowanceConfig(CompanySettingEntity entity, AllowanceConfigRequest request) {
        if (entity == null || request == null) {
            return;
        }

        AllowanceConfig config = AllowanceConfig.builder()
                .allowances(request.getAllowances() != null ? request.getAllowances() : new java.util.ArrayList<>())
                .build();

        entity.setAllowanceConfig(serializeConfig(config));
    }

    /**
     * Cập nhật deduction config từ request
     */
    public void updateDeductionConfig(CompanySettingEntity entity, DeductionConfigRequest request) {
        if (entity == null || request == null) {
            return;
        }

        DeductionConfig config = deserializeDeductionConfig(entity.getDeductionConfig());
        if (config == null) {
            config = DeductionConfig.builder().build();
        }

        if (request.getDeductions() != null) {
            config.setDeductions(request.getDeductions());
        }
        if (request.getEnableLatePenalty() != null) {
            config.setEnableLatePenalty(request.getEnableLatePenalty());
        }
        if (request.getLatePenaltyPerMinute() != null) {
            config.setLatePenaltyPerMinute(request.getLatePenaltyPerMinute());
        }
        if (request.getEnableEarlyLeavePenalty() != null) {
            config.setEnableEarlyLeavePenalty(request.getEnableEarlyLeavePenalty());
        }
        if (request.getEarlyLeavePenaltyPerMinute() != null) {
            config.setEarlyLeavePenaltyPerMinute(request.getEarlyLeavePenaltyPerMinute());
        }
        if (request.getEnableAbsenceDeduction() != null) {
            config.setEnableAbsenceDeduction(request.getEnableAbsenceDeduction());
        }

        entity.setDeductionConfig(serializeConfig(config));
    }

    // ==================== Serialize/Deserialize Methods ====================

    /**
     * Serialize config object thành JSON string
     */
    public <T> String serializeConfig(T config) {
        if (config == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize config: {}", e.getMessage());
            throw new InternalServerException("Lỗi serialize config", e);
        }
    }

    /**
     * Deserialize JSON string thành AttendanceConfig
     */
    public AttendanceConfig deserializeAttendanceConfig(String json) {
        if (json == null || json.isEmpty()) {
            return AttendanceConfig.builder().build();
        }
        try {
            return objectMapper.readValue(json, AttendanceConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi deserialize attendance config: {}", e.getMessage());
            return AttendanceConfig.builder().build();
        }
    }

    /**
     * Deserialize JSON string thành PayrollConfig
     */
    public PayrollConfig deserializePayrollConfig(String json) {
        if (json == null || json.isEmpty()) {
            return PayrollConfig.builder().build();
        }
        try {
            return objectMapper.readValue(json, PayrollConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi deserialize payroll config: {}", e.getMessage());
            return PayrollConfig.builder().build();
        }
    }

    /**
     * Deserialize JSON string thành OvertimeConfig
     */
    public OvertimeConfig deserializeOvertimeConfig(String json) {
        if (json == null || json.isEmpty()) {
            return OvertimeConfig.builder().build();
        }
        try {
            return objectMapper.readValue(json, OvertimeConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi deserialize overtime config: {}", e.getMessage());
            return OvertimeConfig.builder().build();
        }
    }

    /**
     * Deserialize JSON string thành AllowanceConfig
     */
    public AllowanceConfig deserializeAllowanceConfig(String json) {
        if (json == null || json.isEmpty()) {
            return AllowanceConfig.builder().build();
        }
        try {
            return objectMapper.readValue(json, AllowanceConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi deserialize allowance config: {}", e.getMessage());
            return AllowanceConfig.builder().build();
        }
    }

    /**
     * Deserialize JSON string thành DeductionConfig
     */
    public DeductionConfig deserializeDeductionConfig(String json) {
        if (json == null || json.isEmpty()) {
            return DeductionConfig.builder().build();
        }
        try {
            return objectMapper.readValue(json, DeductionConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi deserialize deduction config: {}", e.getMessage());
            return DeductionConfig.builder().build();
        }
    }
}
