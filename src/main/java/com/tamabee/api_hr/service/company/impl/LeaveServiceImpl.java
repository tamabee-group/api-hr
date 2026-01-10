package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.request.CreateLeaveRequest;
import com.tamabee.api_hr.dto.response.LeaveBalanceResponse;
import com.tamabee.api_hr.dto.response.LeaveRequestResponse;
import com.tamabee.api_hr.entity.leave.LeaveBalanceEntity;
import com.tamabee.api_hr.entity.leave.LeaveRequestEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.LeaveStatus;
import com.tamabee.api_hr.enums.LeaveType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.LeaveMapper;
import com.tamabee.api_hr.repository.LeaveBalanceRepository;
import com.tamabee.api_hr.repository.LeaveRequestRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.ILeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation quản lý nghỉ phép.
 * Hỗ trợ tạo yêu cầu nghỉ phép, phê duyệt/từ chối, và theo dõi số ngày phép.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements ILeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;
    private final LeaveMapper leaveMapper;

    // ==================== Employee Operations ====================

    @Override
    @Transactional
    public LeaveRequestResponse createLeaveRequest(Long employeeId, CreateLeaveRequest request) {
        // Validate ngày bắt đầu <= ngày kết thúc
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException(
                    "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc",
                    ErrorCode.LEAVE_INVALID_DATE_RANGE);
        }

        // Kiểm tra có yêu cầu trùng lặp không
        if (leaveRequestRepository.existsOverlappingRequest(
                employeeId, request.getStartDate(), request.getEndDate())) {
            throw new ConflictException(
                    "Đã có yêu cầu nghỉ phép trùng thời gian",
                    ErrorCode.LEAVE_OVERLAPPING);
        }

        // Tính số ngày nghỉ
        int totalDays = calculateLeaveDays(request.getStartDate(), request.getEndDate());

        // Kiểm tra số ngày phép còn lại (chỉ với loại phép có giới hạn)
        if (request.getLeaveType() != LeaveType.UNPAID) {
            int year = request.getStartDate().getYear();
            Integer remainingDays = leaveBalanceRepository.getRemainingDaysByEmployeeIdAndYearAndType(
                    employeeId, year, request.getLeaveType());

            if (remainingDays == null || remainingDays < totalDays) {
                throw new BadRequestException(
                        "Số ngày phép còn lại không đủ",
                        ErrorCode.LEAVE_INSUFFICIENT_BALANCE);
            }
        }

        // Tạo yêu cầu nghỉ phép
        LeaveRequestEntity entity = leaveMapper.toEntity(employeeId, request);
        entity.setTotalDays(totalDays);
        entity.setStatus(LeaveStatus.PENDING);

        entity = leaveRequestRepository.save(entity);
        log.info("Nhân viên {} đã tạo yêu cầu nghỉ phép {} từ {} đến {}",
                employeeId, entity.getId(), request.getStartDate(), request.getEndDate());

        return leaveMapper.toResponse(entity, getEmployeeName(employeeId), null);
    }

    @Override
    @Transactional
    public LeaveRequestResponse cancelLeaveRequest(Long requestId, Long employeeId) {
        LeaveRequestEntity entity = findLeaveRequest(requestId);

        // Kiểm tra quyền sở hữu
        if (!entity.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException(
                    "Không có quyền hủy yêu cầu nghỉ phép này",
                    ErrorCode.LEAVE_NOT_OWNER);
        }

        // Chỉ có thể hủy yêu cầu đang chờ duyệt
        if (entity.getStatus() != LeaveStatus.PENDING) {
            throw new ConflictException(
                    "Chỉ có thể hủy yêu cầu đang chờ duyệt",
                    ErrorCode.LEAVE_CANNOT_CANCEL);
        }

        // Set status = CANCELLED (LeaveRequest không có soft delete)
        entity.setStatus(LeaveStatus.CANCELLED);
        leaveRequestRepository.save(entity);

        log.info("Nhân viên {} đã hủy yêu cầu nghỉ phép {}", employeeId, requestId);
        return leaveMapper.toResponse(entity, getEmployeeName(employeeId), null);
    }

    // ==================== Manager Operations ====================

    @Override
    @Transactional
    public LeaveRequestResponse approveLeave(Long requestId, Long managerId) {
        LeaveRequestEntity entity = findLeaveRequest(requestId);

        // Kiểm tra trạng thái
        if (entity.getStatus() != LeaveStatus.PENDING) {
            throw new ConflictException(
                    "Yêu cầu nghỉ phép đã được xử lý",
                    ErrorCode.LEAVE_ALREADY_PROCESSED);
        }

        // Cập nhật trạng thái
        entity.setStatus(LeaveStatus.APPROVED);
        entity.setApprovedBy(managerId);
        entity.setApprovedAt(LocalDateTime.now());

        entity = leaveRequestRepository.save(entity);

        // Cập nhật số ngày phép đã sử dụng (chỉ với loại phép có giới hạn)
        if (entity.getLeaveType() != LeaveType.UNPAID) {
            updateLeaveBalanceOnApproval(entity);
        }

        log.info("Manager {} đã phê duyệt yêu cầu nghỉ phép {}", managerId, requestId);

        return leaveMapper.toResponse(
                entity,
                getEmployeeName(entity.getEmployeeId()),
                getEmployeeName(managerId));
    }

    @Override
    @Transactional
    public LeaveRequestResponse rejectLeave(Long requestId, Long managerId, String reason) {
        // Kiểm tra lý do từ chối
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException(
                    "Lý do từ chối không được để trống",
                    ErrorCode.REJECTION_REASON_REQUIRED);
        }

        LeaveRequestEntity entity = findLeaveRequest(requestId);

        // Kiểm tra trạng thái
        if (entity.getStatus() != LeaveStatus.PENDING) {
            throw new ConflictException(
                    "Yêu cầu nghỉ phép đã được xử lý",
                    ErrorCode.LEAVE_ALREADY_PROCESSED);
        }

        // Cập nhật trạng thái
        entity.setStatus(LeaveStatus.REJECTED);
        entity.setApprovedBy(managerId);
        entity.setApprovedAt(LocalDateTime.now());
        entity.setRejectionReason(reason);

        entity = leaveRequestRepository.save(entity);
        log.info("Manager {} đã từ chối yêu cầu nghỉ phép {} với lý do: {}",
                managerId, requestId, reason);

        return leaveMapper.toResponse(
                entity,
                getEmployeeName(entity.getEmployeeId()),
                getEmployeeName(managerId));
    }

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestResponse getLeaveRequestById(Long requestId) {
        LeaveRequestEntity entity = findLeaveRequest(requestId);

        String approverName = entity.getApprovedBy() != null
                ? getEmployeeName(entity.getApprovedBy())
                : null;

        return leaveMapper.toResponse(
                entity,
                getEmployeeName(entity.getEmployeeId()),
                approverName);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getPendingLeaveRequests(Pageable pageable) {
        return leaveRequestRepository.findPending(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getAllLeaveRequests(Pageable pageable) {
        return leaveRequestRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getEmployeeLeaveRequests(Long employeeId, Pageable pageable) {
        return leaveRequestRepository.findByEmployeeId(employeeId, pageable)
                .map(this::mapToResponse);
    }

    // ==================== Balance Operations ====================

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getLeaveBalance(Long employeeId, Integer year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .stream()
                .map(leaveMapper::toBalanceResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateLeaveBalance(Long employeeId, LeaveType type, Integer year, Integer adjustment) {
        LeaveBalanceEntity balance = leaveBalanceRepository
                .findByEmployeeIdAndYearAndLeaveType(employeeId, year, type)
                .orElse(null);

        if (balance == null) {
            // Tạo mới nếu chưa có
            balance = new LeaveBalanceEntity();
            balance.setEmployeeId(employeeId);
            balance.setYear(year);
            balance.setLeaveType(type);
            balance.setTotalDays(adjustment > 0 ? adjustment : 0);
            balance.setUsedDays(0);
            balance.setRemainingDays(adjustment > 0 ? adjustment : 0);
        } else {
            // Cập nhật số ngày
            int newTotal = balance.getTotalDays() + adjustment;
            balance.setTotalDays(Math.max(0, newTotal));
            balance.setRemainingDays(Math.max(0, balance.getTotalDays() - balance.getUsedDays()));
        }

        leaveBalanceRepository.save(balance);
        log.info("Đã cập nhật số ngày phép {} cho nhân viên {} năm {}: điều chỉnh {}",
                type, employeeId, year, adjustment);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Tìm yêu cầu nghỉ phép theo ID
     */
    private LeaveRequestEntity findLeaveRequest(Long requestId) {
        return leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy yêu cầu nghỉ phép",
                        ErrorCode.LEAVE_REQUEST_NOT_FOUND));
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
     * Tính số ngày nghỉ (bao gồm cả ngày bắt đầu và kết thúc)
     */
    private int calculateLeaveDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Cập nhật số ngày phép đã sử dụng khi yêu cầu được duyệt
     */
    private void updateLeaveBalanceOnApproval(LeaveRequestEntity leaveRequest) {
        int year = leaveRequest.getStartDate().getYear();
        LeaveBalanceEntity balance = leaveBalanceRepository
                .findByEmployeeIdAndYearAndLeaveType(
                        leaveRequest.getEmployeeId(), year, leaveRequest.getLeaveType())
                .orElse(null);

        if (balance != null) {
            balance.setUsedDays(balance.getUsedDays() + leaveRequest.getTotalDays());
            balance.setRemainingDays(balance.getTotalDays() - balance.getUsedDays());
            leaveBalanceRepository.save(balance);

            log.info("Đã cập nhật số ngày phép đã sử dụng cho nhân viên {}: {} ngày",
                    leaveRequest.getEmployeeId(), leaveRequest.getTotalDays());
        }
    }

    /**
     * Map entity sang response với đầy đủ thông tin
     */
    private LeaveRequestResponse mapToResponse(LeaveRequestEntity entity) {
        String approverName = entity.getApprovedBy() != null
                ? getEmployeeName(entity.getApprovedBy())
                : null;

        return leaveMapper.toResponse(
                entity,
                getEmployeeName(entity.getEmployeeId()),
                approverName);
    }
}
