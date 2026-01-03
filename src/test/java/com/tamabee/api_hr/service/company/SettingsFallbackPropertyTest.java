package com.tamabee.api_hr.service.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.repository.CompanySettingsRepository;
import com.tamabee.api_hr.service.company.cache.CompanySettingsCache;
import com.tamabee.api_hr.service.company.cache.DefaultSettingsProvider;
import com.tamabee.api_hr.service.company.impl.CachedCompanySettingsServiceImpl;
import net.jqwik.api.*;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Settings Fallback
 * Feature: flexible-workforce-management, Property 18: Settings Fallback to
 * Defaults
 * 
 * For any company without specific settings configured, the system SHALL use
 * default values for all required settings without throwing errors.
 */
public class SettingsFallbackPropertyTest {

    private final ObjectMapper objectMapper;
    private final DefaultSettingsProvider defaultSettingsProvider;

    public SettingsFallbackPropertyTest() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.defaultSettingsProvider = new DefaultSettingsProvider();
    }

    /**
     * Property 18: Settings Fallback to Defaults
     * Khi công ty không có settings, hệ thống phải trả về default values mà không
     * throw error
     */
    @Property(tries = 100)
    void settingsFallbackToDefaultsWhenNotConfigured(@ForAll("companyIds") Long companyId) {
        // Arrange - Mock repository trả về empty (không có settings)
        CompanySettingsRepository mockRepository = mock(CompanySettingsRepository.class);
        when(mockRepository.findByCompanyIdAndDeletedFalse(companyId)).thenReturn(Optional.empty());

        @SuppressWarnings("unchecked")
        ObjectProvider<CompanySettingsCache> mockCacheProvider = mock(ObjectProvider.class);
        when(mockCacheProvider.getIfAvailable()).thenReturn(null);

        CachedCompanySettingsServiceImpl service = new CachedCompanySettingsServiceImpl(
                mockRepository, mockCacheProvider, defaultSettingsProvider, objectMapper);

        // Act & Assert - Không throw exception và trả về non-null configs
        assertDoesNotThrow(() -> {
            AttendanceConfig attendanceConfig = service.getAttendanceConfig(companyId);
            assertNotNull(attendanceConfig, "AttendanceConfig should not be null");
            assertNotNull(attendanceConfig.getDefaultWorkStartTime(), "defaultWorkStartTime should have default");
            assertNotNull(attendanceConfig.getDefaultWorkEndTime(), "defaultWorkEndTime should have default");
            assertNotNull(attendanceConfig.getEnableRounding(), "enableRounding should have default");
            assertNotNull(attendanceConfig.getRequireDeviceRegistration(),
                    "requireDeviceRegistration should have default");
            assertNotNull(attendanceConfig.getRequireGeoLocation(), "requireGeoLocation should have default");
        });

        assertDoesNotThrow(() -> {
            PayrollConfig payrollConfig = service.getPayrollConfig(companyId);
            assertNotNull(payrollConfig, "PayrollConfig should not be null");
            assertNotNull(payrollConfig.getDefaultSalaryType(), "defaultSalaryType should have default");
            assertNotNull(payrollConfig.getPayDay(), "payDay should have default");
            assertNotNull(payrollConfig.getCutoffDay(), "cutoffDay should have default");
            assertNotNull(payrollConfig.getStandardWorkingDaysPerMonth(),
                    "standardWorkingDaysPerMonth should have default");
            assertNotNull(payrollConfig.getStandardWorkingHoursPerDay(),
                    "standardWorkingHoursPerDay should have default");
        });

        assertDoesNotThrow(() -> {
            OvertimeConfig overtimeConfig = service.getOvertimeConfig(companyId);
            assertNotNull(overtimeConfig, "OvertimeConfig should not be null");
            assertNotNull(overtimeConfig.getOvertimeEnabled(), "overtimeEnabled should have default");
            assertNotNull(overtimeConfig.getRegularOvertimeRate(), "regularOvertimeRate should have default");
            assertNotNull(overtimeConfig.getNightOvertimeRate(), "nightOvertimeRate should have default");
            assertNotNull(overtimeConfig.getNightStartTime(), "nightStartTime should have default");
            assertNotNull(overtimeConfig.getNightEndTime(), "nightEndTime should have default");
        });

        assertDoesNotThrow(() -> {
            BreakConfig breakConfig = service.getBreakConfig(companyId);
            assertNotNull(breakConfig, "BreakConfig should not be null");
            assertNotNull(breakConfig.getBreakEnabled(), "breakEnabled should have default");
            assertNotNull(breakConfig.getBreakType(), "breakType should have default");
            assertNotNull(breakConfig.getDefaultBreakMinutes(), "defaultBreakMinutes should have default");
            assertNotNull(breakConfig.getMinimumBreakMinutes(), "minimumBreakMinutes should have default");
            assertNotNull(breakConfig.getMaximumBreakMinutes(), "maximumBreakMinutes should have default");
        });

        assertDoesNotThrow(() -> {
            AllowanceConfig allowanceConfig = service.getAllowanceConfig(companyId);
            assertNotNull(allowanceConfig, "AllowanceConfig should not be null");
        });

        assertDoesNotThrow(() -> {
            DeductionConfig deductionConfig = service.getDeductionConfig(companyId);
            assertNotNull(deductionConfig, "DeductionConfig should not be null");
            assertNotNull(deductionConfig.getEnableLatePenalty(), "enableLatePenalty should have default");
            assertNotNull(deductionConfig.getEnableEarlyLeavePenalty(), "enableEarlyLeavePenalty should have default");
        });
    }

    /**
     * Property 18 bổ sung: Khi settings có nhưng một số fields null,
     * hệ thống phải merge với default values
     */
    @Property(tries = 100)
    void settingsMergeWithDefaultsForNullFields(@ForAll("companyIds") Long companyId) {
        // Arrange - Tạo config với một số fields null
        AttendanceConfig partialConfig = new AttendanceConfig();
        partialConfig.setEnableRounding(true); // Chỉ set một field
        // Các fields khác là null

        // Act - Merge với defaults
        AttendanceConfig mergedConfig = defaultSettingsProvider.mergeWithDefaults(partialConfig, companyId);

        // Assert - Field đã set giữ nguyên, fields null được fill với defaults
        assertTrue(mergedConfig.getEnableRounding(), "enableRounding should keep original value");
        assertNotNull(mergedConfig.getDefaultWorkStartTime(), "defaultWorkStartTime should be filled with default");
        assertNotNull(mergedConfig.getDefaultWorkEndTime(), "defaultWorkEndTime should be filled with default");
        assertNotNull(mergedConfig.getLateGraceMinutes(), "lateGraceMinutes should be filled with default");
        assertNotNull(mergedConfig.getRequireDeviceRegistration(),
                "requireDeviceRegistration should be filled with default");
    }

    /**
     * Property 18 bổ sung: Default values phải hợp lệ và có ý nghĩa
     */
    @Property(tries = 100)
    void defaultValuesAreValid(@ForAll("companyIds") Long companyId) {
        // Act
        AttendanceConfig attendanceConfig = defaultSettingsProvider.getDefaultAttendanceConfig(companyId);
        PayrollConfig payrollConfig = defaultSettingsProvider.getDefaultPayrollConfig(companyId);
        OvertimeConfig overtimeConfig = defaultSettingsProvider.getDefaultOvertimeConfig(companyId);
        BreakConfig breakConfig = defaultSettingsProvider.getDefaultBreakConfig(companyId);

        // Assert - Attendance defaults
        assertTrue(attendanceConfig.getDefaultWorkStartTime().isBefore(attendanceConfig.getDefaultWorkEndTime()),
                "Work start time should be before end time");
        assertTrue(attendanceConfig.getDefaultBreakMinutes() > 0, "Default break minutes should be positive");
        assertTrue(attendanceConfig.getGeoFenceRadiusMeters() > 0, "Geo fence radius should be positive");

        // Assert - Payroll defaults
        assertTrue(payrollConfig.getPayDay() >= 1 && payrollConfig.getPayDay() <= 31,
                "Pay day should be valid day of month");
        assertTrue(payrollConfig.getCutoffDay() >= 1 && payrollConfig.getCutoffDay() <= 31,
                "Cutoff day should be valid day of month");
        assertTrue(payrollConfig.getStandardWorkingDaysPerMonth() > 0,
                "Standard working days should be positive");
        assertTrue(payrollConfig.getStandardWorkingHoursPerDay() > 0,
                "Standard working hours should be positive");

        // Assert - Overtime defaults
        assertTrue(overtimeConfig.getRegularOvertimeRate().doubleValue() >= 1.0,
                "Regular overtime rate should be at least 1.0");
        assertTrue(overtimeConfig.getNightOvertimeRate().doubleValue() >= 1.0,
                "Night overtime rate should be at least 1.0");
        assertTrue(overtimeConfig.getMaxOvertimeHoursPerDay() > 0,
                "Max overtime hours per day should be positive");
        assertTrue(overtimeConfig.getMaxOvertimeHoursPerMonth() > 0,
                "Max overtime hours per month should be positive");

        // Assert - Break defaults
        assertTrue(breakConfig.getDefaultBreakMinutes() > 0, "Default break minutes should be positive");
        assertTrue(breakConfig.getMinimumBreakMinutes() > 0, "Minimum break minutes should be positive");
        assertTrue(breakConfig.getMaximumBreakMinutes() >= breakConfig.getMinimumBreakMinutes(),
                "Maximum break should be >= minimum break");
        assertTrue(breakConfig.getDefaultBreakMinutes() >= breakConfig.getMinimumBreakMinutes() &&
                breakConfig.getDefaultBreakMinutes() <= breakConfig.getMaximumBreakMinutes(),
                "Default break should be within min-max range");
    }

    // === Generators ===

    @Provide
    Arbitrary<Long> companyIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }
}
