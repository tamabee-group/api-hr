package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.request.attendance.*;
import com.tamabee.api_hr.dto.response.attendance.BatchAssignmentResult;
import com.tamabee.api_hr.dto.response.attendance.BatchDeleteResult;
import com.tamabee.api_hr.dto.response.attendance.ShiftAssignmentResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftSwapRequestResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftTemplateResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.attendance.ShiftAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.ShiftSwapRequestEntity;
import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.ShiftAssignmentStatus;
import com.tamabee.api_hr.enums.SwapRequestStatus;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.ShiftMapper;
import com.tamabee.api_hr.repository.attendance.ShiftAssignmentRepository;
import com.tamabee.api_hr.repository.attendance.ShiftSwapRequestRepository;
import com.tamabee.api_hr.repository.attendance.ShiftTemplateRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation cho quản lý ca làm việc.
 */
@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements IShiftService {

    private final ShiftTemplateRepository shiftTemplateRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftSwapRequestRepository shiftSwapRequestRepository;
    private final UserRepository userRepository;
    private final ShiftMapper shiftMapper;

    // ==================== Shift Template CRUD ====================

    @Override
    @Transactional
    public ShiftTemplateResponse createShiftTemplate(ShiftTemplateRequest request) {
        ShiftTemplateEntity entity = shiftMapper.toEntity(request);
        entity = shiftTemplateRepository.save(entity);
        return shiftMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public ShiftTemplateResponse updateShiftTemplate(Long id, ShiftTemplateRequest request) {
        ShiftTemplateEntity entity = shiftTemplateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_TEMPLATE_NOT_FOUND));

        shiftMapper.updateEntity(entity, request);
        entity = shiftTemplateRepository.save(entity);
        return shiftMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteShiftTemplate(Long id) {
        ShiftTemplateEntity entity = shiftTemplateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_TEMPLATE_NOT_FOUND));

        // Kiểm tra xem mẫu ca có đang được sử dụng không
        // ShiftAssignment không có soft delete nên dùng existsByShiftTemplateId
        boolean inUse = shiftAssignmentRepository.existsByShiftTemplateId(id);
        if (inUse) {
            throw new ConflictException(ErrorCode.SHIFT_TEMPLATE_IN_USE);
        }

        entity.setDeleted(true);
        shiftTemplateRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShiftTemplateResponse> getShiftTemplates(Pageable pageable) {
        Page<ShiftTemplateEntity> entities = shiftTemplateRepository.findByDeletedFalse(pageable);
        return entities.map(shiftMapper::toResponse);
    }

    // ==================== Shift Assignment ====================

    @Override
    @Transactional
    public ShiftAssignmentResponse assignShift(ShiftAssignmentRequest request) {
        // Validate shift template exists
        ShiftTemplateEntity shiftTemplate = shiftTemplateRepository
                .findByIdAndDeletedFalse(request.getShiftTemplateId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_TEMPLATE_NOT_FOUND));

        // Validate employee exists
        UserEntity employee = userRepository.findByIdAndDeletedFalse(request.getEmployeeId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra overlap thời gian: không được phân 2 ca trùng giờ cho cùng nhân viên trong cùng ngày
        boolean hasTimeOverlap = shiftAssignmentRepository.existsTimeOverlap(
                request.getEmployeeId(),
                request.getWorkDate(),
                shiftTemplate.getStartTime(),
                shiftTemplate.getEndTime());
        if (hasTimeOverlap) {
            throw new ConflictException(ErrorCode.SHIFT_OVERLAP_EXISTS);
        }

        ShiftAssignmentEntity entity = shiftMapper.toEntity(request);
        entity = shiftAssignmentRepository.save(entity);

        return shiftMapper.toResponse(entity, employee, shiftTemplate, null);
    }

    @Override
    @Transactional
    public BatchAssignmentResult batchAssignShift(BatchShiftAssignmentRequest request) {
        // Validate shift template exists
        ShiftTemplateEntity shiftTemplate = shiftTemplateRepository
                .findByIdAndDeletedFalse(request.getShiftTemplateId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_TEMPLATE_NOT_FOUND));

        // Xác định khoảng ngày cần phân ca
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : startDate;

        // Validate date range
        if (endDate.isBefore(startDate)) {
            throw new ConflictException(ErrorCode.INVALID_DATE_RANGE);
        }

        List<ShiftAssignmentResponse> successfulAssignments = new ArrayList<>();
        List<BatchAssignmentResult.FailedAssignment> failedAssignments = new ArrayList<>();

        // Tính tổng số request = số nhân viên * số ngày
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int totalRequested = request.getEmployeeIds().size() * (int) daysBetween;

        // Lặp qua từng ngày trong khoảng
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate workDate = currentDate;

            // Lặp qua từng nhân viên
            for (Long employeeId : request.getEmployeeIds()) {
                try {
                    // Validate employee exists
                    UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId).orElse(null);
                    if (employee == null) {
                        failedAssignments.add(BatchAssignmentResult.FailedAssignment.builder()
                                .employeeId(employeeId)
                                .reason("Nhân viên không tồn tại")
                                .build());
                        continue;
                    }

                    // Kiểm tra overlap thời gian - chỉ chặn nếu trùng giờ
                    boolean hasTimeOverlap = shiftAssignmentRepository.existsTimeOverlap(
                            employeeId,
                            workDate,
                            shiftTemplate.getStartTime(),
                            shiftTemplate.getEndTime());
                    if (hasTimeOverlap) {
                        String employeeName = employee.getProfile() != null ? employee.getProfile().getName()
                                : employee.getEmail();
                        failedAssignments.add(BatchAssignmentResult.FailedAssignment.builder()
                                .employeeId(employeeId)
                                .employeeName(employeeName)
                                .reason("Ca bị trùng giờ với ca khác trong ngày " + workDate)
                                .build());
                        continue;
                    }

                    // Tạo assignment
                    ShiftAssignmentRequest assignRequest = new ShiftAssignmentRequest();
                    assignRequest.setEmployeeId(employeeId);
                    assignRequest.setShiftTemplateId(request.getShiftTemplateId());
                    assignRequest.setWorkDate(workDate);

                    ShiftAssignmentEntity entity = shiftMapper.toEntity(assignRequest);
                    entity = shiftAssignmentRepository.save(entity);

                    successfulAssignments.add(shiftMapper.toResponse(entity, employee, shiftTemplate, null));
                } catch (Exception e) {
                    failedAssignments.add(BatchAssignmentResult.FailedAssignment.builder()
                            .employeeId(employeeId)
                            .reason(e.getMessage())
                            .build());
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return BatchAssignmentResult.builder()
                .totalRequested(totalRequested)
                .successCount(successfulAssignments.size())
                .failedCount(failedAssignments.size())
                .successfulAssignments(successfulAssignments)
                .failedAssignments(failedAssignments)
                .build();
    }

    @Override
    @Transactional
    public void unassignShift(Long assignmentId) {
        // ShiftAssignment không có soft delete - xóa thẳng
        ShiftAssignmentEntity entity = shiftAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND));

        shiftAssignmentRepository.delete(entity);
    }

    @Override
    @Transactional
    public BatchDeleteResult batchDeleteShiftAssignments(BatchDeleteShiftAssignmentRequest request) {
        // Xác định khoảng ngày cần xóa
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : startDate;

        // Validate date range
        if (endDate.isBefore(startDate)) {
            throw new ConflictException(ErrorCode.INVALID_DATE_RANGE);
        }

        int successCount = 0;
        int failedCount = 0;

        // Tính tổng số request = số nhân viên * số ngày
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int totalRequested = request.getEmployeeIds().size() * (int) daysBetween;

        // Lặp qua từng ngày trong khoảng
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate workDate = currentDate;

            // Lặp qua từng nhân viên
            for (Long employeeId : request.getEmployeeIds()) {
                try {
                    // Tìm tất cả assignments của nhân viên trong ngày này với status SCHEDULED
                    List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository
                            .findByEmployeeIdAndWorkDateAndStatus(employeeId, workDate, ShiftAssignmentStatus.SCHEDULED);

                    if (!assignments.isEmpty()) {
                        // Xóa tất cả assignments tìm được
                        shiftAssignmentRepository.deleteAll(assignments);
                        successCount += assignments.size();
                    } else {
                        // Không có assignment nào để xóa - vẫn tính là thất bại
                        failedCount++;
                    }
                } catch (Exception e) {
                    failedCount++;
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return BatchDeleteResult.builder()
                .totalRequested(totalRequested)
                .successCount(successCount)
                .failedCount(failedCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShiftAssignmentResponse> getShiftAssignments(ShiftAssignmentQuery query,
                                                             Pageable pageable) {
        Specification<ShiftAssignmentEntity> spec = buildShiftAssignmentSpec(query);
        Page<ShiftAssignmentEntity> entities = shiftAssignmentRepository.findAll(spec, pageable);

        return entities.map(entity -> {
            UserEntity employee = userRepository.findByIdAndDeletedFalse(entity.getEmployeeId()).orElse(null);
            ShiftTemplateEntity shiftTemplate = shiftTemplateRepository
                    .findByIdAndDeletedFalse(entity.getShiftTemplateId()).orElse(null);
            UserEntity swappedWith = entity.getSwappedWithEmployeeId() != null
                    ? userRepository.findByIdAndDeletedFalse(entity.getSwappedWithEmployeeId()).orElse(null)
                    : null;
            return shiftMapper.toResponse(entity, employee, shiftTemplate, swappedWith);
        });
    }

    private Specification<ShiftAssignmentEntity> buildShiftAssignmentSpec(ShiftAssignmentQuery query) {
        return (root, criteriaQuery, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (query.getEmployeeId() != null) {
                predicates.add(cb.equal(root.get("employeeId"), query.getEmployeeId()));
            }
            if (query.getShiftTemplateId() != null) {
                predicates.add(cb.equal(root.get("shiftTemplateId"), query.getShiftTemplateId()));
            }
            if (query.getWorkDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("workDate"), query.getWorkDateFrom()));
            }
            if (query.getWorkDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("workDate"), query.getWorkDateTo()));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // ==================== Shift Swap ====================

    @Override
    @Transactional
    public ShiftSwapRequestResponse requestSwap(Long employeeId, ShiftSwapRequest request) {
        // Validate assignments exist - ShiftAssignment không có soft delete
        ShiftAssignmentEntity requesterAssignment = shiftAssignmentRepository
                .findById(request.getRequesterAssignmentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND));

        ShiftAssignmentEntity targetAssignment = shiftAssignmentRepository
                .findById(request.getTargetAssignmentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND));

        // Validate requester owns the assignment
        if (!requesterAssignment.getEmployeeId().equals(employeeId)) {
            throw new ConflictException(ErrorCode.SHIFT_SWAP_NOT_ALLOWED);
        }

        // Validate target assignment belongs to target employee
        if (!targetAssignment.getEmployeeId().equals(request.getTargetEmployeeId())) {
            throw new ConflictException(ErrorCode.SHIFT_SWAP_NOT_ALLOWED);
        }

        // Kiểm tra xem sau khi swap có bị trùng giờ với các ca khác không
        validateSwapTimeOverlap(requesterAssignment, targetAssignment);

        ShiftSwapRequestEntity entity = shiftMapper.toEntity(request, employeeId);
        entity = shiftSwapRequestRepository.save(entity);

        return buildSwapRequestResponse(entity);
    }

    @Override
    @Transactional
    public ShiftSwapRequestResponse approveSwap(Long requestId, Long approverId) {
        // ShiftSwapRequest không có soft delete
        ShiftSwapRequestEntity swapRequest = shiftSwapRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_SWAP_REQUEST_NOT_FOUND));

        if (swapRequest.getStatus() != SwapRequestStatus.PENDING) {
            throw new ConflictException(ErrorCode.SHIFT_SWAP_ALREADY_PROCESSED);
        }

        // Update both assignments - ShiftAssignment không có soft delete
        ShiftAssignmentEntity requesterAssignment = shiftAssignmentRepository
                .findById(swapRequest.getRequesterAssignmentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND));

        ShiftAssignmentEntity targetAssignment = shiftAssignmentRepository
                .findById(swapRequest.getTargetAssignmentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND));

        // Kiểm tra lại xem sau khi swap có bị trùng giờ với các ca khác không
        validateSwapTimeOverlap(requesterAssignment, targetAssignment);

        // Update swap request
        swapRequest.setStatus(SwapRequestStatus.APPROVED);
        swapRequest.setApprovedBy(approverId);
        swapRequest.setApprovedAt(LocalDateTime.now());
        swapRequest = shiftSwapRequestRepository.save(swapRequest);

        // Swap the assignments
        Long tempEmployeeId = requesterAssignment.getEmployeeId();
        requesterAssignment.setEmployeeId(targetAssignment.getEmployeeId());
        requesterAssignment.setStatus(ShiftAssignmentStatus.SWAPPED);
        requesterAssignment.setSwappedWithEmployeeId(tempEmployeeId);
        requesterAssignment.setSwappedFromAssignmentId(targetAssignment.getId());

        targetAssignment.setEmployeeId(tempEmployeeId);
        targetAssignment.setStatus(ShiftAssignmentStatus.SWAPPED);
        targetAssignment.setSwappedWithEmployeeId(requesterAssignment.getEmployeeId());
        targetAssignment.setSwappedFromAssignmentId(requesterAssignment.getId());

        shiftAssignmentRepository.save(requesterAssignment);
        shiftAssignmentRepository.save(targetAssignment);

        return buildSwapRequestResponse(swapRequest);
    }

    @Override
    @Transactional
    public ShiftSwapRequestResponse rejectSwap(Long requestId, Long approverId, String reason) {
        // ShiftSwapRequest không có soft delete
        ShiftSwapRequestEntity swapRequest = shiftSwapRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_SWAP_REQUEST_NOT_FOUND));

        if (swapRequest.getStatus() != SwapRequestStatus.PENDING) {
            throw new ConflictException(ErrorCode.SHIFT_SWAP_ALREADY_PROCESSED);
        }

        swapRequest.setStatus(SwapRequestStatus.REJECTED);
        swapRequest.setApprovedBy(approverId);
        swapRequest.setApprovedAt(LocalDateTime.now());
        swapRequest.setRejectionReason(reason);
        swapRequest = shiftSwapRequestRepository.save(swapRequest);

        return buildSwapRequestResponse(swapRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShiftSwapRequestResponse> getSwapRequests(SwapRequestQuery query,
            Pageable pageable) {
        Specification<ShiftSwapRequestEntity> spec = buildSwapRequestSpec(query);
        Page<ShiftSwapRequestEntity> entities = shiftSwapRequestRepository.findAll(spec, pageable);
        return entities.map(this::buildSwapRequestResponse);
    }

    private Specification<ShiftSwapRequestEntity> buildSwapRequestSpec(SwapRequestQuery query) {
        return (root, criteriaQuery, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (query.getRequesterId() != null) {
                predicates.add(cb.equal(root.get("requesterId"), query.getRequesterId()));
            }
            if (query.getTargetEmployeeId() != null) {
                predicates.add(cb.equal(root.get("targetEmployeeId"), query.getTargetEmployeeId()));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt").as(java.time.LocalDate.class),
                        query.getCreatedFrom()));
            }
            if (query.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt").as(java.time.LocalDate.class),
                        query.getCreatedTo()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private ShiftSwapRequestResponse buildSwapRequestResponse(ShiftSwapRequestEntity entity) {
        UserEntity requester = userRepository.findByIdAndDeletedFalse(entity.getRequesterId()).orElse(null);
        UserEntity targetEmployee = userRepository.findByIdAndDeletedFalse(entity.getTargetEmployeeId()).orElse(null);
        UserEntity approver = entity.getApprovedBy() != null
                ? userRepository.findByIdAndDeletedFalse(entity.getApprovedBy()).orElse(null)
                : null;

        // ShiftAssignment không có soft delete
        ShiftAssignmentEntity requesterAssignment = shiftAssignmentRepository
                .findById(entity.getRequesterAssignmentId()).orElse(null);
        ShiftAssignmentEntity targetAssignment = shiftAssignmentRepository
                .findById(entity.getTargetAssignmentId()).orElse(null);

        ShiftAssignmentResponse requesterAssignmentResponse = null;
        ShiftAssignmentResponse targetAssignmentResponse = null;

        if (requesterAssignment != null) {
            ShiftTemplateEntity shiftTemplate = shiftTemplateRepository
                    .findByIdAndDeletedFalse(requesterAssignment.getShiftTemplateId()).orElse(null);
            requesterAssignmentResponse = shiftMapper.toResponse(requesterAssignment, requester, shiftTemplate, null);
        }

        if (targetAssignment != null) {
            ShiftTemplateEntity shiftTemplate = shiftTemplateRepository
                    .findByIdAndDeletedFalse(targetAssignment.getShiftTemplateId()).orElse(null);
            targetAssignmentResponse = shiftMapper.toResponse(targetAssignment, targetEmployee, shiftTemplate, null);
        }

        return shiftMapper.toResponse(entity, requester, targetEmployee,
                requesterAssignmentResponse, targetAssignmentResponse, approver);
    }

    /**
     * Kiểm tra xem sau khi swap 2 ca có bị trùng giờ với các ca khác trong cùng ngày không
     */
    private void validateSwapTimeOverlap(ShiftAssignmentEntity requesterAssignment,
                                          ShiftAssignmentEntity targetAssignment) {
        // Lấy thông tin shift template
        ShiftTemplateEntity requesterTemplate = shiftTemplateRepository
                .findByIdAndDeletedFalse(requesterAssignment.getShiftTemplateId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_TEMPLATE_NOT_FOUND));

        ShiftTemplateEntity targetTemplate = shiftTemplateRepository
                .findByIdAndDeletedFalse(targetAssignment.getShiftTemplateId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_TEMPLATE_NOT_FOUND));

        // Lấy thời gian ca từ template
        LocalTime requesterStart = requesterTemplate.getStartTime();
        LocalTime requesterEnd = requesterTemplate.getEndTime();

        LocalTime targetStart = targetTemplate.getStartTime();
        LocalTime targetEnd = targetTemplate.getEndTime();

        // Kiểm tra requester nhận ca của target có bị trùng với các ca khác của requester không
        List<ShiftAssignmentEntity> requesterOtherAssignments = shiftAssignmentRepository
                .findByEmployeeIdAndWorkDate(requesterAssignment.getEmployeeId(), requesterAssignment.getWorkDate());

        for (ShiftAssignmentEntity otherAssignment : requesterOtherAssignments) {
            // Bỏ qua chính ca đang swap
            if (otherAssignment.getId().equals(requesterAssignment.getId())) {
                continue;
            }

            // Lấy thời gian ca khác từ template
            ShiftTemplateEntity otherTemplate = shiftTemplateRepository
                    .findByIdAndDeletedFalse(otherAssignment.getShiftTemplateId())
                    .orElse(null);
            if (otherTemplate == null) continue;

            LocalTime otherStart = otherTemplate.getStartTime();
            LocalTime otherEnd = otherTemplate.getEndTime();

            // Kiểm tra trùng giờ: ca target với ca khác của requester
            if (isTimeOverlap(targetStart, targetEnd, otherStart, otherEnd)) {
                throw new ConflictException(ErrorCode.SHIFT_OVERLAP_EXISTS);
            }
        }

        // Kiểm tra target nhận ca của requester có bị trùng với các ca khác của target không
        List<ShiftAssignmentEntity> targetOtherAssignments = shiftAssignmentRepository
                .findByEmployeeIdAndWorkDate(targetAssignment.getEmployeeId(), targetAssignment.getWorkDate());

        for (ShiftAssignmentEntity otherAssignment : targetOtherAssignments) {
            // Bỏ qua chính ca đang swap
            if (otherAssignment.getId().equals(targetAssignment.getId())) {
                continue;
            }

            // Lấy thời gian ca khác từ template
            ShiftTemplateEntity otherTemplate = shiftTemplateRepository
                    .findByIdAndDeletedFalse(otherAssignment.getShiftTemplateId())
                    .orElse(null);
            if (otherTemplate == null) continue;

            LocalTime otherStart = otherTemplate.getStartTime();
            LocalTime otherEnd = otherTemplate.getEndTime();

            // Kiểm tra trùng giờ: ca requester với ca khác của target
            if (isTimeOverlap(requesterStart, requesterEnd, otherStart, otherEnd)) {
                throw new ConflictException(ErrorCode.SHIFT_OVERLAP_EXISTS);
            }
        }
    }

    /**
     * Kiểm tra xem 2 khoảng thời gian có trùng nhau không
     */
    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        // Trùng nếu: start1 < end2 AND start2 < end1
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
