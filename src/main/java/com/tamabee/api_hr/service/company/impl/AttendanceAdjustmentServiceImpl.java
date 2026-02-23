package com.tamabee.api_hr.service.company.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.constants.NotificationCode;
import com.tamabee.api_hr.dto.request.attendance.AdjustAttendanceRequest;
import com.tamabee.api_hr.dto.request.attendance.BreakAdjustmentItem;
import com.tamabee.api_hr.dto.request.attendance.CreateAdjustmentRequest;
import com.tamabee.api_hr.dto.response.attendance.AdjustmentRequestResponse;
import com.tamabee.api_hr.entity.attendance.AdjustmentBreakItemEntity;
import com.tamabee.api_hr.entity.attendance.AttendanceAdjustmentRequestEntity;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.enums.AdjustmentRequestType;
import com.tamabee.api_hr.enums.AdjustmentStatus;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.BreakActionType;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.NotificationType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.AttendanceAdjustmentMapper;
import com.tamabee.api_hr.repository.attendance.AttendanceAdjustmentRequestRepository;
import com.tamabee.api_hr.repository.attendance.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.attendance.BreakRecordRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceAdjustmentService;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceService;
import com.tamabee.api_hr.service.core.interfaces.INotificationService;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation quản lý yêu cầu điều chỉnh chấm công.
 * Hỗ trợ điều chỉnh nhiều break records trong 1 request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceAdjustmentServiceImpl implements IAttendanceAdjustmentService {

    private final AttendanceAdjustmentRequestRepository adjustmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final UserRepository userRepository;
    private final IAttendanceService attendanceService;
    private final AttendanceAdjustmentMapper adjustmentMapper;
    private final INotificationService notificationService;

    // ==================== Employee Operations ====================

    @Override
    @Transactional
    public AdjustmentRequestResponse createAdjustmentRequest(
            Long employeeId, CreateAdjustmentRequest request) {

        // Xác định loại yêu cầu (mặc định là ADJUST)
        AdjustmentRequestType requestType = request.getRequestType() != null
                ? request.getRequestType()
                : AdjustmentRequestType.ADJUST;

        // Validate: phải có attendanceRecordId hoặc workDate
        if (request.getAttendanceRecordId() == null && request.getWorkDate() == null) {
            throw new BadRequestException(
                    "Phải cung cấp attendanceRecordId hoặc workDate",
                    ErrorCode.INVALID_REQUEST);
        }

        AttendanceRecordEntity attendanceRecord = null;
        LocalDate workDate = request.getWorkDate();

        // Nếu có attendanceRecordId, kiểm tra bản ghi chấm công
        if (request.getAttendanceRecordId() != null) {
            attendanceRecord = findAttendanceRecord(request.getAttendanceRecordId());

            // Kiểm tra bản ghi thuộc về nhân viên này
            if (!attendanceRecord.getEmployeeId().equals(employeeId)) {
                throw new BadRequestException(
                        "Bản ghi chấm công không thuộc về nhân viên này",
                        ErrorCode.ACCESS_DENIED);
            }

            // Kiểm tra đã có yêu cầu đang chờ duyệt chưa
            if (adjustmentRepository.existsPendingByAttendanceRecordId(request.getAttendanceRecordId())) {
                throw new ConflictException(
                        "Đã có yêu cầu điều chỉnh đang chờ duyệt cho bản ghi này",
                        ErrorCode.ADJUSTMENT_PENDING_EXISTS);
            }

            workDate = attendanceRecord.getWorkDate();
        } else {
            // Không có attendanceRecordId - kiểm tra đã có yêu cầu pending cho ngày này chưa
            if (adjustmentRepository.existsPendingByEmployeeIdAndWorkDate(employeeId, workDate)) {
                throw new ConflictException(
                        "Đã có yêu cầu điều chỉnh đang chờ duyệt cho ngày này",
                        ErrorCode.ADJUSTMENT_PENDING_EXISTS);
            }
        }

        // Validate theo loại yêu cầu
        switch (requestType) {
            case DELETE_RECORD:
                // Xóa record: phải có attendanceRecordId
                if (request.getAttendanceRecordId() == null) {
                    throw new BadRequestException(
                            "Phải có attendanceRecordId để xóa bản ghi chấm công",
                            ErrorCode.INVALID_REQUEST);
                }
                break;

            case ADJUST:
            default:
                // Điều chỉnh: phải có ít nhất một thay đổi (check in/out hoặc break items)
                boolean hasCheckInOutChanges = request.getRequestedCheckIn() != null
                        || request.getRequestedCheckOut() != null;
                boolean hasBreakItems = request.getBreakItems() != null && !request.getBreakItems().isEmpty();

                if (!hasCheckInOutChanges && !hasBreakItems) {
                    throw new BadRequestException(
                            "Phải thay đổi ít nhất một thời gian check-in, check-out hoặc break",
                            ErrorCode.ADJUSTMENT_NO_CHANGES);
                }

                // Validate thời gian điều chỉnh
                if (attendanceRecord != null) {
                    validateAdjustmentTimes(request, attendanceRecord);
                } else {
                    validateAdjustmentTimesWithoutRecord(request);
                }
                break;
        }

        // Tạo yêu cầu điều chỉnh
        AttendanceAdjustmentRequestEntity entity = new AttendanceAdjustmentRequestEntity();
        entity.setEmployeeId(employeeId);
        entity.setRequestType(requestType);
        entity.setAttendanceRecordId(request.getAttendanceRecordId());
        entity.setWorkDate(workDate);
        entity.setAssignedTo(request.getAssignedTo());
        entity.setOriginalCheckIn(attendanceRecord != null ? attendanceRecord.getOriginalCheckIn() : null);
        entity.setOriginalCheckOut(attendanceRecord != null ? attendanceRecord.getOriginalCheckOut() : null);
        entity.setRequestedCheckIn(request.getRequestedCheckIn());
        entity.setRequestedCheckOut(request.getRequestedCheckOut());
        entity.setReason(request.getReason());
        entity.setStatus(AdjustmentStatus.PENDING);

        // Xử lý break items
        if (request.getBreakItems() != null && !request.getBreakItems().isEmpty()) {
            int newBreakNumber = 1;
            for (BreakAdjustmentItem item : request.getBreakItems()) {
                BreakActionType actionType = item.getActionType() != null ? item.getActionType() : BreakActionType.ADJUST;
                
                if (actionType == BreakActionType.CREATE) {
                    // Tạo mới break - không cần breakRecordId
                    if (item.getRequestedBreakStart() == null || item.getRequestedBreakEnd() == null) {
                        throw new BadRequestException(
                                "Phải có thời gian bắt đầu và kết thúc khi tạo mới giờ giải lao",
                                ErrorCode.INVALID_REQUEST);
                    }
                    
                    AdjustmentBreakItemEntity breakItemEntity = AdjustmentBreakItemEntity.builder()
                            .breakRecordId(null)
                            .breakNumber(newBreakNumber++)
                            .actionType(BreakActionType.CREATE)
                            .originalBreakStart(null)
                            .originalBreakEnd(null)
                            .requestedBreakStart(item.getRequestedBreakStart())
                            .requestedBreakEnd(item.getRequestedBreakEnd())
                            .build();
                    entity.addBreakItem(breakItemEntity);
                } else {
                    // ADJUST hoặc DELETE - cần breakRecordId
                    if (item.getBreakRecordId() == null) {
                        throw new BadRequestException(
                                "Phải có breakRecordId khi điều chỉnh hoặc xóa giờ giải lao",
                                ErrorCode.INVALID_REQUEST);
                    }
                    
                    BreakRecordEntity breakRecord = breakRecordRepository.findById(item.getBreakRecordId())
                            .orElseThrow(() -> new NotFoundException(
                                    "Không tìm thấy bản ghi giờ giải lao",
                                    ErrorCode.BREAK_RECORD_NOT_FOUND));

                    if (request.getAttendanceRecordId() != null
                            && !breakRecord.getAttendanceRecordId().equals(request.getAttendanceRecordId())) {
                        throw new BadRequestException(
                                "Bản ghi giờ giải lao không thuộc về bản ghi chấm công này",
                                ErrorCode.INVALID_BREAK_RECORD);
                    }

                    AdjustmentBreakItemEntity breakItemEntity = AdjustmentBreakItemEntity.builder()
                            .breakRecordId(item.getBreakRecordId())
                            .breakNumber(breakRecord.getBreakNumber())
                            .actionType(actionType)
                            .originalBreakStart(breakRecord.getBreakStart())
                            .originalBreakEnd(breakRecord.getBreakEnd())
                            .requestedBreakStart(item.getRequestedBreakStart())
                            .requestedBreakEnd(item.getRequestedBreakEnd())
                            .build();
                    entity.addBreakItem(breakItemEntity);
                }
            }
        }

        entity = adjustmentRepository.save(entity);
        log.info("Nhân viên {} đã tạo yêu cầu {} loại {} cho ngày {} với {} break items",
                employeeId, entity.getId(), requestType, workDate,
                entity.getBreakItems() != null ? entity.getBreakItems().size() : 0);

        // Gửi thông báo cho người được chỉ định duyệt
        notifyAssignedApprover(employeeId, request.getAssignedTo(), workDate, entity.getId());

        return adjustmentMapper.toResponse(
                entity,
                getEmployeeName(employeeId),
                null,
                attendanceRecord);
    }

    // ==================== Manager Operations ====================

    @Override
    @Transactional
    public AdjustmentRequestResponse approveAdjustment(Long requestId, Long managerId, String comment) {
        AttendanceAdjustmentRequestEntity entity = findAdjustmentRequest(requestId);

        // Kiểm tra trạng thái
        if (entity.getStatus() != AdjustmentStatus.PENDING) {
            throw new ConflictException(
                    "Yêu cầu điều chỉnh đã được xử lý",
                    ErrorCode.ADJUSTMENT_ALREADY_PROCESSED);
        }

        // Cập nhật trạng thái
        entity.setStatus(AdjustmentStatus.APPROVED);
        entity.setApprovedBy(managerId);
        entity.setApprovedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        entity.setApproverComment(comment);

        entity = adjustmentRepository.save(entity);

        // Xử lý theo loại yêu cầu
        AdjustmentRequestType requestType = entity.getRequestType() != null
                ? entity.getRequestType()
                : AdjustmentRequestType.ADJUST;

        switch (requestType) {
            case DELETE_RECORD:
                // Xóa toàn bộ attendance record và các break records liên quan
                if (entity.getAttendanceRecordId() != null) {
                    deleteAttendanceRecord(entity.getAttendanceRecordId());
                }
                log.info("Manager {} đã phê duyệt xóa bản ghi chấm công {}",
                        managerId, entity.getAttendanceRecordId());
                break;

            case ADJUST:
            default:
                if (entity.getAttendanceRecordId() != null) {
                    // Cập nhật bản ghi chấm công với thời gian mới
                    updateAttendanceRecord(entity);
                } else {
                    // Tạo mới attendance record nếu chưa có
                    Long newRecordId = createAttendanceRecord(entity);
                    entity.setAttendanceRecordId(newRecordId);
                    entity = adjustmentRepository.save(entity);
                }
                log.info("Manager {} đã phê duyệt yêu cầu điều chỉnh {}", managerId, requestId);
                break;
        }

        // Lấy attendance record để trả về (nếu không bị xóa)
        AttendanceRecordEntity attendanceRecord = null;
        if (entity.getAttendanceRecordId() != null && requestType != AdjustmentRequestType.DELETE_RECORD) {
            attendanceRecord = attendanceRecordRepository
                    .findById(entity.getAttendanceRecordId())
                    .orElse(null);
        }

        // Gửi thông báo cho nhân viên đã gửi yêu cầu
        notifyEmployeeOnAdjustmentApproved(entity.getId(), entity.getEmployeeId(), entity.getWorkDate());

        return adjustmentMapper.toResponse(
                entity,
                getEmployeeName(entity.getEmployeeId()),
                getEmployeeName(managerId),
                attendanceRecord);
    }

    @Override
    @Transactional
    public AdjustmentRequestResponse rejectAdjustment(Long requestId, Long managerId, String reason) {
        // Kiểm tra lý do từ chối
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException(
                    "Lý do từ chối không được để trống",
                    ErrorCode.REJECTION_REASON_REQUIRED);
        }

        AttendanceAdjustmentRequestEntity entity = findAdjustmentRequest(requestId);

        // Kiểm tra trạng thái
        if (entity.getStatus() != AdjustmentStatus.PENDING) {
            throw new ConflictException(
                    "Yêu cầu điều chỉnh đã được xử lý",
                    ErrorCode.ADJUSTMENT_ALREADY_PROCESSED);
        }

        // Cập nhật trạng thái
        entity.setStatus(AdjustmentStatus.REJECTED);
        entity.setApprovedBy(managerId);
        entity.setApprovedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        entity.setRejectionReason(reason);

        entity = adjustmentRepository.save(entity);
        log.info("Manager {} đã từ chối yêu cầu điều chỉnh {} với lý do: {}",
                managerId, requestId, reason);

        // Gửi thông báo cho nhân viên đã gửi yêu cầu
        notifyEmployeeOnAdjustmentRejected(entity.getId(), entity.getEmployeeId(), entity.getWorkDate(), reason);

        // Lấy attendance record
        AttendanceRecordEntity attendanceRecord = null;
        if (entity.getAttendanceRecordId() != null) {
            attendanceRecord = attendanceRecordRepository
                    .findById(entity.getAttendanceRecordId())
                    .orElse(null);
        }

        return adjustmentMapper.toResponse(
                entity,
                getEmployeeName(entity.getEmployeeId()),
                getEmployeeName(managerId),
                attendanceRecord);
    }

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public AdjustmentRequestResponse getRequestById(Long requestId) {
        AttendanceAdjustmentRequestEntity entity = findAdjustmentRequest(requestId);

        AttendanceRecordEntity attendanceRecord = null;
        if (entity.getAttendanceRecordId() != null) {
            attendanceRecord = attendanceRecordRepository
                    .findById(entity.getAttendanceRecordId())
                    .orElse(null);
        }

        String approverName = entity.getApprovedBy() != null
                ? getEmployeeName(entity.getApprovedBy())
                : null;

        String assignedToName = entity.getAssignedTo() != null
                ? getEmployeeName(entity.getAssignedTo())
                : null;

        LocalDate workDate = entity.getWorkDate() != null
                ? entity.getWorkDate()
                : (attendanceRecord != null ? attendanceRecord.getWorkDate() : null);

        // Lấy tất cả break records của ngày để người duyệt có cái nhìn đầy đủ
        List<BreakRecordEntity> allBreakRecords = List.of();
        if (attendanceRecord != null) {
            allBreakRecords = breakRecordRepository.findByAttendanceRecordId(attendanceRecord.getId());
        }

        return adjustmentMapper.toResponse(
                entity,
                getEmployeeName(entity.getEmployeeId()),
                approverName,
                assignedToName,
                workDate,
                allBreakRecords);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdjustmentRequestResponse> getPendingRequests(Long userId, boolean isAdmin,
            Pageable pageable) {
        Page<AttendanceAdjustmentRequestEntity> requests;
        if (isAdmin) {
            requests = adjustmentRepository.findPending(pageable);
        } else {
            requests = adjustmentRepository.findPendingByAssignedTo(userId, pageable);
        }
        return requests.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdjustmentRequestResponse> getAllRequests(Long userId, boolean isAdmin,
            Pageable pageable) {
        Page<AttendanceAdjustmentRequestEntity> requests;
        if (isAdmin) {
            requests = adjustmentRepository.findAllPaged(pageable);
        } else {
            requests = adjustmentRepository.findByAssignedTo(userId, pageable);
        }
        return requests.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdjustmentRequestResponse> getEmployeeRequests(Long employeeId, Pageable pageable) {
        return adjustmentRepository.findByEmployeeId(employeeId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdjustmentRequestResponse> getAdjustmentHistoryByAttendanceRecord(Long attendanceRecordId) {
        return adjustmentRepository.findByAttendanceRecordId(attendanceRecordId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdjustmentRequestResponse> getEmployeeRequestsByWorkDate(Long employeeId, LocalDate workDate) {
        return adjustmentRepository.findByEmployeeIdAndWorkDate(employeeId, workDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelAdjustmentRequest(Long requestId, Long employeeId) {
        AttendanceAdjustmentRequestEntity entity = findAdjustmentRequest(requestId);

        // Kiểm tra yêu cầu thuộc về nhân viên này
        if (!entity.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException(
                    "Không có quyền thu hồi yêu cầu này",
                    ErrorCode.ACCESS_DENIED);
        }

        // Kiểm tra trạng thái phải là PENDING
        if (entity.getStatus() != AdjustmentStatus.PENDING) {
            throw new ConflictException(
                    "Chỉ có thể thu hồi yêu cầu đang chờ duyệt",
                    ErrorCode.ADJUSTMENT_ALREADY_PROCESSED);
        }

        // Hard delete để có thể tạo yêu cầu mới
        adjustmentRepository.delete(entity);

        log.info("Nhân viên {} đã thu hồi yêu cầu điều chỉnh {}", employeeId, requestId);
    }

    // ==================== Private Helper Methods ====================

    private AttendanceAdjustmentRequestEntity findAdjustmentRequest(Long requestId) {
        return adjustmentRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy yêu cầu điều chỉnh",
                        ErrorCode.ADJUSTMENT_NOT_FOUND));
    }

    private AttendanceRecordEntity findAttendanceRecord(Long recordId) {
        return attendanceRecordRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy bản ghi chấm công",
                        ErrorCode.ATTENDANCE_RECORD_NOT_FOUND));
    }

    private String getEmployeeName(Long userId) {
        return userRepository.findWithProfileByIdAndDeletedFalse(userId)
                .map(user -> user.getProfile() != null ? user.getProfile().getName() : user.getEmail())
                .orElse("Unknown");
    }

    private void validateAdjustmentTimes(CreateAdjustmentRequest request, AttendanceRecordEntity record) {
        LocalDateTime checkIn = request.getRequestedCheckIn() != null
                ? request.getRequestedCheckIn()
                : record.getOriginalCheckIn();
        LocalDateTime checkOut = request.getRequestedCheckOut() != null
                ? request.getRequestedCheckOut()
                : record.getOriginalCheckOut();

        if (checkIn != null && checkOut != null) {
            // Hỗ trợ ca qua đêm: nếu checkOut <= checkIn, coi như checkOut là ngày hôm sau
            LocalDateTime effectiveCheckOut = checkOut;
            if (!checkOut.isAfter(checkIn)) {
                effectiveCheckOut = checkOut.plusDays(1);
            }

            // Validate break times nằm trong khoảng checkIn - effectiveCheckOut
            if (request.getBreakItems() != null) {
                validateBreakTimesInRange(request.getBreakItems(), checkIn, effectiveCheckOut);
            }
        }
    }

    private void validateAdjustmentTimesWithoutRecord(CreateAdjustmentRequest request) {
        LocalDateTime checkIn = request.getRequestedCheckIn();
        LocalDateTime checkOut = request.getRequestedCheckOut();

        if (checkIn != null && checkOut != null) {
            // Hỗ trợ ca qua đêm: nếu checkOut <= checkIn, coi như checkOut là ngày hôm sau
            LocalDateTime effectiveCheckOut = checkOut;
            if (!checkOut.isAfter(checkIn)) {
                effectiveCheckOut = checkOut.plusDays(1);
            }

            // Validate break times nằm trong khoảng checkIn - effectiveCheckOut
            if (request.getBreakItems() != null) {
                validateBreakTimesInRange(request.getBreakItems(), checkIn, effectiveCheckOut);
            }
        }
    }

    /**
     * Validate thời gian giải lao phải nằm trong khoảng checkIn - checkOut.
     * checkOut đã được adjust cho ca qua đêm (nếu cần) trước khi gọi method này.
     */
    private void validateBreakTimesInRange(List<BreakAdjustmentItem> breakItems, 
            LocalDateTime checkIn, LocalDateTime checkOut) {
        for (BreakAdjustmentItem item : breakItems) {
            // Bỏ qua DELETE action
            if (item.getActionType() == BreakActionType.DELETE) {
                continue;
            }

            LocalDateTime breakStart = item.getRequestedBreakStart();
            LocalDateTime breakEnd = item.getRequestedBreakEnd();

            // Adjust break times cho ca qua đêm: nếu break time trước checkIn, coi như ngày hôm sau
            if (breakStart != null && breakStart.isBefore(checkIn)) {
                breakStart = breakStart.plusDays(1);
            }
            if (breakEnd != null && breakEnd.isBefore(checkIn)) {
                breakEnd = breakEnd.plusDays(1);
            }

            // Validate break start
            if (breakStart != null) {
                if (breakStart.isBefore(checkIn) || breakStart.isAfter(checkOut)) {
                    throw new BadRequestException(
                            "Thời gian bắt đầu giải lao phải nằm trong khoảng chấm công",
                            ErrorCode.BREAK_TIME_OUT_OF_RANGE);
                }
            }

            // Validate break end
            if (breakEnd != null) {
                if (breakEnd.isBefore(checkIn) || breakEnd.isAfter(checkOut)) {
                    throw new BadRequestException(
                            "Thời gian kết thúc giải lao phải nằm trong khoảng chấm công",
                            ErrorCode.BREAK_TIME_OUT_OF_RANGE);
                }
            }

            // Validate break start < break end
            if (breakStart != null && breakEnd != null && !breakStart.isBefore(breakEnd)) {
                throw new BadRequestException(
                        "Thời gian bắt đầu giải lao phải trước thời gian kết thúc",
                        ErrorCode.INVALID_BREAK_TIME);
            }
        }
    }

    /**
     * Cập nhật bản ghi chấm công sau khi yêu cầu điều chỉnh được duyệt.
     * Xử lý cả check in/out và nhiều break items.
     */
    private void updateAttendanceRecord(AttendanceAdjustmentRequestEntity adjustment) {
        AdjustAttendanceRequest adjustRequest = new AdjustAttendanceRequest();

        // Cập nhật check in/out nếu có
        if (adjustment.getRequestedCheckIn() != null) {
            adjustRequest.setCheckInTime(adjustment.getRequestedCheckIn());
        }
        if (adjustment.getRequestedCheckOut() != null) {
            adjustRequest.setCheckOutTime(adjustment.getRequestedCheckOut());
        }

        // Xử lý break items
        List<AdjustmentBreakItemEntity> breakItems = adjustment.getBreakItems();
        if (breakItems != null && !breakItems.isEmpty()) {
            List<AdjustAttendanceRequest.BreakAdjustment> breakAdjustments = new java.util.ArrayList<>();

            for (AdjustmentBreakItemEntity item : breakItems) {
                if (item.getActionType() == BreakActionType.DELETE) {
                    // Xóa break record
                    deleteBreakRecord(item.getBreakRecordId(), adjustment.getAttendanceRecordId());
                } else {
                    // Điều chỉnh break record
                    if (item.getRequestedBreakStart() != null || item.getRequestedBreakEnd() != null) {
                        AdjustAttendanceRequest.BreakAdjustment breakAdj = AdjustAttendanceRequest.BreakAdjustment
                                .builder()
                                .breakRecordId(item.getBreakRecordId())
                                .breakStartTime(item.getRequestedBreakStart())
                                .breakEndTime(item.getRequestedBreakEnd())
                                .build();
                        breakAdjustments.add(breakAdj);
                    }
                }
            }

            if (!breakAdjustments.isEmpty()) {
                adjustRequest.setBreakAdjustments(breakAdjustments);
            }
        }

        adjustRequest.setReason(
                "Điều chỉnh theo yêu cầu #" + adjustment.getId() + ": " + adjustment.getReason());

        attendanceService.adjustAttendance(
                adjustment.getAttendanceRecordId(),
                adjustment.getApprovedBy(),
                adjustRequest,
                false);
    }

    /**
     * Tạo mới attendance record khi duyệt yêu cầu điều chỉnh cho ngày chưa có record.
     * Bao gồm cả tạo break records nếu có trong request.
     * 
     * @return ID của attendance record mới tạo
     */
    private Long createAttendanceRecord(AttendanceAdjustmentRequestEntity adjustment) {
        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setEmployeeId(adjustment.getEmployeeId());
        entity.setWorkDate(adjustment.getWorkDate());
        entity.setOriginalCheckIn(adjustment.getRequestedCheckIn());
        entity.setOriginalCheckOut(adjustment.getRequestedCheckOut());
        entity.setRoundedCheckIn(adjustment.getRequestedCheckIn());
        entity.setRoundedCheckOut(adjustment.getRequestedCheckOut());
        entity.setStatus(AttendanceStatus.PRESENT);
        entity.setAdjustmentReason("Tạo từ yêu cầu điều chỉnh #" + adjustment.getId() + ": " + adjustment.getReason());
        entity.setAdjustedBy(adjustment.getApprovedBy());
        entity.setAdjustedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));

        entity = attendanceRecordRepository.save(entity);

        // Tạo break records nếu có trong request
        int totalBreakMinutes = 0;
        List<AdjustmentBreakItemEntity> breakItems = adjustment.getBreakItems();
        if (breakItems != null && !breakItems.isEmpty()) {
            int breakNumber = 1;
            for (AdjustmentBreakItemEntity item : breakItems) {
                if (item.getActionType() == BreakActionType.CREATE 
                        && item.getRequestedBreakStart() != null 
                        && item.getRequestedBreakEnd() != null) {
                    BreakRecordEntity breakRecord = new BreakRecordEntity();
                    breakRecord.setAttendanceRecordId(entity.getId());
                    breakRecord.setEmployeeId(adjustment.getEmployeeId());
                    breakRecord.setWorkDate(adjustment.getWorkDate());
                    breakRecord.setBreakNumber(breakNumber++);
                    breakRecord.setBreakStart(item.getRequestedBreakStart());
                    breakRecord.setBreakEnd(item.getRequestedBreakEnd());
                    
                    // Tính break minutes
                    int breakMinutes = (int) java.time.Duration.between(
                            item.getRequestedBreakStart(),
                            item.getRequestedBreakEnd()).toMinutes();
                    breakRecord.setActualBreakMinutes(breakMinutes);
                    breakRecord.setEffectiveBreakMinutes(breakMinutes);
                    
                    breakRecordRepository.save(breakRecord);
                    totalBreakMinutes += breakMinutes;
                    
                    log.info("Đã tạo break record #{} cho attendance record {}: {} - {}",
                            breakRecord.getBreakNumber(), entity.getId(),
                            item.getRequestedBreakStart(), item.getRequestedBreakEnd());
                }
            }
        }

        // Cập nhật break minutes và working minutes
        entity.setTotalBreakMinutes(totalBreakMinutes);
        entity.setEffectiveBreakMinutes(totalBreakMinutes);
        
        // Tính working minutes nếu có cả check in và check out
        if (adjustment.getRequestedCheckIn() != null && adjustment.getRequestedCheckOut() != null) {
            long totalMinutes = java.time.Duration.between(
                    adjustment.getRequestedCheckIn(),
                    adjustment.getRequestedCheckOut()).toMinutes();
            int workingMinutes = (int) Math.max(0, totalMinutes - totalBreakMinutes);
            entity.setWorkingMinutes(workingMinutes);
        }
        
        entity = attendanceRecordRepository.save(entity);
        
        log.info("Đã tạo attendance record {} cho nhân viên {} ngày {} từ yêu cầu điều chỉnh #{}, break: {} phút",
                entity.getId(), adjustment.getEmployeeId(), adjustment.getWorkDate(), 
                adjustment.getId(), totalBreakMinutes);

        return entity.getId();
    }

    private void deleteAttendanceRecord(Long attendanceRecordId) {
        // Xóa tất cả break records trước
        List<BreakRecordEntity> breakRecords = breakRecordRepository
                .findByAttendanceRecordId(attendanceRecordId);
        if (!breakRecords.isEmpty()) {
            breakRecordRepository.deleteAll(breakRecords);
            log.info("Đã xóa {} break records của attendance record {}",
                    breakRecords.size(), attendanceRecordId);
        }

        // Xóa attendance record
        attendanceRecordRepository.deleteById(attendanceRecordId);
        log.info("Đã xóa attendance record {}", attendanceRecordId);
    }

    private void deleteBreakRecord(Long breakRecordId, Long attendanceRecordId) {
        breakRecordRepository.deleteById(breakRecordId);
        log.info("Đã xóa break record {}", breakRecordId);

        // Cập nhật tổng break minutes trong attendance record
        if (attendanceRecordId != null) {
            AttendanceRecordEntity attendance = attendanceRecordRepository
                    .findById(attendanceRecordId)
                    .orElse(null);
            if (attendance != null) {
                List<BreakRecordEntity> remainingBreaks = breakRecordRepository
                        .findByAttendanceRecordId(attendanceRecordId);
                int totalBreakMinutes = remainingBreaks.stream()
                        .filter(b -> b.getActualBreakMinutes() != null)
                        .mapToInt(BreakRecordEntity::getActualBreakMinutes)
                        .sum();
                attendance.setTotalBreakMinutes(totalBreakMinutes);
                attendance.setEffectiveBreakMinutes(totalBreakMinutes);
                attendanceRecordRepository.save(attendance);
                log.info("Đã cập nhật tổng break minutes của attendance record {} thành {} phút",
                        attendanceRecordId, totalBreakMinutes);
            }
        }
    }

    private AdjustmentRequestResponse mapToResponse(AttendanceAdjustmentRequestEntity entity) {
        AttendanceRecordEntity attendanceRecord = null;
        if (entity.getAttendanceRecordId() != null) {
            attendanceRecord = attendanceRecordRepository
                    .findById(entity.getAttendanceRecordId())
                    .orElse(null);
        }

        String approverName = entity.getApprovedBy() != null
                ? getEmployeeName(entity.getApprovedBy())
                : null;

        String assignedToName = entity.getAssignedTo() != null
                ? getEmployeeName(entity.getAssignedTo())
                : null;

        LocalDate workDate = entity.getWorkDate() != null
                ? entity.getWorkDate()
                : (attendanceRecord != null ? attendanceRecord.getWorkDate() : null);

        return adjustmentMapper.toResponse(
                entity,
                getEmployeeName(entity.getEmployeeId()),
                approverName,
                assignedToName,
                workDate);
    }

    // ==================== Notification Helper Methods ====================

    /**
     * Gửi thông báo cho người được chỉ định duyệt khi có yêu cầu điều chỉnh chấm công mới
     */
    private void notifyAssignedApprover(Long employeeId, Long assignedTo, LocalDate workDate, Long adjustmentId) {
        try {
            if (assignedTo == null) {
                log.warn("Không có người được chỉ định duyệt cho yêu cầu điều chỉnh {}", adjustmentId);
                return;
            }

            // Tạo params cho thông báo
            Map<String, Object> params = new HashMap<>();
            params.put("employeeName", getEmployeeName(employeeId));
            params.put("date", workDate != null ? workDate.toString() : "");

            // Gửi thông báo cho người được chỉ định
            notificationService.createNotification(
                    assignedTo,
                    NotificationCode.ADJUSTMENT_SUBMITTED,
                    params,
                    "/dashboard/adjustments?id=" + adjustmentId,
                    NotificationType.ADJUSTMENT);

            log.info("Đã gửi thông báo điều chỉnh chấm công mới cho người duyệt {}", assignedTo);
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo điều chỉnh chấm công mới: {}", e.getMessage());
        }
    }

    /**
     * Gửi thông báo cho nhân viên khi yêu cầu điều chỉnh chấm công được duyệt
     */
    private void notifyEmployeeOnAdjustmentApproved(Long adjustmentId, Long employeeId, LocalDate workDate) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("date", workDate != null ? workDate.toString() : "");

            notificationService.createNotification(
                    employeeId,
                    NotificationCode.ADJUSTMENT_APPROVED,
                    params,
                    "/me/adjustments?id=" + adjustmentId,
                    NotificationType.ADJUSTMENT);

            log.info("Đã gửi thông báo duyệt điều chỉnh chấm công cho nhân viên {}", employeeId);
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo duyệt điều chỉnh chấm công: {}", e.getMessage());
        }
    }

    /**
     * Gửi thông báo cho nhân viên khi yêu cầu điều chỉnh chấm công bị từ chối
     */
    private void notifyEmployeeOnAdjustmentRejected(Long adjustmentId, Long employeeId, LocalDate workDate, String reason) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("date", workDate != null ? workDate.toString() : "");
            params.put("reason", reason != null ? reason : "");

            notificationService.createNotification(
                    employeeId,
                    NotificationCode.ADJUSTMENT_REJECTED,
                    params,
                    "/me/adjustments?id=" + adjustmentId,
                    NotificationType.ADJUSTMENT);

            log.info("Đã gửi thông báo từ chối điều chỉnh chấm công cho nhân viên {}", employeeId);
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo từ chối điều chỉnh chấm công: {}", e.getMessage());
        }
    }
}
