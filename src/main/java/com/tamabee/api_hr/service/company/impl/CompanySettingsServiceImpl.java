package com.tamabee.api_hr.service.company.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.dto.request.*;
import com.tamabee.api_hr.dto.response.CompanySettingsResponse;
import com.tamabee.api_hr.dto.response.WorkModeChangeLogResponse;
import com.tamabee.api_hr.dto.response.WorkModeConfigResponse;
import com.tamabee.api_hr.entity.audit.WorkModeChangeLogEntity;
import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.WorkMode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.InternalServerException;
import com.tamabee.api_hr.repository.CompanySettingsRepository;
import com.tamabee.api_hr.repository.WorkModeChangeLogRepository;
import com.tamabee.api_hr.repository.WorkScheduleRepository;
import com.tamabee.api_hr.service.company.cache.CompanySettingsCache;
import com.tamabee.api_hr.service.calculator.LegalBreakRequirements;
import com.tamabee.api_hr.service.calculator.LegalOvertimeRequirements;
import com.tamabee.api_hr.service.company.ICompanySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation quản lý cấu hình chấm công và tính lương của công ty.
 * Sử dụng JSON serialization để lưu trữ các config vào JSONB columns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanySettingsServiceImpl implements ICompanySettingsService {

    private final CompanySettingsRepository companySettingsRepository;
    private final WorkModeChangeLogRepository workModeChangeLogRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final ObjectMapper objectMapper;
    private final LegalBreakRequirements legalBreakRequirements;
    private final LegalOvertimeRequirements legalOvertimeRequirements;
    private final ObjectProvider<CompanySettingsCache> settingsCacheProvider;

    /**
     * Lấy cache instance, trả về null nếu không có request context
     */
    private CompanySettingsCache getCache() {
        try {
            return settingsCacheProvider.getIfAvailable();
        } catch (Exception e) {
            log.debug("No request context available for cache");
            return null;
        }
    }

    /**
     * Invalidate cache nếu có
     */
    private void invalidateCacheIfAvailable() {
        CompanySettingsCache cache = getCache();
        if (cache != null) {
            cache.invalidate();
        }
    }

    @Override
    @Transactional
    public CompanySettingsResponse getSettings() {
        CompanySettingEntity entity = findSettings();
        return toResponse(entity);
    }

    @Override
    @Transactional
    public WorkModeConfigResponse getWorkModeConfig() {
        CompanySettingEntity entity = findSettings();
        return toWorkModeConfigResponse(entity);
    }

    @Override
    @Transactional
    public WorkModeConfigResponse updateWorkModeConfig(WorkModeConfigRequest request,
            String changedBy) {
        CompanySettingEntity entity = findSettings();
        WorkMode previousMode = entity.getWorkMode();
        WorkMode newMode = request.getMode();

        // Validate request
        validateWorkModeConfig(request);

        // Nếu mode thay đổi, tạo audit log và xử lý schedules
        if (previousMode != newMode) {
            // Nếu switch sang FIXED_HOURS, đánh dấu tất cả schedules là inactive
            if (newMode == WorkMode.FIXED_HOURS) {
                deactivateAllSchedules();
            } else {
                // Nếu switch sang FLEXIBLE_SHIFT, reactivate schedules
                reactivateAllSchedules();
            }

            // Tạo audit log
            createWorkModeChangeLog(previousMode, newMode, changedBy, request.getReason());
        }

        // Cập nhật entity
        entity.setWorkMode(newMode);
        entity.setDefaultWorkStartTime(request.getDefaultWorkStartTime());
        entity.setDefaultWorkEndTime(request.getDefaultWorkEndTime());
        entity.setDefaultBreakMinutes(request.getDefaultBreakMinutes());

        companySettingsRepository.save(entity);

        // Invalidate cache
        invalidateCacheIfAvailable();

        log.info("Đã cập nhật work mode: {} -> {}", previousMode, newMode);

        return toWorkModeConfigResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkModeChangeLogResponse> getWorkModeChangeLogs() {
        List<WorkModeChangeLogEntity> logs = workModeChangeLogRepository
                .findAllByOrderByChangedAtDesc();
        return logs.stream()
                .map(this::toWorkModeChangeLogResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AttendanceConfig updateAttendanceConfig(AttendanceConfigRequest request) {
        CompanySettingEntity entity = findSettings();

        // Lấy config hiện tại hoặc tạo mới
        AttendanceConfig config = deserializeConfig(entity.getAttendanceConfig(), AttendanceConfig.class);
        if (config == null) {
            config = AttendanceConfig.builder().build();
        }

        // Cập nhật các field được gửi lên
        updateAttendanceConfigFields(config, request);

        // Validate config
        validateAttendanceConfig(config);

        // Serialize và lưu
        entity.setAttendanceConfig(serializeConfig(config));
        companySettingsRepository.save(entity);

        // Invalidate cache
        invalidateCacheIfAvailable();

        return config;
    }

    @Override
    @Transactional
    public PayrollConfig updatePayrollConfig(PayrollConfigRequest request) {
        CompanySettingEntity entity = findSettings();

        PayrollConfig config = deserializeConfig(entity.getPayrollConfig(), PayrollConfig.class);
        if (config == null) {
            config = PayrollConfig.builder().build();
        }

        updatePayrollConfigFields(config, request);

        entity.setPayrollConfig(serializeConfig(config));
        companySettingsRepository.save(entity);

        // Invalidate cache
        invalidateCacheIfAvailable();

        return config;
    }

    @Override
    @Transactional
    public OvertimeConfig updateOvertimeConfig(OvertimeConfigRequest request) {
        CompanySettingEntity entity = findSettings();

        OvertimeConfig config = deserializeConfig(entity.getOvertimeConfig(), OvertimeConfig.class);
        if (config == null) {
            config = OvertimeConfig.builder().build();
        }

        updateOvertimeConfigFields(config, request);

        // Validate overtime config
        validateOvertimeConfig(config);

        entity.setOvertimeConfig(serializeConfig(config));
        companySettingsRepository.save(entity);

        // Invalidate cache
        invalidateCacheIfAvailable();

        return config;
    }

    @Override
    @Transactional
    public AllowanceConfig updateAllowanceConfig(AllowanceConfigRequest request) {
        CompanySettingEntity entity = findSettings();

        AllowanceConfig config = deserializeConfig(entity.getAllowanceConfig(), AllowanceConfig.class);
        if (config == null) {
            config = AllowanceConfig.builder().build();
        }

        if (request.getAllowances() != null) {
            config.setAllowances(request.getAllowances());
        }

        entity.setAllowanceConfig(serializeConfig(config));
        companySettingsRepository.save(entity);

        // Invalidate cache
        invalidateCacheIfAvailable();

        return config;
    }

    @Override
    @Transactional
    public DeductionConfig updateDeductionConfig(DeductionConfigRequest request) {
        CompanySettingEntity entity = findSettings();

        DeductionConfig config = deserializeConfig(entity.getDeductionConfig(), DeductionConfig.class);
        if (config == null) {
            config = DeductionConfig.builder().build();
        }

        updateDeductionConfigFields(config, request);

        entity.setDeductionConfig(serializeConfig(config));
        companySettingsRepository.save(entity);

        // Invalidate cache
        invalidateCacheIfAvailable();

        return config;
    }

    @Override
    @Transactional
    public void initializeDefaultSettings() {
        // Kiểm tra đã tồn tại chưa
        if (companySettingsRepository.existsByDeletedFalse()) {
            throw new ConflictException("Cấu hình công ty đã tồn tại", ErrorCode.SETTINGS_ALREADY_EXISTS);
        }

        // Tạo entity mới với default configs
        CompanySettingEntity entity = new CompanySettingEntity();
        entity.setAttendanceConfig(serializeConfig(AttendanceConfig.builder().build()));
        entity.setPayrollConfig(serializeConfig(PayrollConfig.builder().build()));
        entity.setOvertimeConfig(serializeConfig(OvertimeConfig.builder().build()));
        entity.setAllowanceConfig(serializeConfig(AllowanceConfig.builder().build()));
        entity.setDeductionConfig(serializeConfig(DeductionConfig.builder().build()));

        companySettingsRepository.save(entity);
        log.info("Đã khởi tạo cấu hình mặc định cho tenant");
    }

    @Override
    @Transactional
    public AttendanceConfig getAttendanceConfig() {
        CompanySettingEntity entity = findSettings();
        AttendanceConfig config = deserializeConfig(entity.getAttendanceConfig(), AttendanceConfig.class);
        return config != null ? config : AttendanceConfig.builder().build();
    }

    @Override
    @Transactional
    public PayrollConfig getPayrollConfig() {
        CompanySettingEntity entity = findSettings();
        PayrollConfig config = deserializeConfig(entity.getPayrollConfig(), PayrollConfig.class);
        return config != null ? config : PayrollConfig.builder().build();
    }

    @Override
    @Transactional
    public OvertimeConfig getOvertimeConfig() {
        CompanySettingEntity entity = findSettings();
        OvertimeConfig config = deserializeConfig(entity.getOvertimeConfig(), OvertimeConfig.class);
        return config != null ? config : OvertimeConfig.builder().build();
    }

    @Override
    @Transactional
    public AllowanceConfig getAllowanceConfig() {
        CompanySettingEntity entity = findSettings();
        AllowanceConfig config = deserializeConfig(entity.getAllowanceConfig(), AllowanceConfig.class);
        return config != null ? config : AllowanceConfig.builder().build();
    }

    @Override
    @Transactional
    public DeductionConfig getDeductionConfig() {
        CompanySettingEntity entity = findSettings();
        DeductionConfig config = deserializeConfig(entity.getDeductionConfig(), DeductionConfig.class);
        return config != null ? config : DeductionConfig.builder().build();
    }

    @Override
    @Transactional
    public BreakConfig getBreakConfig() {
        CompanySettingEntity entity = findSettings();
        BreakConfig config = deserializeConfig(entity.getBreakConfig(), BreakConfig.class);
        return config != null ? config : BreakConfig.builder().build();
    }

    @Override
    @Transactional
    public BreakConfig updateBreakConfig(BreakConfigRequest request) {
        CompanySettingEntity entity = findSettings();

        BreakConfig config = deserializeConfig(entity.getBreakConfig(), BreakConfig.class);
        if (config == null) {
            config = BreakConfig.builder().build();
        }

        updateBreakConfigFields(config, request);

        // Validate break config
        validateBreakConfig(config);

        entity.setBreakConfig(serializeConfig(config));
        companySettingsRepository.save(entity);

        // Invalidate cache
        invalidateCacheIfAvailable();

        return config;
    }

    // ==================== Private helper methods ====================

    /**
     * Tìm settings của tenant hiện tại, tự động tạo nếu chưa có
     */
    private CompanySettingEntity findSettings() {
        return companySettingsRepository.findFirstByDeletedFalse()
                .orElseGet(this::createDefaultSettings);
    }

    /**
     * Tạo default settings cho tenant mới
     */
    private CompanySettingEntity createDefaultSettings() {
        log.info("Tạo default settings cho tenant");
        CompanySettingEntity entity = new CompanySettingEntity();

        // Tạo default configs
        AttendanceConfig attendanceConfig = AttendanceConfig.builder()
                .enableRounding(false)
                .requireDeviceRegistration(false)
                .requireGeoLocation(false)
                .lateGraceMinutes(0)
                .earlyLeaveGraceMinutes(0)
                .build();
        entity.setAttendanceConfig(serializeConfig(attendanceConfig));

        BreakConfig breakConfig = BreakConfig.builder()
                .breakTrackingEnabled(false)
                .defaultBreakMinutes(60)
                .build();
        entity.setBreakConfig(serializeConfig(breakConfig));

        PayrollConfig payrollConfig = PayrollConfig.builder()
                .build();
        entity.setPayrollConfig(serializeConfig(payrollConfig));

        OvertimeConfig overtimeConfig = OvertimeConfig.builder()
                .overtimeEnabled(false)
                .build();
        entity.setOvertimeConfig(serializeConfig(overtimeConfig));

        return companySettingsRepository.save(entity);
    }

    /**
     * Serialize config object thành JSON string
     */
    private String serializeConfig(Object config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize config: {}", e.getMessage());
            throw new InternalServerException("Lỗi serialize config", e);
        }
    }

    /**
     * Deserialize JSON string thành config object
     */
    private <T> T deserializeConfig(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Lỗi deserialize config: {}", e.getMessage());
            throw new InternalServerException("Lỗi deserialize config", e);
        }
    }

    /**
     * Chuyển entity thành response
     */
    private CompanySettingsResponse toResponse(CompanySettingEntity entity) {
        // Lấy thông tin work mode change log gần nhất
        List<WorkModeChangeLogEntity> logs = workModeChangeLogRepository
                .findAllByOrderByChangedAtDesc();
        LocalDateTime lastModeChangeAt = logs.isEmpty() ? null : logs.get(0).getChangedAt();
        String lastModeChangeBy = logs.isEmpty() ? null : logs.get(0).getChangedBy();

        WorkModeConfig workModeConfig = WorkModeConfig.builder()
                .mode(entity.getWorkMode())
                .defaultWorkStartTime(entity.getDefaultWorkStartTime())
                .defaultWorkEndTime(entity.getDefaultWorkEndTime())
                .defaultBreakMinutes(entity.getDefaultBreakMinutes())
                .lastModeChangeAt(lastModeChangeAt)
                .lastModeChangeBy(lastModeChangeBy)
                .build();

        return CompanySettingsResponse.builder()
                .id(entity.getId())
                .workModeConfig(workModeConfig)
                .attendanceConfig(deserializeConfig(entity.getAttendanceConfig(), AttendanceConfig.class))
                .breakConfig(deserializeConfig(entity.getBreakConfig(), BreakConfig.class))
                .payrollConfig(deserializeConfig(entity.getPayrollConfig(), PayrollConfig.class))
                .overtimeConfig(deserializeConfig(entity.getOvertimeConfig(), OvertimeConfig.class))
                .allowanceConfig(deserializeConfig(entity.getAllowanceConfig(), AllowanceConfig.class))
                .deductionConfig(deserializeConfig(entity.getDeductionConfig(), DeductionConfig.class))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển entity thành WorkModeConfigResponse
     */
    private WorkModeConfigResponse toWorkModeConfigResponse(CompanySettingEntity entity) {
        // Lấy thông tin work mode change log gần nhất
        List<WorkModeChangeLogEntity> logs = workModeChangeLogRepository
                .findAllByOrderByChangedAtDesc();
        LocalDateTime lastModeChangeAt = logs.isEmpty() ? null : logs.get(0).getChangedAt();
        String lastModeChangeBy = logs.isEmpty() ? null : logs.get(0).getChangedBy();

        return WorkModeConfigResponse.builder()
                .mode(entity.getWorkMode())
                .defaultWorkStartTime(entity.getDefaultWorkStartTime())
                .defaultWorkEndTime(entity.getDefaultWorkEndTime())
                .defaultBreakMinutes(entity.getDefaultBreakMinutes())
                .lastModeChangeAt(lastModeChangeAt)
                .lastModeChangeBy(lastModeChangeBy)
                .build();
    }

    /**
     * Chuyển WorkModeChangeLogEntity thành response
     */
    private WorkModeChangeLogResponse toWorkModeChangeLogResponse(WorkModeChangeLogEntity entity) {
        return WorkModeChangeLogResponse.builder()
                .id(entity.getId())
                .previousMode(entity.getPreviousMode())
                .newMode(entity.getNewMode())
                .changedBy(entity.getChangedBy())
                .changedAt(entity.getChangedAt())
                .reason(entity.getReason())
                .build();
    }

    /**
     * Validate work mode config request
     */
    private void validateWorkModeConfig(WorkModeConfigRequest request) {
        if (request.getMode() == WorkMode.FIXED_HOURS) {
            // FIXED_HOURS mode yêu cầu cấu hình giờ làm việc mặc định
            if (request.getDefaultWorkStartTime() == null || request.getDefaultWorkEndTime() == null) {
                throw new BadRequestException(
                        "Chế độ giờ cố định yêu cầu cấu hình giờ làm việc mặc định",
                        ErrorCode.FIXED_HOURS_MISSING_CONFIG);
            }
            // Validate giờ bắt đầu phải trước giờ kết thúc
            if (!request.getDefaultWorkStartTime().isBefore(request.getDefaultWorkEndTime())) {
                throw new BadRequestException("Giờ bắt đầu phải trước giờ kết thúc", ErrorCode.INVALID_WORK_TIME);
            }
        }
    }

    /**
     * Đánh dấu tất cả schedules là inactive
     * Được gọi khi switch sang FIXED_HOURS mode
     */
    private void deactivateAllSchedules() {
        workScheduleRepository.findByDeletedFalse(org.springframework.data.domain.Pageable.unpaged())
                .forEach(schedule -> {
                    schedule.setIsActive(false);
                    workScheduleRepository.save(schedule);
                });
        log.info("Đã đánh dấu tất cả schedules là inactive");
    }

    /**
     * Reactivate tất cả schedules
     * Được gọi khi switch sang FLEXIBLE_SHIFT mode
     */
    private void reactivateAllSchedules() {
        workScheduleRepository.findByDeletedFalse(org.springframework.data.domain.Pageable.unpaged())
                .forEach(schedule -> {
                    schedule.setIsActive(true);
                    workScheduleRepository.save(schedule);
                });
        log.info("Đã reactivate tất cả schedules");
    }

    /**
     * Tạo audit log khi work mode thay đổi
     */
    private void createWorkModeChangeLog(WorkMode previousMode, WorkMode newMode,
            String changedBy, String reason) {
        WorkModeChangeLogEntity logEntity = new WorkModeChangeLogEntity();
        logEntity.setPreviousMode(previousMode);
        logEntity.setNewMode(newMode);
        logEntity.setChangedBy(changedBy);
        logEntity.setChangedAt(LocalDateTime.now());
        logEntity.setReason(reason);
        workModeChangeLogRepository.save(logEntity);
        log.info("Đã tạo audit log cho thay đổi work mode: {} -> {}", previousMode, newMode);
    }

    /**
     * Validate attendance config
     */
    private void validateAttendanceConfig(AttendanceConfig config) {
        if (config.getDefaultWorkStartTime() != null && config.getDefaultWorkEndTime() != null) {
            if (!config.getDefaultWorkStartTime().isBefore(config.getDefaultWorkEndTime())) {
                throw new BadRequestException("Giờ bắt đầu phải trước giờ kết thúc", ErrorCode.INVALID_WORK_TIME);
            }
        }
    }

    /**
     * Cập nhật các field của AttendanceConfig từ request
     */
    private void updateAttendanceConfigFields(AttendanceConfig config, AttendanceConfigRequest request) {
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
        // Individual rounding toggles
        if (request.getEnableCheckInRounding() != null) {
            config.setEnableCheckInRounding(request.getEnableCheckInRounding());
        }
        if (request.getEnableCheckOutRounding() != null) {
            config.setEnableCheckOutRounding(request.getEnableCheckOutRounding());
        }
        if (request.getEnableBreakStartRounding() != null) {
            config.setEnableBreakStartRounding(request.getEnableBreakStartRounding());
        }
        if (request.getEnableBreakEndRounding() != null) {
            config.setEnableBreakEndRounding(request.getEnableBreakEndRounding());
        }
        // Rounding configs
        if (request.getCheckInRounding() != null) {
            config.setCheckInRounding(request.getCheckInRounding());
        }
        if (request.getCheckOutRounding() != null) {
            config.setCheckOutRounding(request.getCheckOutRounding());
        }
        if (request.getBreakStartRounding() != null) {
            config.setBreakStartRounding(request.getBreakStartRounding());
        }
        if (request.getBreakEndRounding() != null) {
            config.setBreakEndRounding(request.getBreakEndRounding());
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
    }

    /**
     * Cập nhật các field của PayrollConfig từ request
     */
    private void updatePayrollConfigFields(PayrollConfig config, PayrollConfigRequest request) {
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
    }

    /**
     * Cập nhật các field của OvertimeConfig từ request
     */
    private void updateOvertimeConfigFields(OvertimeConfig config, OvertimeConfigRequest request) {
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
    }

    /**
     * Cập nhật các field của DeductionConfig từ request
     */
    private void updateDeductionConfigFields(DeductionConfig config, DeductionConfigRequest request) {
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
    }

    /**
     * Cập nhật các field của BreakConfig từ request
     */
    private void updateBreakConfigFields(BreakConfig config, BreakConfigRequest request) {
        if (request.getBreakEnabled() != null) {
            config.setBreakEnabled(request.getBreakEnabled());
        }
        if (request.getBreakType() != null) {
            config.setBreakType(request.getBreakType());
        }
        if (request.getDefaultBreakMinutes() != null) {
            config.setDefaultBreakMinutes(request.getDefaultBreakMinutes());
        }
        if (request.getMinimumBreakMinutes() != null) {
            config.setMinimumBreakMinutes(request.getMinimumBreakMinutes());
        }
        if (request.getMaximumBreakMinutes() != null) {
            config.setMaximumBreakMinutes(request.getMaximumBreakMinutes());
        }
        if (request.getUseLegalMinimum() != null) {
            config.setUseLegalMinimum(request.getUseLegalMinimum());
        }
        if (request.getBreakTrackingEnabled() != null) {
            config.setBreakTrackingEnabled(request.getBreakTrackingEnabled());
        }
        if (request.getLocale() != null) {
            config.setLocale(request.getLocale());
        }
        if (request.getFixedBreakMode() != null) {
            config.setFixedBreakMode(request.getFixedBreakMode());
        }
        if (request.getBreakPeriodsPerAttendance() != null) {
            config.setBreakPeriodsPerAttendance(request.getBreakPeriodsPerAttendance());
        }
        if (request.getMaxBreaksPerDay() != null) {
            config.setMaxBreaksPerDay(request.getMaxBreaksPerDay());
        }
        if (request.getFixedBreakPeriods() != null) {
            config.setFixedBreakPeriods(request.getFixedBreakPeriods());
        }
        if (request.getNightShiftStartTime() != null) {
            config.setNightShiftStartTime(request.getNightShiftStartTime());
        }
        if (request.getNightShiftEndTime() != null) {
            config.setNightShiftEndTime(request.getNightShiftEndTime());
        }
        if (request.getNightShiftMinimumBreakMinutes() != null) {
            config.setNightShiftMinimumBreakMinutes(request.getNightShiftMinimumBreakMinutes());
        }
        if (request.getNightShiftDefaultBreakMinutes() != null) {
            config.setNightShiftDefaultBreakMinutes(request.getNightShiftDefaultBreakMinutes());
        }
    }

    /**
     * Validate break config
     */
    private void validateBreakConfig(BreakConfig config) {
        // Validate minimum không vượt quá maximum
        if (config.getMinimumBreakMinutes() != null && config.getMaximumBreakMinutes() != null) {
            if (config.getMinimumBreakMinutes() > config.getMaximumBreakMinutes()) {
                throw new BadRequestException(
                        "Thời gian giải lao tối thiểu không được vượt quá tối đa",
                        ErrorCode.INVALID_BREAK_CONFIG);
            }
        }

        // Validate default break nằm trong khoảng min-max
        if (config.getDefaultBreakMinutes() != null) {
            if (config.getMinimumBreakMinutes() != null &&
                    config.getDefaultBreakMinutes() < config.getMinimumBreakMinutes()) {
                throw new BadRequestException(
                        "Thời gian giải lao mặc định không được nhỏ hơn tối thiểu",
                        ErrorCode.INVALID_BREAK_CONFIG);
            }
            if (config.getMaximumBreakMinutes() != null &&
                    config.getDefaultBreakMinutes() > config.getMaximumBreakMinutes()) {
                throw new BadRequestException(
                        "Thời gian giải lao mặc định không được lớn hơn tối đa",
                        ErrorCode.INVALID_BREAK_CONFIG);
            }
        }

        // Validate legal minimum compliance nếu useLegalMinimum = true
        if (Boolean.TRUE.equals(config.getUseLegalMinimum())) {
            String locale = config.getLocale() != null ? config.getLocale() : "ja";
            // Kiểm tra với 8 giờ làm việc (trường hợp phổ biến)
            int legalMinimum = legalBreakRequirements.getMinimumBreak(locale, 8, false);

            if (config.getMinimumBreakMinutes() != null &&
                    config.getMinimumBreakMinutes() < legalMinimum) {
                throw new BadRequestException(
                        "Thời gian giải lao tối thiểu không đạt yêu cầu pháp luật (" + legalMinimum + " phút)",
                        ErrorCode.BREAK_BELOW_LEGAL_MINIMUM);
            }
        }

        // Validate night shift break
        if (config.getNightShiftMinimumBreakMinutes() != null &&
                config.getNightShiftDefaultBreakMinutes() != null) {
            if (config.getNightShiftDefaultBreakMinutes() < config.getNightShiftMinimumBreakMinutes()) {
                throw new BadRequestException(
                        "Thời gian giải lao ca đêm mặc định không được nhỏ hơn tối thiểu",
                        ErrorCode.INVALID_BREAK_CONFIG);
            }
        }
    }

    /**
     * Validate overtime config
     */
    private void validateOvertimeConfig(OvertimeConfig config) {
        // Validate night hours
        if (config.getNightStartTime() != null && config.getNightEndTime() != null) {
            // Night hours thường qua đêm (22:00 - 05:00) nên không cần validate start < end
        }

        // Validate legal minimum compliance nếu useLegalMinimum = true
        if (Boolean.TRUE.equals(config.getUseLegalMinimum())) {
            String locale = config.getLocale() != null ? config.getLocale() : "ja";
            OvertimeMultipliers legalMinimum = legalOvertimeRequirements.getMinimumMultipliers(locale);

            // Validate từng multiplier
            if (config.getRegularOvertimeRate() != null &&
                    config.getRegularOvertimeRate().compareTo(legalMinimum.getRegularOvertime()) < 0) {
                throw new BadRequestException(
                        "Hệ số tăng ca thường không đạt yêu cầu pháp luật (" + legalMinimum.getRegularOvertime() + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }

            if (config.getNightWorkRate() != null &&
                    config.getNightWorkRate().compareTo(legalMinimum.getNightWork()) < 0) {
                throw new BadRequestException(
                        "Hệ số làm đêm không đạt yêu cầu pháp luật (" + legalMinimum.getNightWork() + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }

            if (config.getNightOvertimeRate() != null &&
                    config.getNightOvertimeRate().compareTo(legalMinimum.getNightOvertime()) < 0) {
                throw new BadRequestException(
                        "Hệ số tăng ca đêm không đạt yêu cầu pháp luật (" + legalMinimum.getNightOvertime() + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }

            if (config.getHolidayOvertimeRate() != null &&
                    config.getHolidayOvertimeRate().compareTo(legalMinimum.getHolidayOvertime()) < 0) {
                throw new BadRequestException(
                        "Hệ số tăng ca ngày lễ không đạt yêu cầu pháp luật (" + legalMinimum.getHolidayOvertime() + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }

            if (config.getHolidayNightOvertimeRate() != null &&
                    config.getHolidayNightOvertimeRate().compareTo(legalMinimum.getHolidayNightOvertime()) < 0) {
                throw new BadRequestException(
                        "Hệ số tăng ca đêm ngày lễ không đạt yêu cầu pháp luật ("
                                + legalMinimum.getHolidayNightOvertime() + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }

            if (config.getWeekendOvertimeRate() != null &&
                    config.getWeekendOvertimeRate().compareTo(legalMinimum.getWeekendOvertime()) < 0) {
                throw new BadRequestException(
                        "Hệ số tăng ca cuối tuần không đạt yêu cầu pháp luật (" + legalMinimum.getWeekendOvertime()
                                + ")",
                        ErrorCode.OVERTIME_RATE_BELOW_LEGAL_MINIMUM);
            }
        }
    }
}
