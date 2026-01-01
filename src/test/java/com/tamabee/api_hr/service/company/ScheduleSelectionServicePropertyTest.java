package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.response.WorkScheduleResponse;
import com.tamabee.api_hr.entity.attendance.ScheduleSelectionEntity;
import com.tamabee.api_hr.entity.attendance.WorkScheduleEntity;
import com.tamabee.api_hr.enums.ScheduleType;
import com.tamabee.api_hr.enums.SelectionStatus;
import com.tamabee.api_hr.mapper.company.ScheduleSelectionMapper;
import com.tamabee.api_hr.mapper.company.WorkScheduleMapper;
import com.tamabee.api_hr.repository.ScheduleSelectionRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WorkScheduleAssignmentRepository;
import com.tamabee.api_hr.repository.WorkScheduleRepository;
import com.tamabee.api_hr.service.company.impl.ScheduleSelectionServiceImpl;
import net.jqwik.api.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests cho ScheduleSelectionService
 * Feature: attendance-payroll-backend
 */
public class ScheduleSelectionServicePropertyTest {

    /**
     * Property 25: Schedule Suggestion Relevance
     * For any schedule suggestion for an employee, the suggested schedules SHALL
     * include schedules from the employee's past selections or company-recommended
     * schedules.
     */
    @Property(tries = 100)
    void scheduleSuggestionRelevance_includesPastSelectionsOrCompanyRecommendations(
            @ForAll("employeeIds") Long employeeId,
            @ForAll("companyIds") Long companyId,
            @ForAll("pastApprovedScheduleIds") List<Long> pastApprovedIds,
            @ForAll("defaultScheduleIds") Long defaultScheduleId) {

        // Setup mocks
        ScheduleSelectionRepository selectionRepo = mock(ScheduleSelectionRepository.class);
        WorkScheduleRepository scheduleRepo = mock(WorkScheduleRepository.class);
        WorkScheduleAssignmentRepository assignmentRepo = mock(WorkScheduleAssignmentRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ScheduleSelectionMapper selectionMapper = mock(ScheduleSelectionMapper.class);
        WorkScheduleMapper scheduleMapper = mock(WorkScheduleMapper.class);

        // Mock các lịch đã được nhân viên chọn và duyệt trước đó
        when(selectionRepo.findApprovedScheduleIdsByEmployeeId(employeeId))
                .thenReturn(pastApprovedIds);

        // Mock lịch mặc định của công ty
        WorkScheduleEntity defaultSchedule = createScheduleEntity(defaultScheduleId, companyId, "Default Schedule",
                true);
        when(scheduleRepo.findDefaultByCompanyId(companyId))
                .thenReturn(Optional.of(defaultSchedule));

        // Mock các schedule entities cho past approved
        for (Long scheduleId : pastApprovedIds) {
            WorkScheduleEntity schedule = createScheduleEntity(scheduleId, companyId, "Schedule " + scheduleId, false);
            when(scheduleRepo.findByIdAndDeletedFalse(scheduleId))
                    .thenReturn(Optional.of(schedule));
            when(scheduleMapper.toResponse(schedule))
                    .thenReturn(createScheduleResponse(scheduleId, companyId, "Schedule " + scheduleId, false));
        }

        // Mock default schedule response
        when(scheduleRepo.findByIdAndDeletedFalse(defaultScheduleId))
                .thenReturn(Optional.of(defaultSchedule));
        when(scheduleMapper.toResponse(defaultSchedule))
                .thenReturn(createScheduleResponse(defaultScheduleId, companyId, "Default Schedule", true));

        // Mock empty page for fallback case
        when(scheduleRepo.findByCompanyIdAndDeletedFalse(eq(companyId), any()))
                .thenReturn(Page.empty());

        ScheduleSelectionServiceImpl service = new ScheduleSelectionServiceImpl(
                selectionRepo, scheduleRepo, assignmentRepo, userRepo, selectionMapper, scheduleMapper);

        // Gọi getSuggestedSchedules
        List<WorkScheduleResponse> suggestions = service.getSuggestedSchedules(employeeId, companyId);

        // Kiểm tra kết quả
        assertNotNull(suggestions, "Suggestions should not be null");

        // Lấy tất cả IDs từ suggestions
        Set<Long> suggestedIds = suggestions.stream()
                .map(WorkScheduleResponse::getId)
                .collect(Collectors.toSet());

        // Tập hợp các ID hợp lệ (past approved + default)
        Set<Long> validIds = new java.util.HashSet<>(pastApprovedIds);
        validIds.add(defaultScheduleId);

        // Mỗi suggestion phải thuộc về past approved hoặc default schedule
        for (Long suggestedId : suggestedIds) {
            assertTrue(validIds.contains(suggestedId),
                    "Suggested schedule " + suggestedId + " should be from past selections or company recommendations");
        }

        // Nếu có past approved hoặc default, suggestions không được rỗng
        if (!pastApprovedIds.isEmpty() || defaultScheduleId != null) {
            assertFalse(suggestions.isEmpty(),
                    "Suggestions should not be empty when there are past selections or default schedule");
        }
    }

    /**
     * Property 25 (case 2): When employee has past approved selections,
     * those schedules should be included in suggestions.
     */
    @Property(tries = 100)
    void scheduleSuggestionRelevance_includesPastApprovedSelections(
            @ForAll("employeeIds") Long employeeId,
            @ForAll("companyIds") Long companyId,
            @ForAll("nonEmptyPastApprovedScheduleIds") List<Long> pastApprovedIds) {

        // Setup mocks
        ScheduleSelectionRepository selectionRepo = mock(ScheduleSelectionRepository.class);
        WorkScheduleRepository scheduleRepo = mock(WorkScheduleRepository.class);
        WorkScheduleAssignmentRepository assignmentRepo = mock(WorkScheduleAssignmentRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ScheduleSelectionMapper selectionMapper = mock(ScheduleSelectionMapper.class);
        WorkScheduleMapper scheduleMapper = mock(WorkScheduleMapper.class);

        // Mock các lịch đã được nhân viên chọn và duyệt trước đó
        when(selectionRepo.findApprovedScheduleIdsByEmployeeId(employeeId))
                .thenReturn(pastApprovedIds);

        // Không có default schedule
        when(scheduleRepo.findDefaultByCompanyId(companyId))
                .thenReturn(Optional.empty());

        // Mock các schedule entities
        for (Long scheduleId : pastApprovedIds) {
            WorkScheduleEntity schedule = createScheduleEntity(scheduleId, companyId, "Schedule " + scheduleId, false);
            when(scheduleRepo.findByIdAndDeletedFalse(scheduleId))
                    .thenReturn(Optional.of(schedule));
            when(scheduleMapper.toResponse(schedule))
                    .thenReturn(createScheduleResponse(scheduleId, companyId, "Schedule " + scheduleId, false));
        }

        // Mock empty page for fallback
        when(scheduleRepo.findByCompanyIdAndDeletedFalse(eq(companyId), any()))
                .thenReturn(Page.empty());

        ScheduleSelectionServiceImpl service = new ScheduleSelectionServiceImpl(
                selectionRepo, scheduleRepo, assignmentRepo, userRepo, selectionMapper, scheduleMapper);

        // Gọi getSuggestedSchedules
        List<WorkScheduleResponse> suggestions = service.getSuggestedSchedules(employeeId, companyId);

        // Kiểm tra tất cả past approved schedules đều có trong suggestions
        Set<Long> suggestedIds = suggestions.stream()
                .map(WorkScheduleResponse::getId)
                .collect(Collectors.toSet());

        for (Long pastId : pastApprovedIds) {
            assertTrue(suggestedIds.contains(pastId),
                    "Past approved schedule " + pastId + " should be included in suggestions");
        }
    }

    /**
     * Property 25 (case 3): When company has default schedule,
     * it should be included in suggestions.
     */
    @Property(tries = 100)
    void scheduleSuggestionRelevance_includesCompanyDefaultSchedule(
            @ForAll("employeeIds") Long employeeId,
            @ForAll("companyIds") Long companyId,
            @ForAll("defaultScheduleIds") Long defaultScheduleId) {

        // Setup mocks
        ScheduleSelectionRepository selectionRepo = mock(ScheduleSelectionRepository.class);
        WorkScheduleRepository scheduleRepo = mock(WorkScheduleRepository.class);
        WorkScheduleAssignmentRepository assignmentRepo = mock(WorkScheduleAssignmentRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ScheduleSelectionMapper selectionMapper = mock(ScheduleSelectionMapper.class);
        WorkScheduleMapper scheduleMapper = mock(WorkScheduleMapper.class);

        // Không có past approved selections
        when(selectionRepo.findApprovedScheduleIdsByEmployeeId(employeeId))
                .thenReturn(new ArrayList<>());

        // Có default schedule
        WorkScheduleEntity defaultSchedule = createScheduleEntity(defaultScheduleId, companyId, "Default Schedule",
                true);
        when(scheduleRepo.findDefaultByCompanyId(companyId))
                .thenReturn(Optional.of(defaultSchedule));
        when(scheduleRepo.findByIdAndDeletedFalse(defaultScheduleId))
                .thenReturn(Optional.of(defaultSchedule));
        when(scheduleMapper.toResponse(defaultSchedule))
                .thenReturn(createScheduleResponse(defaultScheduleId, companyId, "Default Schedule", true));

        // Mock empty page for fallback
        when(scheduleRepo.findByCompanyIdAndDeletedFalse(eq(companyId), any()))
                .thenReturn(Page.empty());

        ScheduleSelectionServiceImpl service = new ScheduleSelectionServiceImpl(
                selectionRepo, scheduleRepo, assignmentRepo, userRepo, selectionMapper, scheduleMapper);

        // Gọi getSuggestedSchedules
        List<WorkScheduleResponse> suggestions = service.getSuggestedSchedules(employeeId, companyId);

        // Kiểm tra default schedule có trong suggestions
        Set<Long> suggestedIds = suggestions.stream()
                .map(WorkScheduleResponse::getId)
                .collect(Collectors.toSet());

        assertTrue(suggestedIds.contains(defaultScheduleId),
                "Company default schedule should be included in suggestions");
    }

    /**
     * Property 25 (case 4): When no past selections and no default,
     * fallback to all company schedules.
     */
    @Property(tries = 100)
    void scheduleSuggestionRelevance_fallbackToAllCompanySchedules(
            @ForAll("employeeIds") Long employeeId,
            @ForAll("companyIds") Long companyId,
            @ForAll("companyScheduleList") List<WorkScheduleEntity> companySchedules) {

        // Setup mocks
        ScheduleSelectionRepository selectionRepo = mock(ScheduleSelectionRepository.class);
        WorkScheduleRepository scheduleRepo = mock(WorkScheduleRepository.class);
        WorkScheduleAssignmentRepository assignmentRepo = mock(WorkScheduleAssignmentRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ScheduleSelectionMapper selectionMapper = mock(ScheduleSelectionMapper.class);
        WorkScheduleMapper scheduleMapper = mock(WorkScheduleMapper.class);

        // Không có past approved selections
        when(selectionRepo.findApprovedScheduleIdsByEmployeeId(employeeId))
                .thenReturn(new ArrayList<>());

        // Không có default schedule
        when(scheduleRepo.findDefaultByCompanyId(companyId))
                .thenReturn(Optional.empty());

        // Set companyId cho tất cả schedules
        for (WorkScheduleEntity schedule : companySchedules) {
            schedule.setCompanyId(companyId);
            when(scheduleRepo.findByIdAndDeletedFalse(schedule.getId()))
                    .thenReturn(Optional.of(schedule));
            when(scheduleMapper.toResponse(schedule))
                    .thenReturn(createScheduleResponse(schedule.getId(), companyId, schedule.getName(), false));
        }

        // Mock page of all company schedules
        Page<WorkScheduleEntity> schedulePage = new PageImpl<>(companySchedules);
        when(scheduleRepo.findByCompanyIdAndDeletedFalse(eq(companyId), any()))
                .thenReturn(schedulePage);

        ScheduleSelectionServiceImpl service = new ScheduleSelectionServiceImpl(
                selectionRepo, scheduleRepo, assignmentRepo, userRepo, selectionMapper, scheduleMapper);

        // Gọi getSuggestedSchedules
        List<WorkScheduleResponse> suggestions = service.getSuggestedSchedules(employeeId, companyId);

        // Khi không có past selections và không có default, phải fallback về tất cả
        // schedules
        if (!companySchedules.isEmpty()) {
            assertFalse(suggestions.isEmpty(),
                    "Should fallback to company schedules when no past selections and no default");

            Set<Long> companyScheduleIds = companySchedules.stream()
                    .map(WorkScheduleEntity::getId)
                    .collect(Collectors.toSet());

            Set<Long> suggestedIds = suggestions.stream()
                    .map(WorkScheduleResponse::getId)
                    .collect(Collectors.toSet());

            // Tất cả suggestions phải thuộc về company schedules
            for (Long suggestedId : suggestedIds) {
                assertTrue(companyScheduleIds.contains(suggestedId),
                        "Suggested schedule should be from company schedules");
            }
        }
    }

    // === Helper Methods ===

    private WorkScheduleEntity createScheduleEntity(Long id, Long companyId, String name, boolean isDefault) {
        WorkScheduleEntity entity = new WorkScheduleEntity();
        entity.setId(id);
        entity.setCompanyId(companyId);
        entity.setName(name);
        entity.setType(ScheduleType.FIXED);
        entity.setIsDefault(isDefault);
        return entity;
    }

    private WorkScheduleResponse createScheduleResponse(Long id, Long companyId, String name, boolean isDefault) {
        return WorkScheduleResponse.builder()
                .id(id)
                .companyId(companyId)
                .name(name)
                .type(ScheduleType.FIXED)
                .isDefault(isDefault)
                .build();
    }

    // === Generators ===

    @Provide
    Arbitrary<Long> employeeIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<Long> companyIds() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    @Provide
    Arbitrary<Long> defaultScheduleIds() {
        return Arbitraries.longs().between(1L, 100L);
    }

    @Provide
    Arbitrary<List<Long>> pastApprovedScheduleIds() {
        return Arbitraries.longs().between(101L, 200L)
                .list()
                .ofMinSize(0)
                .ofMaxSize(5)
                .uniqueElements();
    }

    @Provide
    Arbitrary<List<Long>> nonEmptyPastApprovedScheduleIds() {
        return Arbitraries.longs().between(101L, 200L)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5)
                .uniqueElements();
    }

    @Provide
    Arbitrary<List<WorkScheduleEntity>> companyScheduleList() {
        return Arbitraries.integers().between(1, 5)
                .flatMap(count -> {
                    List<Arbitrary<WorkScheduleEntity>> arbitraries = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        final int index = i;
                        arbitraries.add(Arbitraries.longs().between(201L + index * 10, 210L + index * 10)
                                .map(id -> {
                                    WorkScheduleEntity entity = new WorkScheduleEntity();
                                    entity.setId(id);
                                    entity.setName("Company Schedule " + id);
                                    entity.setType(ScheduleType.FIXED);
                                    entity.setIsDefault(false);
                                    return entity;
                                }));
                    }
                    return Combinators.combine(arbitraries).as(list -> list);
                });
    }
}
