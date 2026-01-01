package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.dto.request.CreateWorkScheduleRequest;
import com.tamabee.api_hr.dto.response.WorkScheduleResponse;
import com.tamabee.api_hr.entity.attendance.WorkScheduleAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.WorkScheduleEntity;
import com.tamabee.api_hr.enums.ScheduleType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.mapper.company.WorkScheduleMapper;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WorkScheduleAssignmentRepository;
import com.tamabee.api_hr.repository.WorkScheduleRepository;
import com.tamabee.api_hr.service.company.impl.WorkScheduleServiceImpl;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho WorkScheduleService
 * Feature: attendance-payroll-backend
 */
public class WorkScheduleServicePropertyTest {

    /**
     * Property 3: Work Schedule Time Validity
     * For any valid work schedule, the start time SHALL always be before the end
     * time
     * for each working period.
     */
    @Property(tries = 100)
    void workScheduleTimeValidity_startTimeMustBeBeforeEndTime(
            @ForAll("invalidTimeSchedules") CreateWorkScheduleRequest request) {

        // Setup mocks
        WorkScheduleRepository scheduleRepo = mock(WorkScheduleRepository.class);
        WorkScheduleAssignmentRepository assignmentRepo = mock(WorkScheduleAssignmentRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        WorkScheduleMapper mapper = mock(WorkScheduleMapper.class);

        when(scheduleRepo.existsByCompanyIdAndNameAndDeletedFalse(any(), any())).thenReturn(false);

        WorkScheduleServiceImpl service = new WorkScheduleServiceImpl(
                scheduleRepo, assignmentRepo, userRepo, mapper);

        // Khi tạo schedule với start time >= end time, phải throw exception
        assertThrows(BadRequestException.class, () -> {
            service.createSchedule(1L, request);
        }, "Should throw BadRequestException when start time is not before end time");
    }

    /**
     * Property 3 (positive case): Valid schedules should be created successfully
     * For any schedule where start time < end time, creation should succeed.
     */
    @Property(tries = 100)
    void workScheduleTimeValidity_validScheduleShouldBeCreated(
            @ForAll("validTimeSchedules") CreateWorkScheduleRequest request) {

        // Setup mocks
        WorkScheduleRepository scheduleRepo = mock(WorkScheduleRepository.class);
        WorkScheduleAssignmentRepository assignmentRepo = mock(WorkScheduleAssignmentRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        WorkScheduleMapper mapper = mock(WorkScheduleMapper.class);

        when(scheduleRepo.existsByCompanyIdAndNameAndDeletedFalse(any(), any())).thenReturn(false);

        WorkScheduleEntity savedEntity = new WorkScheduleEntity();
        savedEntity.setId(1L);
        savedEntity.setCompanyId(1L);
        savedEntity.setName(request.getName());
        savedEntity.setType(request.getType());
        savedEntity.setIsDefault(false);

        when(mapper.toEntity(any(), any())).thenReturn(savedEntity);
        when(scheduleRepo.save(any())).thenReturn(savedEntity);
        when(mapper.toResponse(any())).thenReturn(WorkScheduleResponse.builder()
                .id(1L)
                .companyId(1L)
                .name(request.getName())
                .type(request.getType())
                .build());

        WorkScheduleServiceImpl service = new WorkScheduleServiceImpl(
                scheduleRepo, assignmentRepo, userRepo, mapper);

        // Không throw exception khi schedule hợp lệ
        WorkScheduleResponse response = service.createSchedule(1L, request);
        assertNotNull(response, "Valid schedule should be created successfully");
    }

    /**
     * Property 4: Employee Schedule Resolution
     * For any employee without an assigned schedule, the system SHALL return
     * the company's default schedule.
     */
    @Property(tries = 100)
    void employeeScheduleResolution_fallbackToDefaultSchedule(
            @ForAll("companyIds") Long companyId,
            @ForAll("employeeIds") Long employeeId,
            @ForAll("dates") LocalDate date) {

        // Setup mocks
        WorkScheduleRepository scheduleRepo = mock(WorkScheduleRepository.class);
        WorkScheduleAssignmentRepository assignmentRepo = mock(WorkScheduleAssignmentRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        WorkScheduleMapper mapper = mock(WorkScheduleMapper.class);

        // Nhân viên không có assignment
        when(assignmentRepo.findByEmployeeIdAndEffectiveDate(eq(employeeId), eq(date)))
                .thenReturn(Collections.emptyList());

        // Công ty có default schedule
        WorkScheduleEntity defaultSchedule = new WorkScheduleEntity();
        defaultSchedule.setId(100L);
        defaultSchedule.setCompanyId(companyId);
        defaultSchedule.setName("Default Schedule");
        defaultSchedule.setType(ScheduleType.FIXED);
        defaultSchedule.setIsDefault(true);

        when(scheduleRepo.findDefaultByCompanyId(companyId))
                .thenReturn(Optional.of(defaultSchedule));

        WorkScheduleResponse expectedResponse = WorkScheduleResponse.builder()
                .id(100L)
                .companyId(companyId)
                .name("Default Schedule")
                .type(ScheduleType.FIXED)
                .isDefault(true)
                .build();

        when(mapper.toResponse(defaultSchedule)).thenReturn(expectedResponse);

        WorkScheduleServiceImpl service = new WorkScheduleServiceImpl(
                scheduleRepo, assignmentRepo, userRepo, mapper);

        // Khi nhân viên không có assignment, phải trả về default schedule
        WorkScheduleResponse result = service.getEffectiveSchedule(employeeId, companyId, date);

        assertNotNull(result, "Should return default schedule when employee has no assignment");
        assertTrue(result.getIsDefault(), "Returned schedule should be the default schedule");
        assertEquals(companyId, result.getCompanyId(), "Schedule should belong to the same company");
    }

    /**
     * Property 4 (with assignment): Employee with assignment should get assigned
     * schedule
     */
    @Property(tries = 100)
    void employeeScheduleResolution_returnAssignedSchedule(
            @ForAll("companyIds") Long companyId,
            @ForAll("employeeIds") Long employeeId,
            @ForAll("dates") LocalDate date) {

        // Setup mocks
        WorkScheduleRepository scheduleRepo = mock(WorkScheduleRepository.class);
        WorkScheduleAssignmentRepository assignmentRepo = mock(WorkScheduleAssignmentRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        WorkScheduleMapper mapper = mock(WorkScheduleMapper.class);

        // Nhân viên có assignment
        Long assignedScheduleId = 50L;
        WorkScheduleAssignmentEntity assignment = new WorkScheduleAssignmentEntity();
        assignment.setId(1L);
        assignment.setEmployeeId(employeeId);
        assignment.setScheduleId(assignedScheduleId);
        assignment.setEffectiveFrom(date.minusDays(30));
        assignment.setEffectiveTo(date.plusDays(30));

        when(assignmentRepo.findByEmployeeIdAndEffectiveDate(eq(employeeId), eq(date)))
                .thenReturn(Collections.singletonList(assignment));

        // Schedule được gán
        WorkScheduleEntity assignedSchedule = new WorkScheduleEntity();
        assignedSchedule.setId(assignedScheduleId);
        assignedSchedule.setCompanyId(companyId);
        assignedSchedule.setName("Assigned Schedule");
        assignedSchedule.setType(ScheduleType.FLEXIBLE);
        assignedSchedule.setIsDefault(false);

        when(scheduleRepo.findByIdAndDeletedFalse(assignedScheduleId))
                .thenReturn(Optional.of(assignedSchedule));

        WorkScheduleResponse expectedResponse = WorkScheduleResponse.builder()
                .id(assignedScheduleId)
                .companyId(companyId)
                .name("Assigned Schedule")
                .type(ScheduleType.FLEXIBLE)
                .isDefault(false)
                .build();

        when(mapper.toResponse(assignedSchedule)).thenReturn(expectedResponse);

        WorkScheduleServiceImpl service = new WorkScheduleServiceImpl(
                scheduleRepo, assignmentRepo, userRepo, mapper);

        // Khi nhân viên có assignment, phải trả về schedule được gán
        WorkScheduleResponse result = service.getEffectiveSchedule(employeeId, companyId, date);

        assertNotNull(result, "Should return assigned schedule");
        assertEquals(assignedScheduleId, result.getId(), "Should return the assigned schedule ID");
        assertFalse(result.getIsDefault(), "Returned schedule should not be the default");
    }

    // === Generators ===

    @Provide
    Arbitrary<CreateWorkScheduleRequest> invalidTimeSchedules() {
        // Tạo schedule với start time >= end time (invalid)
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
                Arbitraries.of(ScheduleType.FIXED),
                Arbitraries.integers().between(0, 23),
                Arbitraries.integers().between(0, 59))
                .as((name, type, hour, minute) -> {
                    // Tạo start time
                    LocalTime startTime = LocalTime.of(hour, minute);
                    // End time = start time hoặc trước start time
                    LocalTime endTime = startTime.minusMinutes(1);
                    if (endTime.isAfter(startTime)) {
                        endTime = startTime; // Nếu wrap around, dùng cùng thời gian
                    }

                    WorkScheduleData data = WorkScheduleData.builder()
                            .defaultStartTime(startTime)
                            .defaultEndTime(endTime)
                            .defaultBreakMinutes(60)
                            .build();

                    return CreateWorkScheduleRequest.builder()
                            .name(name)
                            .type(type)
                            .isDefault(false)
                            .scheduleData(data)
                            .build();
                });
    }

    @Provide
    Arbitrary<CreateWorkScheduleRequest> validTimeSchedules() {
        // Tạo schedule với start time < end time (valid)
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
                Arbitraries.of(ScheduleType.FIXED),
                Arbitraries.integers().between(6, 12), // Start hour: 6-12
                Arbitraries.integers().between(0, 59))
                .as((name, type, startHour, minute) -> {
                    LocalTime startTime = LocalTime.of(startHour, minute);
                    // End time luôn sau start time ít nhất 1 giờ
                    LocalTime endTime = startTime.plusHours(6);

                    WorkScheduleData data = WorkScheduleData.builder()
                            .defaultStartTime(startTime)
                            .defaultEndTime(endTime)
                            .defaultBreakMinutes(60)
                            .build();

                    return CreateWorkScheduleRequest.builder()
                            .name(name)
                            .type(type)
                            .isDefault(false)
                            .scheduleData(data)
                            .build();
                });
    }

    @Provide
    Arbitrary<Long> companyIds() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    @Provide
    Arbitrary<Long> employeeIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<LocalDate> dates() {
        return Arbitraries.integers().between(2020, 2030)
                .flatMap(year -> Arbitraries.integers().between(1, 12)
                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                .map(day -> LocalDate.of(year, month, day))));
    }
}
