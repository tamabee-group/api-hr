package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.request.schedule.SelectScheduleRequest;
import com.tamabee.api_hr.dto.response.schedule.ScheduleSelectionResponse;
import com.tamabee.api_hr.dto.response.schedule.WorkScheduleResponse;
import com.tamabee.api_hr.entity.attendance.ScheduleSelectionEntity;
import com.tamabee.api_hr.entity.attendance.WorkScheduleAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.WorkScheduleEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.SelectionStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.ScheduleSelectionMapper;
import com.tamabee.api_hr.mapper.company.WorkScheduleMapper;
import com.tamabee.api_hr.repository.attendance.ScheduleSelectionRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.repository.attendance.WorkScheduleAssignmentRepository;
import com.tamabee.api_hr.repository.attendance.WorkScheduleRepository;
import com.tamabee.api_hr.service.company.interfaces.IScheduleSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation quản lý yêu cầu chọn lịch làm việc của nhân viên.
 * Xử lý tạo yêu cầu, phê duyệt/từ chối, và gợi ý lịch phù hợp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleSelectionServiceImpl implements IScheduleSelectionService {

        private final ScheduleSelectionRepository selectionRepository;
        private final WorkScheduleRepository workScheduleRepository;
        private final WorkScheduleAssignmentRepository assignmentRepository;
        private final UserRepository userRepository;
        private final ScheduleSelectionMapper selectionMapper;
        private final WorkScheduleMapper workScheduleMapper;

        // ==================== Employee Operations ====================

        @Override
        @Transactional
        public ScheduleSelectionResponse selectSchedule(Long employeeId,
                        SelectScheduleRequest request) {
                // Kiểm tra lịch làm việc tồn tại (không cần check companyId trong tenant DB)
                WorkScheduleEntity schedule = findScheduleById(request.getScheduleId());

                // Kiểm tra đã có yêu cầu đang chờ duyệt chưa
                if (selectionRepository.existsPendingByEmployeeId(employeeId)) {
                        throw new ConflictException(
                                        "Đã có yêu cầu chọn lịch đang chờ duyệt",
                                        ErrorCode.SELECTION_PENDING_EXISTS);
                }

                // Validate thời gian
                if (request.getEffectiveTo() != null &&
                                request.getEffectiveFrom().isAfter(request.getEffectiveTo())) {
                        throw new BadRequestException(
                                        "Ngày bắt đầu phải trước ngày kết thúc",
                                        ErrorCode.INVALID_SELECTION_DATE);
                }

                // Tạo yêu cầu chọn lịch
                ScheduleSelectionEntity entity = new ScheduleSelectionEntity();
                entity.setEmployeeId(employeeId);
                entity.setScheduleId(request.getScheduleId());
                entity.setEffectiveFrom(request.getEffectiveFrom());
                entity.setEffectiveTo(request.getEffectiveTo());
                entity.setStatus(SelectionStatus.PENDING);

                entity = selectionRepository.save(entity);
                log.info("Nhân viên {} đã tạo yêu cầu chọn lịch {} cho lịch {}",
                                employeeId, entity.getId(), request.getScheduleId());

                return selectionMapper.toResponse(
                                entity,
                                getEmployeeName(employeeId),
                                schedule.getName(),
                                null);
        }

        // ==================== Manager Operations ====================

        @Override
        @Transactional
        public ScheduleSelectionResponse approveSelection(Long selectionId, Long managerId) {
                ScheduleSelectionEntity entity = findSelectionById(selectionId);

                // Kiểm tra trạng thái
                if (entity.getStatus() != SelectionStatus.PENDING) {
                        throw new ConflictException(
                                        "Yêu cầu chọn lịch đã được xử lý",
                                        ErrorCode.SELECTION_ALREADY_PROCESSED);
                }

                // Cập nhật trạng thái
                entity.setStatus(SelectionStatus.APPROVED);
                entity.setApprovedBy(managerId);
                entity.setApprovedAt(LocalDateTime.now());

                entity = selectionRepository.save(entity);

                // Tạo assignment cho nhân viên
                createScheduleAssignment(entity);

                log.info("Manager {} đã phê duyệt yêu cầu chọn lịch {}", managerId, selectionId);

                WorkScheduleEntity schedule = findScheduleById(entity.getScheduleId());
                return selectionMapper.toResponse(
                                entity,
                                getEmployeeName(entity.getEmployeeId()),
                                schedule.getName(),
                                getEmployeeName(managerId));
        }

        @Override
        @Transactional
        public ScheduleSelectionResponse rejectSelection(Long selectionId, Long managerId, String reason) {
                // Kiểm tra lý do từ chối
                if (reason == null || reason.isBlank()) {
                        throw new BadRequestException(
                                        "Lý do từ chối không được để trống",
                                        ErrorCode.REJECTION_REASON_REQUIRED);
                }

                ScheduleSelectionEntity entity = findSelectionById(selectionId);

                // Kiểm tra trạng thái
                if (entity.getStatus() != SelectionStatus.PENDING) {
                        throw new ConflictException(
                                        "Yêu cầu chọn lịch đã được xử lý",
                                        ErrorCode.SELECTION_ALREADY_PROCESSED);
                }

                // Cập nhật trạng thái
                entity.setStatus(SelectionStatus.REJECTED);
                entity.setApprovedBy(managerId);
                entity.setApprovedAt(LocalDateTime.now());
                entity.setRejectionReason(reason);

                entity = selectionRepository.save(entity);
                log.info("Manager {} đã từ chối yêu cầu chọn lịch {} với lý do: {}",
                                managerId, selectionId, reason);

                WorkScheduleEntity schedule = findScheduleById(entity.getScheduleId());
                return selectionMapper.toResponse(
                                entity,
                                getEmployeeName(entity.getEmployeeId()),
                                schedule.getName(),
                                getEmployeeName(managerId));
        }

        // ==================== Query Operations ====================

        @Override
        @Transactional(readOnly = true)
        public ScheduleSelectionResponse getSelectionById(Long selectionId) {
                ScheduleSelectionEntity entity = findSelectionById(selectionId);
                WorkScheduleEntity schedule = findScheduleById(entity.getScheduleId());

                String approverName = entity.getApprovedBy() != null
                                ? getEmployeeName(entity.getApprovedBy())
                                : null;

                return selectionMapper.toResponse(
                                entity,
                                getEmployeeName(entity.getEmployeeId()),
                                schedule.getName(),
                                approverName);
        }

        @Override
        @Transactional(readOnly = true)
        public List<WorkScheduleResponse> getSuggestedSchedules(Long employeeId) {
                Set<Long> suggestedScheduleIds = new HashSet<>();

                // 1. Lấy các lịch đã được nhân viên chọn và được duyệt trước đó
                List<Long> pastApprovedScheduleIds = selectionRepository
                                .findApprovedScheduleIdsByEmployeeId(employeeId);
                suggestedScheduleIds.addAll(pastApprovedScheduleIds);

                // 2. Lấy lịch mặc định
                workScheduleRepository.findDefaultSchedule()
                                .ifPresent(schedule -> suggestedScheduleIds.add(schedule.getId()));

                // 3. Nếu chưa có gợi ý nào, lấy tất cả lịch
                if (suggestedScheduleIds.isEmpty()) {
                        Page<WorkScheduleEntity> allSchedules = workScheduleRepository
                                        .findByDeletedFalse(PageRequest.of(0, 10));
                        allSchedules.forEach(schedule -> suggestedScheduleIds.add(schedule.getId()));
                }

                // Chuyển đổi sang response
                List<WorkScheduleResponse> suggestions = new ArrayList<>();
                for (Long scheduleId : suggestedScheduleIds) {
                        workScheduleRepository.findByIdAndDeletedFalse(scheduleId)
                                        .map(workScheduleMapper::toResponse)
                                        .ifPresent(suggestions::add);
                }

                return suggestions;
        }

        @Override
        @Transactional(readOnly = true)
        public List<WorkScheduleResponse> getAvailableSchedules(LocalDate date) {
                // Lấy tất cả lịch
                Page<WorkScheduleEntity> schedules = workScheduleRepository
                                .findByDeletedFalse(PageRequest.of(0, 100));

                return schedules.stream()
                                .map(workScheduleMapper::toResponse)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ScheduleSelectionResponse> getPendingSelections(Pageable pageable) {
                return selectionRepository.findPending(pageable)
                                .map(this::mapToResponse);
        }

        @Override
        @Transactional(readOnly = true)
        public List<ScheduleSelectionResponse> getEmployeeSelectionHistory(Long employeeId) {
                return selectionRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        // ==================== Private Helper Methods ====================

        /**
         * Tìm yêu cầu chọn lịch theo ID
         */
        private ScheduleSelectionEntity findSelectionById(Long selectionId) {
                return selectionRepository.findById(selectionId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Không tìm thấy yêu cầu chọn lịch",
                                                ErrorCode.SELECTION_NOT_FOUND));
        }

        /**
         * Tìm lịch làm việc theo ID
         */
        private WorkScheduleEntity findScheduleById(Long scheduleId) {
                return workScheduleRepository.findByIdAndDeletedFalse(scheduleId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Không tìm thấy lịch làm việc",
                                                ErrorCode.SCHEDULE_NOT_FOUND));
        }

        /**
         * Lấy tên nhân viên
         */
        private String getEmployeeName(Long userId) {
                return userRepository.findById(userId)
                                .map(user -> user.getProfile() != null ? user.getProfile().getName() : user.getEmail())
                                .orElse("Unknown");
        }

        /**
         * Tạo assignment cho nhân viên sau khi yêu cầu được duyệt
         */
        private void createScheduleAssignment(ScheduleSelectionEntity selection) {
                WorkScheduleAssignmentEntity assignment = new WorkScheduleAssignmentEntity();
                assignment.setEmployeeId(selection.getEmployeeId());
                assignment.setScheduleId(selection.getScheduleId());
                assignment.setEffectiveFrom(selection.getEffectiveFrom());
                assignment.setEffectiveTo(selection.getEffectiveTo());

                assignmentRepository.save(assignment);
                log.info("Đã tạo assignment cho nhân viên {} với lịch {}",
                                selection.getEmployeeId(), selection.getScheduleId());
        }

        /**
         * Map entity sang response với đầy đủ thông tin
         */
        private ScheduleSelectionResponse mapToResponse(ScheduleSelectionEntity entity) {
                WorkScheduleEntity schedule = workScheduleRepository
                                .findByIdAndDeletedFalse(entity.getScheduleId())
                                .orElse(null);

                String scheduleName = schedule != null ? schedule.getName() : "Unknown";
                String approverName = entity.getApprovedBy() != null
                                ? getEmployeeName(entity.getApprovedBy())
                                : null;

                return selectionMapper.toResponse(
                                entity,
                                getEmployeeName(entity.getEmployeeId()),
                                scheduleName,
                                approverName);
        }
}
