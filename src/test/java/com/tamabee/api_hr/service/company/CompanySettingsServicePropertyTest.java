package com.tamabee.api_hr.service.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.repository.CompanySettingsRepository;
import com.tamabee.api_hr.repository.WorkModeChangeLogRepository;
import com.tamabee.api_hr.repository.WorkScheduleRepository;
import com.tamabee.api_hr.service.calculator.LegalBreakRequirements;
import com.tamabee.api_hr.service.calculator.LegalOvertimeRequirements;
import com.tamabee.api_hr.service.company.cache.CompanySettingsCache;
import com.tamabee.api_hr.service.company.impl.CompanySettingsServiceImpl;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho CompanySettingsService
 * Feature: attendance-payroll-backend, Property 2: Default Configuration
 * Initialization
 */
public class CompanySettingsServicePropertyTest {

    private final ObjectMapper objectMapper;
    private final LegalBreakRequirements legalBreakRequirements;
    private final LegalOvertimeRequirements legalOvertimeRequirements;

    public CompanySettingsServicePropertyTest() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.legalBreakRequirements = new LegalBreakRequirements();
        this.legalOvertimeRequirements = new LegalOvertimeRequirements();
    }

    /**
     * Property 2: Default Configuration Initialization
     * For any newly created company, the Company_Settings_Service SHALL create
     * settings
     * with all required fields having non-null default values.
     */
    @Property(tries = 100)
    void initializeDefaultSettingsCreatesNonNullDefaults(@ForAll("companyIds") Long companyId) {
        // Arrange
        CompanySettingsRepository mockRepository = mock(CompanySettingsRepository.class);
        WorkModeChangeLogRepository mockLogRepo = mock(WorkModeChangeLogRepository.class);
        WorkScheduleRepository mockScheduleRepo = mock(WorkScheduleRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<CompanySettingsCache> mockCacheProvider = mock(ObjectProvider.class);
        when(mockCacheProvider.getIfAvailable()).thenReturn(null);

        when(mockRepository.existsByCompanyIdAndDeletedFalse(companyId)).thenReturn(false);
        when(mockRepository.save(any(CompanySettingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanySettingsServiceImpl service = new CompanySettingsServiceImpl(
                mockRepository, mockLogRepo, mockScheduleRepo, objectMapper,
                legalBreakRequirements, legalOvertimeRequirements, mockCacheProvider);

        // Act
        service.initializeDefaultSettings(companyId);

        // Assert - Capture saved entity
        ArgumentCaptor<CompanySettingEntity> captor = ArgumentCaptor.forClass(CompanySettingEntity.class);
        verify(mockRepository).save(captor.capture());

        CompanySettingEntity savedEntity = captor.getValue();

        // Verify companyId is set
        assertEquals(companyId, savedEntity.getCompanyId());

        // Verify all configs are non-null JSON strings
        assertNotNull(savedEntity.getAttendanceConfig(), "AttendanceConfig should not be null");
        assertNotNull(savedEntity.getPayrollConfig(), "PayrollConfig should not be null");
        assertNotNull(savedEntity.getOvertimeConfig(), "OvertimeConfig should not be null");
        assertNotNull(savedEntity.getAllowanceConfig(), "AllowanceConfig should not be null");
        assertNotNull(savedEntity.getDeductionConfig(), "DeductionConfig should not be null");

        // Verify configs can be deserialized and have default values
        verifyAttendanceConfigDefaults(savedEntity.getAttendanceConfig());
        verifyPayrollConfigDefaults(savedEntity.getPayrollConfig());
        verifyOvertimeConfigDefaults(savedEntity.getOvertimeConfig());
        verifyAllowanceConfigDefaults(savedEntity.getAllowanceConfig());
        verifyDeductionConfigDefaults(savedEntity.getDeductionConfig());
    }

    /**
     * Property bổ sung: Không thể khởi tạo settings cho công ty đã có settings
     */
    @Property(tries = 100)
    void initializeDefaultSettingsThrowsWhenAlreadyExists(@ForAll("companyIds") Long companyId) {
        // Arrange
        CompanySettingsRepository mockRepository = mock(CompanySettingsRepository.class);
        WorkModeChangeLogRepository mockLogRepo = mock(WorkModeChangeLogRepository.class);
        WorkScheduleRepository mockScheduleRepo = mock(WorkScheduleRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<CompanySettingsCache> mockCacheProvider = mock(ObjectProvider.class);
        when(mockCacheProvider.getIfAvailable()).thenReturn(null);

        when(mockRepository.existsByCompanyIdAndDeletedFalse(companyId)).thenReturn(true);

        CompanySettingsServiceImpl service = new CompanySettingsServiceImpl(
                mockRepository, mockLogRepo, mockScheduleRepo, objectMapper,
                legalBreakRequirements, legalOvertimeRequirements, mockCacheProvider);

        // Act & Assert
        assertThrows(ConflictException.class, () -> service.initializeDefaultSettings(companyId),
                "Should throw ConflictException when settings already exist");

        // Verify save was never called
        verify(mockRepository, never()).save(any());
    }

    // === Helper methods ===

    private void verifyAttendanceConfigDefaults(String json) {
        try {
            AttendanceConfig config = objectMapper.readValue(json, AttendanceConfig.class);

            assertNotNull(config.getDefaultWorkStartTime(), "defaultWorkStartTime should not be null");
            assertNotNull(config.getDefaultWorkEndTime(), "defaultWorkEndTime should not be null");
            assertNotNull(config.getDefaultBreakMinutes(), "defaultBreakMinutes should not be null");
            assertNotNull(config.getEnableRounding(), "enableRounding should not be null");
            assertNotNull(config.getLateGraceMinutes(), "lateGraceMinutes should not be null");
            assertNotNull(config.getEarlyLeaveGraceMinutes(), "earlyLeaveGraceMinutes should not be null");
            assertNotNull(config.getRequireDeviceRegistration(), "requireDeviceRegistration should not be null");
            assertNotNull(config.getRequireGeoLocation(), "requireGeoLocation should not be null");
            assertNotNull(config.getGeoFenceRadiusMeters(), "geoFenceRadiusMeters should not be null");
            assertNotNull(config.getAllowMobileCheckIn(), "allowMobileCheckIn should not be null");
            assertNotNull(config.getAllowWebCheckIn(), "allowWebCheckIn should not be null");

            // Verify default values
            assertEquals(LocalTime.of(9, 0), config.getDefaultWorkStartTime());
            assertEquals(LocalTime.of(18, 0), config.getDefaultWorkEndTime());
            assertEquals(60, config.getDefaultBreakMinutes());
            assertFalse(config.getEnableRounding());
            assertEquals(0, config.getLateGraceMinutes());
            assertEquals(0, config.getEarlyLeaveGraceMinutes());
            assertFalse(config.getRequireDeviceRegistration());
            assertFalse(config.getRequireGeoLocation());
            assertEquals(100, config.getGeoFenceRadiusMeters());
            assertTrue(config.getAllowMobileCheckIn());
            assertTrue(config.getAllowWebCheckIn());

        } catch (Exception e) {
            fail("Failed to deserialize AttendanceConfig: " + e.getMessage());
        }
    }

    private void verifyPayrollConfigDefaults(String json) {
        try {
            PayrollConfig config = objectMapper.readValue(json, PayrollConfig.class);

            assertNotNull(config.getDefaultSalaryType(), "defaultSalaryType should not be null");
            assertNotNull(config.getPayDay(), "payDay should not be null");
            assertNotNull(config.getCutoffDay(), "cutoffDay should not be null");
            assertNotNull(config.getSalaryRounding(), "salaryRounding should not be null");
            assertNotNull(config.getStandardWorkingDaysPerMonth(), "standardWorkingDaysPerMonth should not be null");
            assertNotNull(config.getStandardWorkingHoursPerDay(), "standardWorkingHoursPerDay should not be null");

            // Verify default values
            assertEquals(25, config.getPayDay());
            assertEquals(20, config.getCutoffDay());
            assertEquals(22, config.getStandardWorkingDaysPerMonth());
            assertEquals(8, config.getStandardWorkingHoursPerDay());

        } catch (Exception e) {
            fail("Failed to deserialize PayrollConfig: " + e.getMessage());
        }
    }

    private void verifyOvertimeConfigDefaults(String json) {
        try {
            OvertimeConfig config = objectMapper.readValue(json, OvertimeConfig.class);

            assertNotNull(config.getOvertimeEnabled(), "overtimeEnabled should not be null");
            assertNotNull(config.getRequireApproval(), "requireApproval should not be null");
            assertNotNull(config.getRegularOvertimeRate(), "regularOvertimeRate should not be null");
            assertNotNull(config.getNightOvertimeRate(), "nightOvertimeRate should not be null");
            assertNotNull(config.getHolidayOvertimeRate(), "holidayOvertimeRate should not be null");
            assertNotNull(config.getWeekendOvertimeRate(), "weekendOvertimeRate should not be null");
            assertNotNull(config.getNightStartTime(), "nightStartTime should not be null");
            assertNotNull(config.getNightEndTime(), "nightEndTime should not be null");
            assertNotNull(config.getMaxOvertimeHoursPerDay(), "maxOvertimeHoursPerDay should not be null");
            assertNotNull(config.getMaxOvertimeHoursPerMonth(), "maxOvertimeHoursPerMonth should not be null");

            // Verify default values
            assertTrue(config.getOvertimeEnabled());
            assertFalse(config.getRequireApproval());
            assertEquals(0, new BigDecimal("1.25").compareTo(config.getRegularOvertimeRate()));
            assertEquals(0, new BigDecimal("1.50").compareTo(config.getNightOvertimeRate()));
            assertEquals(0, new BigDecimal("1.35").compareTo(config.getHolidayOvertimeRate()));
            assertEquals(0, new BigDecimal("1.35").compareTo(config.getWeekendOvertimeRate()));
            assertEquals(LocalTime.of(22, 0), config.getNightStartTime());
            assertEquals(LocalTime.of(5, 0), config.getNightEndTime());
            assertEquals(4, config.getMaxOvertimeHoursPerDay());
            assertEquals(45, config.getMaxOvertimeHoursPerMonth());

        } catch (Exception e) {
            fail("Failed to deserialize OvertimeConfig: " + e.getMessage());
        }
    }

    private void verifyAllowanceConfigDefaults(String json) {
        try {
            AllowanceConfig config = objectMapper.readValue(json, AllowanceConfig.class);

            assertNotNull(config.getAllowances(), "allowances should not be null");
            assertTrue(config.getAllowances().isEmpty(), "allowances should be empty by default");

        } catch (Exception e) {
            fail("Failed to deserialize AllowanceConfig: " + e.getMessage());
        }
    }

    private void verifyDeductionConfigDefaults(String json) {
        try {
            DeductionConfig config = objectMapper.readValue(json, DeductionConfig.class);

            assertNotNull(config.getDeductions(), "deductions should not be null");
            assertNotNull(config.getEnableLatePenalty(), "enableLatePenalty should not be null");
            assertNotNull(config.getLatePenaltyPerMinute(), "latePenaltyPerMinute should not be null");
            assertNotNull(config.getEnableEarlyLeavePenalty(), "enableEarlyLeavePenalty should not be null");
            assertNotNull(config.getEarlyLeavePenaltyPerMinute(), "earlyLeavePenaltyPerMinute should not be null");
            assertNotNull(config.getEnableAbsenceDeduction(), "enableAbsenceDeduction should not be null");

            // Verify default values
            assertTrue(config.getDeductions().isEmpty());
            assertFalse(config.getEnableLatePenalty());
            assertEquals(0, BigDecimal.ZERO.compareTo(config.getLatePenaltyPerMinute()));
            assertFalse(config.getEnableEarlyLeavePenalty());
            assertEquals(0, BigDecimal.ZERO.compareTo(config.getEarlyLeavePenaltyPerMinute()));
            assertTrue(config.getEnableAbsenceDeduction());

        } catch (Exception e) {
            fail("Failed to deserialize DeductionConfig: " + e.getMessage());
        }
    }

    // === Generators ===

    @Provide
    Arbitrary<Long> companyIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }
}
