package com.tamabee.api_hr.service.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tamabee.api_hr.dto.request.WorkModeConfigRequest;
import com.tamabee.api_hr.dto.response.WorkModeConfigResponse;
import com.tamabee.api_hr.entity.audit.WorkModeChangeLogEntity;
import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import com.tamabee.api_hr.enums.WorkMode;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Work Mode feature
 * Feature: work-schedule-redesign
 */
public class WorkModeServicePropertyTest {

    private final ObjectMapper objectMapper;
    private final LegalBreakRequirements legalBreakRequirements;
    private final LegalOvertimeRequirements legalOvertimeRequirements;

    public WorkModeServicePropertyTest() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.legalBreakRequirements = new LegalBreakRequirements();
        this.legalOvertimeRequirements = new LegalOvertimeRequirements();
    }

    /**
     * Property 1: Work Mode Persistence Round-Trip
     * For any valid work mode configuration, saving to the API and then fetching
     * should return an equivalent configuration with the same mode and default
     * hours.
     */
    @Property(tries = 100)
    void workModePersistenceRoundTrip(
            @ForAll("companyIds") Long companyId,
            @ForAll("workModes") WorkMode workMode,
            @ForAll("workStartTimes") LocalTime startTime,
            @ForAll("workEndTimes") LocalTime endTime,
            @ForAll("breakMinutes") Integer breakMinutes) {

        // Skip invalid combinations (start time must be before end time)
        if (!startTime.isBefore(endTime)) {
            return;
        }

        // Arrange
        CompanySettingsRepository mockSettingsRepo = mock(CompanySettingsRepository.class);
        WorkModeChangeLogRepository mockLogRepo = mock(WorkModeChangeLogRepository.class);
        WorkScheduleRepository mockScheduleRepo = mock(WorkScheduleRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<CompanySettingsCache> mockCacheProvider = mock(ObjectProvider.class);
        when(mockCacheProvider.getIfAvailable()).thenReturn(null);

        // Tạo entity với work mode ban đầu khác
        CompanySettingEntity entity = new CompanySettingEntity();
        entity.setId(1L);
        entity.setCompanyId(companyId);
        entity.setWorkMode(workMode == WorkMode.FIXED_HOURS ? WorkMode.FLEXIBLE_SHIFT : WorkMode.FIXED_HOURS);

        when(mockSettingsRepo.findByCompanyIdAndDeletedFalse(companyId)).thenReturn(Optional.of(entity));
        when(mockSettingsRepo.save(any(CompanySettingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mockLogRepo.findByCompanyIdAndDeletedFalseOrderByChangedAtDesc(companyId))
                .thenReturn(Collections.emptyList());
        when(mockLogRepo.save(any(WorkModeChangeLogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock empty schedules
        Page<com.tamabee.api_hr.entity.attendance.WorkScheduleEntity> emptyPage = new PageImpl<>(
                Collections.emptyList());
        when(mockScheduleRepo.findByCompanyIdAndDeletedFalse(eq(companyId), any(Pageable.class)))
                .thenReturn(emptyPage);

        CompanySettingsServiceImpl service = new CompanySettingsServiceImpl(
                mockSettingsRepo, mockLogRepo, mockScheduleRepo, objectMapper,
                legalBreakRequirements, legalOvertimeRequirements, mockCacheProvider);

        // Act - Update work mode
        WorkModeConfigRequest request = WorkModeConfigRequest.builder()
                .mode(workMode)
                .defaultWorkStartTime(workMode == WorkMode.FIXED_HOURS ? startTime : null)
                .defaultWorkEndTime(workMode == WorkMode.FIXED_HOURS ? endTime : null)
                .defaultBreakMinutes(workMode == WorkMode.FIXED_HOURS ? breakMinutes : null)
                .reason("Test update")
                .build();

        WorkModeConfigResponse updateResponse = service.updateWorkModeConfig(companyId, request, "test@example.com");

        // Assert - Verify saved entity
        ArgumentCaptor<CompanySettingEntity> captor = ArgumentCaptor.forClass(CompanySettingEntity.class);
        verify(mockSettingsRepo).save(captor.capture());
        CompanySettingEntity savedEntity = captor.getValue();

        // Verify round-trip: saved mode equals requested mode
        assertEquals(workMode, savedEntity.getWorkMode(), "Work mode should be persisted correctly");
        assertEquals(workMode, updateResponse.getMode(), "Response mode should match request");

        // Verify FIXED_HOURS specific fields
        if (workMode == WorkMode.FIXED_HOURS) {
            assertEquals(startTime, savedEntity.getDefaultWorkStartTime(),
                    "Default work start time should be persisted");
            assertEquals(endTime, savedEntity.getDefaultWorkEndTime(),
                    "Default work end time should be persisted");
            assertEquals(breakMinutes, savedEntity.getDefaultBreakMinutes(),
                    "Default break minutes should be persisted");
        }
    }

    /**
     * Property bổ sung: Work mode change creates audit log
     * For any work mode change, an audit log entry should be created.
     */
    @Property(tries = 100)
    void workModeChangeCreatesAuditLog(
            @ForAll("companyIds") Long companyId,
            @ForAll("workModes") WorkMode newMode) {

        // Arrange
        CompanySettingsRepository mockSettingsRepo = mock(CompanySettingsRepository.class);
        WorkModeChangeLogRepository mockLogRepo = mock(WorkModeChangeLogRepository.class);
        WorkScheduleRepository mockScheduleRepo = mock(WorkScheduleRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<CompanySettingsCache> mockCacheProvider = mock(ObjectProvider.class);
        when(mockCacheProvider.getIfAvailable()).thenReturn(null);

        // Tạo entity với work mode khác với newMode
        WorkMode previousMode = newMode == WorkMode.FIXED_HOURS ? WorkMode.FLEXIBLE_SHIFT : WorkMode.FIXED_HOURS;
        CompanySettingEntity entity = new CompanySettingEntity();
        entity.setId(1L);
        entity.setCompanyId(companyId);
        entity.setWorkMode(previousMode);

        when(mockSettingsRepo.findByCompanyIdAndDeletedFalse(companyId)).thenReturn(Optional.of(entity));
        when(mockSettingsRepo.save(any(CompanySettingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mockLogRepo.findByCompanyIdAndDeletedFalseOrderByChangedAtDesc(companyId))
                .thenReturn(Collections.emptyList());
        when(mockLogRepo.save(any(WorkModeChangeLogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock empty schedules
        Page<com.tamabee.api_hr.entity.attendance.WorkScheduleEntity> emptyPage = new PageImpl<>(
                Collections.emptyList());
        when(mockScheduleRepo.findByCompanyIdAndDeletedFalse(eq(companyId), any(Pageable.class)))
                .thenReturn(emptyPage);

        CompanySettingsServiceImpl service = new CompanySettingsServiceImpl(
                mockSettingsRepo, mockLogRepo, mockScheduleRepo, objectMapper,
                legalBreakRequirements, legalOvertimeRequirements, mockCacheProvider);

        // Act
        WorkModeConfigRequest request = WorkModeConfigRequest.builder()
                .mode(newMode)
                .defaultWorkStartTime(newMode == WorkMode.FIXED_HOURS ? LocalTime.of(9, 0) : null)
                .defaultWorkEndTime(newMode == WorkMode.FIXED_HOURS ? LocalTime.of(18, 0) : null)
                .defaultBreakMinutes(newMode == WorkMode.FIXED_HOURS ? 60 : null)
                .reason("Test change")
                .build();

        service.updateWorkModeConfig(companyId, request, "test@example.com");

        // Assert - Verify audit log was created
        ArgumentCaptor<WorkModeChangeLogEntity> logCaptor = ArgumentCaptor.forClass(WorkModeChangeLogEntity.class);
        verify(mockLogRepo).save(logCaptor.capture());
        WorkModeChangeLogEntity savedLog = logCaptor.getValue();

        assertEquals(companyId, savedLog.getCompanyId(), "Log should have correct company ID");
        assertEquals(previousMode, savedLog.getPreviousMode(), "Log should have correct previous mode");
        assertEquals(newMode, savedLog.getNewMode(), "Log should have correct new mode");
        assertEquals("test@example.com", savedLog.getChangedBy(), "Log should have correct changed by");
        assertNotNull(savedLog.getChangedAt(), "Log should have changed at timestamp");
    }

    /**
     * Property 9: Mode Switch Preserves Schedules as Inactive
     * Khi switch từ FLEXIBLE_SHIFT sang FIXED_HOURS, tất cả schedules phải được
     * đánh dấu là inactive (isActive = false).
     */
    @Property(tries = 100)
    void modeSwitchPreservesSchedulesAsInactive(
            @ForAll("companyIds") Long companyId,
            @ForAll("scheduleCounts") int scheduleCount) {

        // Arrange
        CompanySettingsRepository mockSettingsRepo = mock(CompanySettingsRepository.class);
        WorkModeChangeLogRepository mockLogRepo = mock(WorkModeChangeLogRepository.class);
        WorkScheduleRepository mockScheduleRepo = mock(WorkScheduleRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<CompanySettingsCache> mockCacheProvider = mock(ObjectProvider.class);
        when(mockCacheProvider.getIfAvailable()).thenReturn(null);

        // Tạo entity với FLEXIBLE_SHIFT mode (mode ban đầu)
        CompanySettingEntity entity = new CompanySettingEntity();
        entity.setId(1L);
        entity.setCompanyId(companyId);
        entity.setWorkMode(WorkMode.FLEXIBLE_SHIFT);

        // Tạo danh sách schedules với isActive = true
        List<com.tamabee.api_hr.entity.attendance.WorkScheduleEntity> schedules = new java.util.ArrayList<>();
        for (int i = 0; i < scheduleCount; i++) {
            com.tamabee.api_hr.entity.attendance.WorkScheduleEntity schedule = new com.tamabee.api_hr.entity.attendance.WorkScheduleEntity();
            schedule.setId((long) (i + 1));
            schedule.setCompanyId(companyId);
            schedule.setName("Schedule " + (i + 1));
            schedule.setIsActive(true);
            schedules.add(schedule);
        }

        when(mockSettingsRepo.findByCompanyIdAndDeletedFalse(companyId)).thenReturn(Optional.of(entity));
        when(mockSettingsRepo.save(any(CompanySettingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mockLogRepo.findByCompanyIdAndDeletedFalseOrderByChangedAtDesc(companyId))
                .thenReturn(Collections.emptyList());
        when(mockLogRepo.save(any(WorkModeChangeLogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock schedules page
        Page<com.tamabee.api_hr.entity.attendance.WorkScheduleEntity> schedulesPage = new PageImpl<>(schedules);
        when(mockScheduleRepo.findByCompanyIdAndDeletedFalse(eq(companyId), any(Pageable.class)))
                .thenReturn(schedulesPage);
        when(mockScheduleRepo.save(any(com.tamabee.api_hr.entity.attendance.WorkScheduleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompanySettingsServiceImpl service = new CompanySettingsServiceImpl(
                mockSettingsRepo, mockLogRepo, mockScheduleRepo, objectMapper,
                legalBreakRequirements, legalOvertimeRequirements, mockCacheProvider);

        // Act - Switch từ FLEXIBLE_SHIFT sang FIXED_HOURS
        WorkModeConfigRequest request = WorkModeConfigRequest.builder()
                .mode(WorkMode.FIXED_HOURS)
                .defaultWorkStartTime(LocalTime.of(9, 0))
                .defaultWorkEndTime(LocalTime.of(18, 0))
                .defaultBreakMinutes(60)
                .reason("Switch to fixed hours")
                .build();

        service.updateWorkModeConfig(companyId, request, "test@example.com");

        // Assert - Verify tất cả schedules được save với isActive = false
        if (scheduleCount > 0) {
            ArgumentCaptor<com.tamabee.api_hr.entity.attendance.WorkScheduleEntity> scheduleCaptor = ArgumentCaptor
                    .forClass(com.tamabee.api_hr.entity.attendance.WorkScheduleEntity.class);
            verify(mockScheduleRepo, times(scheduleCount)).save(scheduleCaptor.capture());

            List<com.tamabee.api_hr.entity.attendance.WorkScheduleEntity> savedSchedules = scheduleCaptor
                    .getAllValues();
            for (com.tamabee.api_hr.entity.attendance.WorkScheduleEntity savedSchedule : savedSchedules) {
                assertFalse(savedSchedule.getIsActive(),
                        "Schedule " + savedSchedule.getName() + " should be marked as inactive");
            }
        }
    }

    // === Generators ===

    @Provide
    Arbitrary<Long> companyIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    @Provide
    Arbitrary<Integer> scheduleCounts() {
        return Arbitraries.integers().between(0, 10);
    }

    @Provide
    Arbitrary<WorkMode> workModes() {
        return Arbitraries.of(WorkMode.FIXED_HOURS, WorkMode.FLEXIBLE_SHIFT);
    }

    @Provide
    Arbitrary<LocalTime> workStartTimes() {
        return Arbitraries.integers().between(6, 12)
                .map(hour -> LocalTime.of(hour, 0));
    }

    @Provide
    Arbitrary<LocalTime> workEndTimes() {
        return Arbitraries.integers().between(15, 22)
                .map(hour -> LocalTime.of(hour, 0));
    }

    @Provide
    Arbitrary<Integer> breakMinutes() {
        return Arbitraries.integers().between(30, 120);
    }
}
