package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.request.attendance.*;
import com.tamabee.api_hr.dto.response.attendance.BatchAssignmentResult;
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

import java.time.LocalDateTime;
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

        // Kiểm tra overlap: không được phân 2 ca trùng nhau cho cùng nhân viên trong
        // cùng ngày
        // ShiftAssignment không có soft delete nên dùng existsByEmployeeIdAndWorkDate
        boolean hasOverlap = shiftAssignmentRepository.existsByEmployeeIdAndWorkDate(
                request.getEmployeeId(), request.getWorkDate());
        if (hasOverlap) {
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

        List<ShiftAssignmentResponse> successfulAssignments = new ArrayList<>();
        List<BatchAssignmentResult.FailedAssignment> failedAssignments = new ArrayList<>();

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

                // Kiểm tra overlap - ShiftAssignment không có soft delete
                boolean hasOverlap = shiftAssignmentRepository.existsByEmployeeIdAndWorkDate(
                        employeeId, request.getWorkDate());
                if (hasOverlap) {
                    String employeeName = employee.getProfile() != null ? employee.getProfile().getName()
                            : employee.getEmail();
                    failedAssignments.add(BatchAssignmentResult.FailedAssignment.builder()
                            .employeeId(employeeId)
                            .employeeName(employeeName)
                            .reason("Đã có ca làm việc trong ngày này")
                            .build());
                    continue;
                }

                // Tạo assignment
                ShiftAssignmentRequest assignRequest = new ShiftAssignmentRequest();
                assignRequest.setEmployeeId(employeeId);
                assignRequest.setShiftTemplateId(request.getShiftTemplateId());
                assignRequest.setWorkDate(request.getWorkDate());

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

        return BatchAssignmentResult.builder()
                .totalRequested(request.getEmployeeIds().size())
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

        // Update swap request
        swapRequest.setStatus(SwapRequestStatus.APPROVED);
        swapRequest.setApprovedBy(approverId);
        swapRequest.setApprovedAt(LocalDateTime.now());
        swapRequest = shiftSwapRequestRepository.save(swapRequest);

        // Update both assignments - ShiftAssignment không có soft delete
        ShiftAssignmentEntity requesterAssignment = shiftAssignmentRepository
                .findById(swapRequest.getRequesterAssignmentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND));

        ShiftAssignmentEntity targetAssignment = shiftAssignmentRepository
                .findById(swapRequest.getTargetAssignmentId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHIFT_ASSIGNMENT_NOT_FOUND));

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
}
