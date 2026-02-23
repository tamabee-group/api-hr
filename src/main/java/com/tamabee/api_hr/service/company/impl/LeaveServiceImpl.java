package com.tamabee.api_hr.service.company.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.constants.NotificationCode;
import com.tamabee.api_hr.dto.request.leave.BulkAllocateLeaveRequest;
import com.tamabee.api_hr.dto.request.leave.CreateLeaveRequest;
import com.tamabee.api_hr.dto.request.leave.UpdateLeaveBalanceRequest;
import com.tamabee.api_hr.dto.response.department.DefaultApproverResponse;
import com.tamabee.api_hr.dto.response.leave.BulkAllocateResponse;
import com.tamabee.api_hr.dto.response.leave.LeaveBalanceResponse;
import com.tamabee.api_hr.dto.response.leave.LeaveBalanceSummaryResponse;
import com.tamabee.api_hr.dto.response.leave.LeaveRequestResponse;
import com.tamabee.api_hr.entity.leave.LeaveBalanceEntity;
import com.tamabee.api_hr.entity.leave.LeaveRequestEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.LeaveStatus;
import com.tamabee.api_hr.enums.LeaveType;
import com.tamabee.api_hr.enums.NotificationType;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.LeaveMapper;
import com.tamabee.api_hr.repository.leave.LeaveBalanceRepository;
import com.tamabee.api_hr.repository.leave.LeaveRequestRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IDepartmentService;
import com.tamabee.api_hr.service.company.interfaces.ILeaveService;
import com.tamabee.api_hr.service.core.interfaces.INotificationService;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final IDepartmentService departmentService;
    private final INotificationService notificationService;

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

        // Kiểm tra số ngày phép còn lại (chỉ với loại phép có giới hạn: ANNUAL, SICK)
        // Các loại khác (UNPAID, MATERNITY, PATERNITY, BEREAVEMENT, OTHER) không giới hạn
        if (request.getLeaveType() == LeaveType.ANNUAL || request.getLeaveType() == LeaveType.SICK) {
            int year = request.getStartDate().getYear();
            Integer remainingDays = leaveBalanceRepository.getRemainingDaysByEmployeeIdAndYearAndType(
                    employeeId, year, request.getLeaveType());

            if (remainingDays == null || remainingDays < totalDays) {
                throw new BadRequestException(
                        "Số ngày phép còn lại không đủ",
                        ErrorCode.LEAVE_INSUFFICIENT_BALANCE);
            }
        }

        // Xác định người duyệt: ưu tiên từ request, nếu không có thì lấy từ department manager
        Long approverId = request.getApproverId();
        if (approverId == null) {
            DefaultApproverResponse defaultApprover = departmentService.getDefaultApprover(employeeId);
            if (defaultApprover != null) {
                approverId = defaultApprover.getId();
            }
        }

        // Tạo yêu cầu nghỉ phép
        LeaveRequestEntity entity = leaveMapper.toEntity(employeeId, request, approverId);
        entity.setTotalDays(totalDays);
        entity.setStatus(LeaveStatus.PENDING);

        entity = leaveRequestRepository.save(entity);
        log.info("Nhân viên {} đã tạo yêu cầu nghỉ phép {} từ {} đến {}",
                employeeId, entity.getId(), request.getStartDate(), request.getEndDate());

        // Gửi thông báo cho admin/manager
        notifyAdminManagerOnLeaveSubmit(entity.getId(), employeeId, request.getStartDate(), request.getEndDate());

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
        entity.setApprovedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));

        entity = leaveRequestRepository.save(entity);

        // Cập nhật số ngày phép đã sử dụng (chỉ với loại phép có giới hạn: ANNUAL, SICK)
        if (entity.getLeaveType() == LeaveType.ANNUAL || entity.getLeaveType() == LeaveType.SICK) {
            updateLeaveBalanceOnApproval(entity);
        }

        log.info("Manager {} đã phê duyệt yêu cầu nghỉ phép {}", managerId, requestId);

        // Gửi thông báo cho nhân viên đã gửi yêu cầu
        notifyEmployeeOnLeaveApproved(entity.getId(), entity.getEmployeeId(), entity.getStartDate(), entity.getEndDate());

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
        entity.setApprovedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        entity.setRejectionReason(reason);

        entity = leaveRequestRepository.save(entity);
        log.info("Manager {} đã từ chối yêu cầu nghỉ phép {} với lý do: {}",
                managerId, requestId, reason);

        // Gửi thông báo cho nhân viên đã gửi yêu cầu
        notifyEmployeeOnLeaveRejected(entity.getId(), entity.getEmployeeId(), entity.getStartDate(), entity.getEndDate(), reason);

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
        return userRepository.findWithProfileByIdAndDeletedFalse(userId)
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

    // ==================== Notification Helper Methods ====================

    /**
     * Gửi thông báo cho admin/manager khi có yêu cầu nghỉ phép mới
     */
    private void notifyAdminManagerOnLeaveSubmit(Long leaveId, Long employeeId, LocalDate startDate, LocalDate endDate) {
        try {
            // Lấy danh sách admin/manager trong công ty
            List<UserRole> adminManagerRoles = List.of(UserRole.ADMIN_COMPANY, UserRole.MANAGER_COMPANY);
            List<UserEntity> adminManagers = userRepository.findByRoleInAndDeletedFalse(adminManagerRoles);

            if (adminManagers.isEmpty()) {
                log.warn("Không tìm thấy admin/manager để gửi thông báo nghỉ phép");
                return;
            }

            // Lấy danh sách ID của admin/manager
            List<Long> adminManagerIds = adminManagers.stream()
                    .map(UserEntity::getId)
                    .collect(Collectors.toList());

            // Tạo params cho thông báo
            Map<String, Object> params = new HashMap<>();
            params.put("employeeName", getEmployeeName(employeeId));
            params.put("startDate", startDate.toString());
            params.put("endDate", endDate.toString());

            // Gửi thông báo hàng loạt cho admin/manager với URL chứa leaveId
            notificationService.createBulkNotifications(
                    adminManagerIds,
                    NotificationCode.LEAVE_SUBMITTED,
                    params,
                    "/dashboard/leaves?id=" + leaveId,
                    NotificationType.LEAVE);

            log.info("Đã gửi thông báo nghỉ phép mới cho {} admin/manager", adminManagerIds.size());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo nghỉ phép mới: {}", e.getMessage());
        }
    }

    /**
     * Gửi thông báo cho nhân viên khi yêu cầu nghỉ phép được duyệt
     */
    private void notifyEmployeeOnLeaveApproved(Long leaveId, Long employeeId, LocalDate startDate, LocalDate endDate) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startDate", startDate.toString());
            params.put("endDate", endDate.toString());

            notificationService.createNotification(
                    employeeId,
                    NotificationCode.LEAVE_APPROVED,
                    params,
                    "/me/leave?id=" + leaveId,
                    NotificationType.LEAVE);

            log.info("Đã gửi thông báo duyệt nghỉ phép cho nhân viên {}", employeeId);
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo duyệt nghỉ phép: {}", e.getMessage());
        }
    }

    /**
     * Gửi thông báo cho nhân viên khi yêu cầu nghỉ phép bị từ chối
     */
    private void notifyEmployeeOnLeaveRejected(Long leaveId, Long employeeId, LocalDate startDate, LocalDate endDate, String reason) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("startDate", startDate.toString());
            params.put("endDate", endDate.toString());
            params.put("reason", reason != null ? reason : "");

            notificationService.createNotification(
                    employeeId,
                    NotificationCode.LEAVE_REJECTED,
                    params,
                    "/me/leave?id=" + leaveId,
                    NotificationType.LEAVE);

            log.info("Đã gửi thông báo từ chối nghỉ phép cho nhân viên {}", employeeId);
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo từ chối nghỉ phép: {}", e.getMessage());
        }
    }

    // ==================== Leave Balance Management ====================

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveBalanceSummaryResponse> getAllLeaveBalances(Integer year, String search, Pageable pageable) {
        // Lấy tất cả nhân viên (có soft delete - dùng findByDeletedFalse)
        List<UserEntity> allEmployees = userRepository.findByDeletedFalse();

        // Lọc theo từ khóa tìm kiếm (tên hoặc mã nhân viên)
        List<UserEntity> filteredEmployees = allEmployees;
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase().trim();
            filteredEmployees = allEmployees.stream()
                    .filter(user -> {
                        String name = user.getProfile() != null ? user.getProfile().getName() : "";
                        String code = user.getEmployeeCode() != null ? user.getEmployeeCode() : "";
                        return name.toLowerCase().contains(searchLower)
                                || code.toLowerCase().contains(searchLower);
                    })
                    .collect(Collectors.toList());
        }

        // Tính toán phân trang
        int totalElements = filteredEmployees.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), totalElements);

        // Lấy danh sách nhân viên cho trang hiện tại
        List<UserEntity> pagedEmployees = start < totalElements
                ? filteredEmployees.subList(start, end)
                : new ArrayList<>();

        // Lấy danh sách ID nhân viên
        List<Long> employeeIds = pagedEmployees.stream()
                .map(UserEntity::getId)
                .collect(Collectors.toList());

        // Lấy tất cả balance của các nhân viên trong năm
        List<LeaveBalanceEntity> balances = employeeIds.isEmpty()
                ? new ArrayList<>()
                : leaveBalanceRepository.findByYearAndEmployeeIdIn(year, employeeIds);

        // Group balances theo employeeId
        Map<Long, List<LeaveBalanceEntity>> balancesByEmployee = balances.stream()
                .collect(Collectors.groupingBy(LeaveBalanceEntity::getEmployeeId));

        // Tạo response cho từng nhân viên
        List<LeaveBalanceSummaryResponse> content = pagedEmployees.stream()
                .map(user -> {
                    List<LeaveBalanceEntity> userBalances = balancesByEmployee.getOrDefault(user.getId(),
                            new ArrayList<>());
                    List<LeaveBalanceResponse> balanceResponses = userBalances.stream()
                            .map(leaveMapper::toBalanceResponse)
                            .collect(Collectors.toList());

                    return LeaveBalanceSummaryResponse.builder()
                            .employeeId(user.getId())
                            .employeeName(user.getProfile() != null ? user.getProfile().getName() : user.getEmail())
                            .employeeCode(user.getEmployeeCode())
                            .balances(balanceResponses)
                            .build();
                })
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, totalElements);
    }

    @Override
    @Transactional
    public LeaveBalanceResponse updateEmployeeLeaveBalance(Long employeeId, UpdateLeaveBalanceRequest request) {
        // Kiểm tra nhân viên tồn tại (User có soft delete)
        if (!userRepository.existsByIdAndDeletedFalse(employeeId)) {
            throw new NotFoundException(
                    "Không tìm thấy nhân viên",
                    ErrorCode.USER_NOT_FOUND);
        }

        // Tìm balance hiện tại hoặc tạo mới
        LeaveBalanceEntity balance = leaveBalanceRepository
                .findByEmployeeIdAndYearAndLeaveType(employeeId, request.getYear(), request.getLeaveType())
                .orElse(null);

        if (balance == null) {
            // Tạo mới nếu chưa tồn tại
            balance = new LeaveBalanceEntity();
            balance.setEmployeeId(employeeId);
            balance.setYear(request.getYear());
            balance.setLeaveType(request.getLeaveType());
            balance.setUsedDays(0);
        }

        // Cập nhật totalDays và tính lại remainingDays
        balance.setTotalDays(request.getTotalDays());
        balance.setRemainingDays(Math.max(0, balance.getTotalDays() - balance.getUsedDays()));

        balance = leaveBalanceRepository.save(balance);

        log.info("Đã cập nhật số ngày phép {} cho nhân viên {} năm {}: totalDays={}",
                request.getLeaveType(), employeeId, request.getYear(), request.getTotalDays());

        return leaveMapper.toBalanceResponse(balance);
    }

    @Override
    @Transactional
    public BulkAllocateResponse bulkAllocateLeaveBalance(BulkAllocateLeaveRequest request) {
        List<Long> employeeIds = request.getEmployeeIds();

        // Nếu không chỉ định employeeIds, lấy tất cả nhân viên
        if (employeeIds == null || employeeIds.isEmpty()) {
            employeeIds = userRepository.findByDeletedFalse().stream()
                    .map(UserEntity::getId)
                    .collect(Collectors.toList());
        } else {
            // Validate các employeeIds có tồn tại không
            for (Long empId : employeeIds) {
                if (!userRepository.existsByIdAndDeletedFalse(empId)) {
                    throw new NotFoundException(
                            "Không tìm thấy nhân viên với ID: " + empId,
                            ErrorCode.USER_NOT_FOUND);
                }
            }
        }

        // Lấy tất cả balance hiện có của các nhân viên trong năm và loại phép
        List<LeaveBalanceEntity> existingBalances = leaveBalanceRepository
                .findByYearAndEmployeeIdIn(request.getYear(), employeeIds);

        // Map balance theo employeeId để tra cứu nhanh
        Map<Long, LeaveBalanceEntity> balanceMap = existingBalances.stream()
                .filter(b -> b.getLeaveType() == request.getLeaveType())
                .collect(Collectors.toMap(LeaveBalanceEntity::getEmployeeId, b -> b));

        int updatedCount = 0;

        // Cập nhật hoặc tạo mới balance cho từng nhân viên
        for (Long empId : employeeIds) {
            LeaveBalanceEntity balance = balanceMap.get(empId);

            if (balance == null) {
                // Tạo mới nếu chưa tồn tại
                balance = new LeaveBalanceEntity();
                balance.setEmployeeId(empId);
                balance.setYear(request.getYear());
                balance.setLeaveType(request.getLeaveType());
                balance.setUsedDays(0);
            }

            // Cập nhật totalDays và tính lại remainingDays (giữ nguyên usedDays)
            balance.setTotalDays(request.getTotalDays());
            balance.setRemainingDays(Math.max(0, balance.getTotalDays() - balance.getUsedDays()));

            leaveBalanceRepository.save(balance);
            updatedCount++;
        }

        log.info("Đã cấp phát hàng loạt {} ngày phép {} cho {} nhân viên năm {}",
                request.getTotalDays(), request.getLeaveType(), updatedCount, request.getYear());

        return BulkAllocateResponse.builder()
                .updatedCount(updatedCount)
                .build();
    }
}
