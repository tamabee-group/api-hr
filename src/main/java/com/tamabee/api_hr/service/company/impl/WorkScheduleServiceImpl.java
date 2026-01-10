package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.config.BreakPeriod;
import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.dto.request.AssignScheduleRequest;
import com.tamabee.api_hr.dto.request.CreateWorkScheduleRequest;
import com.tamabee.api_hr.dto.request.UpdateWorkScheduleRequest;
import com.tamabee.api_hr.dto.response.WorkScheduleAssignmentResponse;
import com.tamabee.api_hr.dto.response.WorkScheduleResponse;
import com.tamabee.api_hr.entity.attendance.WorkScheduleAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.WorkScheduleEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.ScheduleType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.WorkScheduleMapper;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WorkScheduleAssignmentRepository;
import com.tamabee.api_hr.repository.WorkScheduleRepository;
import com.tamabee.api_hr.service.company.IWorkScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service implementation quản lý lịch làm việc của công ty.
 * Hỗ trợ validation thời gian và fallback về lịch mặc định.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkScheduleServiceImpl implements IWorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;
    private final WorkScheduleAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final WorkScheduleMapper workScheduleMapper;

    // ==================== CRUD Operations ====================

    @Override
    @Transactional
    public WorkScheduleResponse createSchedule(CreateWorkScheduleRequest request) {
        // Kiểm tra tên đã tồn tại chưa
        if (workScheduleRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new ConflictException("Tên lịch làm việc đã tồn tại", ErrorCode.SCHEDULE_NAME_EXISTS);
        }

        // Validate schedule data
        validateScheduleData(request.getType(), request.getScheduleData());

        // Tạo entity
        WorkScheduleEntity entity = workScheduleMapper.toEntity(request);

        // Nếu đánh dấu là default, bỏ default của các lịch khác
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultSchedule();
        }

        entity = workScheduleRepository.save(entity);
        log.info("Đã tạo lịch làm việc: {}", entity.getName());

        return workScheduleMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public WorkScheduleResponse updateSchedule(Long scheduleId, UpdateWorkScheduleRequest request) {
        WorkScheduleEntity entity = findScheduleById(scheduleId);

        // Kiểm tra tên trùng (nếu đổi tên)
        if (request.getName() != null && !request.getName().equals(entity.getName())) {
            if (workScheduleRepository.existsByNameAndDeletedFalse(request.getName())) {
                throw new ConflictException("Tên lịch làm việc đã tồn tại", ErrorCode.SCHEDULE_NAME_EXISTS);
            }
        }

        // Validate schedule data nếu có cập nhật
        ScheduleType typeToValidate = request.getType() != null ? request.getType() : entity.getType();
        if (request.getScheduleData() != null) {
            validateScheduleData(typeToValidate, request.getScheduleData());
        }

        // Nếu đánh dấu là default, bỏ default của các lịch khác
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(entity.getIsDefault())) {
            clearDefaultSchedule();
        }

        workScheduleMapper.updateEntity(entity, request);
        entity = workScheduleRepository.save(entity);
        log.info("Đã cập nhật lịch làm việc: {}", scheduleId);

        return workScheduleMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        WorkScheduleEntity entity = findScheduleById(scheduleId);

        // Kiểm tra có assignment nào đang sử dụng không
        List<WorkScheduleAssignmentEntity> assignments = assignmentRepository
                .findAllByScheduleId(scheduleId);
        if (!assignments.isEmpty()) {
            throw new ConflictException("Lịch làm việc đang được sử dụng, không thể xóa", ErrorCode.SCHEDULE_IN_USE);
        }

        entity.setDeleted(true);
        workScheduleRepository.save(entity);
        log.info("Đã xóa lịch làm việc: {}", scheduleId);
    }

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<WorkScheduleResponse> getSchedules(Pageable pageable) {
        return workScheduleRepository.findByDeletedFalse(pageable)
                .map(workScheduleMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkScheduleResponse getScheduleById(Long scheduleId) {
        WorkScheduleEntity entity = findScheduleById(scheduleId);
        return workScheduleMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkScheduleResponse getDefaultSchedule() {
        return workScheduleRepository.findDefault()
                .map(workScheduleMapper::toResponse)
                .orElse(null);
    }

    // ==================== Employee Schedule ====================

    @Override
    @Transactional(readOnly = true)
    public WorkScheduleResponse getEffectiveSchedule(Long employeeId, LocalDate date) {
        // Tìm assignment hiệu lực của nhân viên
        List<WorkScheduleAssignmentEntity> assignments = assignmentRepository
                .findByEmployeeIdAndEffectiveDate(employeeId, date);

        if (!assignments.isEmpty()) {
            // Lấy assignment mới nhất (đã sort theo effectiveFrom DESC)
            Long scheduleId = assignments.get(0).getScheduleId();
            return workScheduleRepository.findByIdAndDeletedFalse(scheduleId)
                    .map(workScheduleMapper::toResponse)
                    .orElse(null);
        }

        return workScheduleRepository.findDefault()
                .map(workScheduleMapper::toResponse)
                .orElse(null);
    }

    // ==================== Assignment Operations ====================

    @Override
    @Transactional
    public WorkScheduleAssignmentResponse assignScheduleToEmployee(
            Long scheduleId,
            Long employeeId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo) {

        // Validate schedule tồn tại
        WorkScheduleEntity schedule = findScheduleById(scheduleId);

        // Validate employee tồn tại
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));

        // Validate thời gian
        if (effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc", ErrorCode.INVALID_SCHEDULE_TIME);
        }

        // Kiểm tra overlap với assignment hiện có
        LocalDate endDate = effectiveTo != null ? effectiveTo : LocalDate.of(9999, 12, 31);
        if (assignmentRepository.existsOverlappingAssignment(employeeId, effectiveFrom, endDate)) {
            throw new ConflictException("Lịch làm việc bị trùng với lịch hiện có", ErrorCode.SCHEDULE_OVERLAP);
        }

        // Tạo assignment
        WorkScheduleAssignmentEntity assignment = new WorkScheduleAssignmentEntity();
        assignment.setEmployeeId(employeeId);
        assignment.setScheduleId(scheduleId);
        assignment.setEffectiveFrom(effectiveFrom);
        assignment.setEffectiveTo(effectiveTo);

        assignment = assignmentRepository.save(assignment);
        log.info("Đã gán lịch {} cho nhân viên {}", scheduleId, employeeId);

        String employeeName = employee.getProfile() != null ? employee.getProfile().getName() : employee.getEmail();
        return workScheduleMapper.toAssignmentResponse(assignment, employeeName, schedule.getName());
    }

    @Override
    @Transactional
    public List<WorkScheduleAssignmentResponse> assignScheduleToEmployees(
            Long scheduleId,
            AssignScheduleRequest request) {

        List<WorkScheduleAssignmentResponse> results = new ArrayList<>();

        for (Long employeeId : request.getEmployeeIds()) {
            try {
                WorkScheduleAssignmentResponse response = assignScheduleToEmployee(
                        scheduleId,
                        employeeId,
                        request.getEffectiveFrom(),
                        request.getEffectiveTo());
                results.add(response);
            } catch (Exception e) {
                log.warn("Không thể gán lịch cho nhân viên {}: {}", employeeId, e.getMessage());
            }
        }

        return results;
    }

    @Override
    @Transactional
    public void removeAssignment(Long assignmentId) {
        WorkScheduleAssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy assignment",
                        ErrorCode.SCHEDULE_ASSIGNMENT_NOT_FOUND));

        // WorkScheduleAssignment không có soft delete - xóa thẳng
        assignmentRepository.delete(assignment);
        log.info("Đã xóa assignment: {}", assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkScheduleAssignmentResponse> getAssignmentsBySchedule(Long scheduleId, Pageable pageable) {
        WorkScheduleEntity schedule = findScheduleById(scheduleId);

        return assignmentRepository.findByScheduleId(scheduleId, pageable)
                .map(assignment -> {
                    String employeeName = getEmployeeName(assignment.getEmployeeId());
                    return workScheduleMapper.toAssignmentResponse(assignment, employeeName, schedule.getName());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkScheduleAssignmentResponse> getAssignmentsByEmployee(Long employeeId) {
        List<WorkScheduleAssignmentEntity> assignments = assignmentRepository.findByEmployeeIdAndEffectiveDateRange(
                employeeId,
                LocalDate.of(1900, 1, 1),
                LocalDate.of(9999, 12, 31));

        String employeeName = getEmployeeName(employeeId);

        return assignments.stream()
                .map(assignment -> {
                    String scheduleName = workScheduleRepository.findByIdAndDeletedFalse(assignment.getScheduleId())
                            .map(WorkScheduleEntity::getName)
                            .orElse("Unknown");
                    return workScheduleMapper.toAssignmentResponse(assignment, employeeName, scheduleName);
                })
                .toList();
    }

    // ==================== Private Helper Methods ====================

    /**
     * Tìm schedule theo ID
     */
    private WorkScheduleEntity findScheduleById(Long scheduleId) {
        return workScheduleRepository.findByIdAndDeletedFalse(scheduleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lịch làm việc", ErrorCode.SCHEDULE_NOT_FOUND));
    }

    /**
     * Bỏ đánh dấu default của tất cả lịch
     */
    private void clearDefaultSchedule() {
        workScheduleRepository.findDefault()
                .ifPresent(schedule -> {
                    schedule.setIsDefault(false);
                    workScheduleRepository.save(schedule);
                });
    }

    /**
     * Lấy tên nhân viên
     */
    private String getEmployeeName(Long employeeId) {
        return userRepository.findById(employeeId)
                .map(user -> user.getProfile() != null ? user.getProfile().getName() : user.getEmail())
                .orElse("Unknown");
    }

    /**
     * Validate schedule data theo loại lịch
     */
    private void validateScheduleData(ScheduleType type, WorkScheduleData data) {
        if (data == null) {
            return;
        }

        switch (type) {
            case FIXED -> validateFixedSchedule(data);
            case FLEXIBLE -> validateFlexibleSchedule(data);
            case SHIFT -> validateShiftSchedule(data);
        }
    }

    /**
     * Validate lịch cố định
     */
    private void validateFixedSchedule(WorkScheduleData data) {
        LocalTime startTime = data.getDefaultStartTime();
        LocalTime endTime = data.getDefaultEndTime();

        if (startTime != null && endTime != null) {
            if (!startTime.isBefore(endTime)) {
                throw new BadRequestException("Giờ bắt đầu phải trước giờ kết thúc", ErrorCode.INVALID_SCHEDULE_TIME);
            }

            // Validate break periods nằm trong working hours
            validateBreakPeriods(data.getBreakPeriods(), startTime, endTime);
        }
    }

    /**
     * Validate lịch linh hoạt
     */
    private void validateFlexibleSchedule(WorkScheduleData data) {
        Map<String, WorkScheduleData.DailySchedule> dailySchedules = data.getDailySchedules();
        if (dailySchedules == null) {
            return;
        }

        for (Map.Entry<String, WorkScheduleData.DailySchedule> entry : dailySchedules.entrySet()) {
            WorkScheduleData.DailySchedule daily = entry.getValue();
            if (daily.getStartTime() != null && daily.getEndTime() != null) {
                if (!daily.getStartTime().isBefore(daily.getEndTime())) {
                    throw new BadRequestException(
                            "Giờ bắt đầu phải trước giờ kết thúc cho ngày " + entry.getKey(),
                            ErrorCode.INVALID_SCHEDULE_TIME);
                }
            }
        }
    }

    /**
     * Validate lịch theo ca
     */
    private void validateShiftSchedule(WorkScheduleData data) {
        List<WorkScheduleData.ShiftSchedule> shifts = data.getShifts();
        if (shifts == null) {
            return;
        }

        for (WorkScheduleData.ShiftSchedule shift : shifts) {
            if (shift.getStartTime() != null && shift.getEndTime() != null) {
                if (!shift.getStartTime().isBefore(shift.getEndTime())) {
                    throw new BadRequestException(
                            "Giờ bắt đầu phải trước giờ kết thúc cho ca " + shift.getShiftName(),
                            ErrorCode.INVALID_SCHEDULE_TIME);
                }
            }
        }

        // Validate break periods
        validateBreakPeriods(data.getBreakPeriods(), null, null);
    }

    /**
     * Validate break periods
     * - Kiểm tra thời gian bắt đầu trước thời gian kết thúc
     * - Kiểm tra break periods nằm trong working hours (nếu có)
     * - Tính tổng thời gian giải lao
     */
    private void validateBreakPeriods(List<BreakPeriod> breakPeriods, LocalTime workStart, LocalTime workEnd) {
        if (breakPeriods == null || breakPeriods.isEmpty()) {
            return;
        }

        for (BreakPeriod breakPeriod : breakPeriods) {
            LocalTime breakStart = breakPeriod.getStartTime();
            LocalTime breakEnd = breakPeriod.getEndTime();

            // Kiểm tra thời gian bắt đầu trước thời gian kết thúc
            if (breakStart != null && breakEnd != null) {
                if (!breakStart.isBefore(breakEnd)) {
                    throw new BadRequestException(
                            "Giờ bắt đầu giải lao phải trước giờ kết thúc: " + breakPeriod.getName(),
                            ErrorCode.INVALID_SCHEDULE_TIME);
                }

                // Kiểm tra break periods nằm trong working hours (cho FIXED schedule)
                if (workStart != null && workEnd != null) {
                    if (breakStart.isBefore(workStart) || breakEnd.isAfter(workEnd)) {
                        throw new BadRequestException(
                                "Giờ giải lao phải nằm trong giờ làm việc: " + breakPeriod.getName(),
                                ErrorCode.BREAK_OUTSIDE_WORKING_HOURS);
                    }
                }
            }

            // Kiểm tra duration nếu có
            if (breakPeriod.getDurationMinutes() != null && breakPeriod.getDurationMinutes() < 0) {
                throw new BadRequestException(
                        "Thời gian giải lao không được âm: " + breakPeriod.getName(),
                        ErrorCode.INVALID_BREAK_DURATION);
            }
        }
    }
}
