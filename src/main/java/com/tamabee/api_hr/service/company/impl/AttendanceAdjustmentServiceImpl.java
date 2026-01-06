package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.request.AdjustAttendanceRequest;
import com.tamabee.api_hr.dto.request.CreateAdjustmentRequest;
import com.tamabee.api_hr.dto.response.AdjustmentRequestResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceAdjustmentRequestEntity;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.enums.AdjustmentStatus;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.AttendanceAdjustmentMapper;
import com.tamabee.api_hr.repository.AttendanceAdjustmentRequestRepository;
import com.tamabee.api_hr.repository.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.BreakRecordRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IAttendanceAdjustmentService;
import com.tamabee.api_hr.service.company.IAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation quản lý yêu cầu điều chỉnh chấm công.
 * Xử lý tạo yêu cầu, phê duyệt/từ chối, và cập nhật bản ghi chấm công khi được
 * duyệt.
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

        // ==================== Employee Operations ====================

        @Override
        @Transactional
        public AdjustmentRequestResponse createAdjustmentRequest(
                        Long employeeId, Long companyId, CreateAdjustmentRequest request) {

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
                        // Không có attendanceRecordId - kiểm tra đã có yêu cầu pending cho ngày này
                        // chưa
                        if (adjustmentRepository.existsPendingByEmployeeIdAndWorkDate(employeeId, workDate)) {
                                throw new ConflictException(
                                                "Đã có yêu cầu điều chỉnh đang chờ duyệt cho ngày này",
                                                ErrorCode.ADJUSTMENT_PENDING_EXISTS);
                        }
                }

                // Kiểm tra phải có ít nhất một thay đổi
                if (request.getRequestedCheckIn() == null && request.getRequestedCheckOut() == null
                                && request.getRequestedBreakStart() == null && request.getRequestedBreakEnd() == null) {
                        throw new BadRequestException(
                                        "Phải thay đổi ít nhất một thời gian check-in, check-out, break-in hoặc break-out",
                                        ErrorCode.ADJUSTMENT_NO_CHANGES);
                }

                // Validate breakRecordId khi có break fields (chỉ khi có attendanceRecord)
                boolean hasBreakFields = request.getRequestedBreakStart() != null
                                || request.getRequestedBreakEnd() != null;
                if (hasBreakFields && request.getBreakRecordId() == null && attendanceRecord != null) {
                        throw new BadRequestException(
                                        "Phải chỉ định breakRecordId khi điều chỉnh giờ giải lao",
                                        ErrorCode.BREAK_RECORD_ID_REQUIRED);
                }

                // Validate breakRecord thuộc về attendanceRecord nếu có breakRecordId
                // BreakRecord không có soft delete
                BreakRecordEntity breakRecord = null;
                if (request.getBreakRecordId() != null) {
                        breakRecord = breakRecordRepository.findById(request.getBreakRecordId())
                                        .orElseThrow(() -> new NotFoundException(
                                                        "Không tìm thấy bản ghi giờ giải lao",
                                                        ErrorCode.BREAK_RECORD_NOT_FOUND));

                        if (!breakRecord.getAttendanceRecordId().equals(request.getAttendanceRecordId())) {
                                throw new BadRequestException(
                                                "Bản ghi giờ giải lao không thuộc về bản ghi chấm công này",
                                                ErrorCode.INVALID_BREAK_RECORD);
                        }
                }

                // Validate thời gian điều chỉnh (chỉ khi có attendanceRecord)
                if (attendanceRecord != null) {
                        validateAdjustmentTimes(request, attendanceRecord);
                } else {
                        // Validate thời gian khi không có attendanceRecord
                        validateAdjustmentTimesWithoutRecord(request);
                }

                // Tạo yêu cầu điều chỉnh
                AttendanceAdjustmentRequestEntity entity = new AttendanceAdjustmentRequestEntity();
                entity.setEmployeeId(employeeId);
                entity.setCompanyId(companyId);
                entity.setAttendanceRecordId(request.getAttendanceRecordId());
                entity.setWorkDate(workDate);
                entity.setBreakRecordId(request.getBreakRecordId());
                entity.setAssignedTo(request.getAssignedTo());
                entity.setOriginalCheckIn(attendanceRecord != null ? attendanceRecord.getOriginalCheckIn() : null);
                entity.setOriginalCheckOut(attendanceRecord != null ? attendanceRecord.getOriginalCheckOut() : null);
                entity.setOriginalBreakStart(breakRecord != null ? breakRecord.getBreakStart() : null);
                entity.setOriginalBreakEnd(breakRecord != null ? breakRecord.getBreakEnd() : null);
                entity.setRequestedCheckIn(request.getRequestedCheckIn());
                entity.setRequestedCheckOut(request.getRequestedCheckOut());
                entity.setRequestedBreakStart(request.getRequestedBreakStart());
                entity.setRequestedBreakEnd(request.getRequestedBreakEnd());
                entity.setReason(request.getReason());
                entity.setStatus(AdjustmentStatus.PENDING);

                entity = adjustmentRepository.save(entity);
                log.info("Nhân viên {} đã tạo yêu cầu điều chỉnh {} cho ngày {}",
                                employeeId, entity.getId(), workDate);

                return adjustmentMapper.toResponse(
                                entity,
                                getEmployeeName(employeeId),
                                null,
                                attendanceRecord,
                                breakRecord);
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
                entity.setApprovedAt(LocalDateTime.now());
                entity.setApproverComment(comment);

                entity = adjustmentRepository.save(entity);

                // Cập nhật bản ghi chấm công với thời gian mới (chỉ khi có attendanceRecordId)
                if (entity.getAttendanceRecordId() != null) {
                        updateAttendanceRecord(entity);
                }

                log.info("Manager {} đã phê duyệt yêu cầu điều chỉnh {}", managerId, requestId);

                // AttendanceRecord không có soft delete
                AttendanceRecordEntity attendanceRecord = null;
                if (entity.getAttendanceRecordId() != null) {
                        attendanceRecord = attendanceRecordRepository
                                        .findById(entity.getAttendanceRecordId())
                                        .orElse(null);
                }

                // Lấy break record nếu có breakRecordId
                // BreakRecord không có soft delete
                BreakRecordEntity breakRecord = null;
                if (entity.getBreakRecordId() != null) {
                        breakRecord = breakRecordRepository.findById(entity.getBreakRecordId())
                                        .orElse(null);
                }

                return adjustmentMapper.toResponse(
                                entity,
                                getEmployeeName(entity.getEmployeeId()),
                                getEmployeeName(managerId),
                                attendanceRecord,
                                breakRecord);
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
                entity.setApprovedAt(LocalDateTime.now());
                entity.setRejectionReason(reason);

                entity = adjustmentRepository.save(entity);
                log.info("Manager {} đã từ chối yêu cầu điều chỉnh {} với lý do: {}",
                                managerId, requestId, reason);

                // AttendanceRecord không có soft delete
                AttendanceRecordEntity attendanceRecord = null;
                if (entity.getAttendanceRecordId() != null) {
                        attendanceRecord = attendanceRecordRepository
                                        .findById(entity.getAttendanceRecordId())
                                        .orElse(null);
                }

                // Lấy break record nếu có breakRecordId
                // BreakRecord không có soft delete
                BreakRecordEntity breakRecord = null;
                if (entity.getBreakRecordId() != null) {
                        breakRecord = breakRecordRepository.findById(entity.getBreakRecordId())
                                        .orElse(null);
                }

                return adjustmentMapper.toResponse(
                                entity,
                                getEmployeeName(entity.getEmployeeId()),
                                getEmployeeName(managerId),
                                attendanceRecord,
                                breakRecord);
        }

        // ==================== Query Operations ====================

        @Override
        @Transactional(readOnly = true)
        public AdjustmentRequestResponse getRequestById(Long requestId) {
                AttendanceAdjustmentRequestEntity entity = findAdjustmentRequest(requestId);

                // AttendanceRecord không có soft delete
                AttendanceRecordEntity attendanceRecord = null;
                if (entity.getAttendanceRecordId() != null) {
                        attendanceRecord = attendanceRecordRepository
                                        .findById(entity.getAttendanceRecordId())
                                        .orElse(null);
                }

                // Lấy break record nếu có breakRecordId
                // BreakRecord không có soft delete
                BreakRecordEntity breakRecord = null;
                if (entity.getBreakRecordId() != null) {
                        breakRecord = breakRecordRepository.findById(entity.getBreakRecordId())
                                        .orElse(null);
                }

                String approverName = entity.getApprovedBy() != null
                                ? getEmployeeName(entity.getApprovedBy())
                                : null;

                return adjustmentMapper.toResponse(
                                entity,
                                getEmployeeName(entity.getEmployeeId()),
                                approverName,
                                attendanceRecord,
                                breakRecord);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<AdjustmentRequestResponse> getPendingRequests(Long companyId, Long userId, boolean isAdmin,
                        Pageable pageable) {
                Page<AttendanceAdjustmentRequestEntity> requests;
                if (isAdmin) {
                        // Admin xem tất cả yêu cầu pending của công ty
                        requests = adjustmentRepository.findPendingByCompanyId(companyId, pageable);
                } else {
                        // Manager chỉ xem yêu cầu được gán cho mình
                        requests = adjustmentRepository.findPendingByCompanyIdAndAssignedTo(companyId, userId,
                                        pageable);
                }
                return requests.map(this::mapToResponse);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<AdjustmentRequestResponse> getAllRequests(Long companyId, Long userId, boolean isAdmin,
                        Pageable pageable) {
                Page<AttendanceAdjustmentRequestEntity> requests;
                if (isAdmin) {
                        // Admin xem tất cả yêu cầu của công ty
                        requests = adjustmentRepository.findByCompanyId(companyId, pageable);
                } else {
                        // Manager chỉ xem yêu cầu được gán cho mình
                        requests = adjustmentRepository.findByCompanyIdAndAssignedTo(companyId, userId, pageable);
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

        /**
         * Tìm yêu cầu điều chỉnh theo ID
         * AttendanceAdjustmentRequest không có soft delete
         */
        private AttendanceAdjustmentRequestEntity findAdjustmentRequest(Long requestId) {
                return adjustmentRepository.findById(requestId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Không tìm thấy yêu cầu điều chỉnh",
                                                ErrorCode.ADJUSTMENT_NOT_FOUND));
        }

        /**
         * Tìm bản ghi chấm công theo ID
         * AttendanceRecord không có soft delete
         */
        private AttendanceRecordEntity findAttendanceRecord(Long recordId) {
                return attendanceRecordRepository.findById(recordId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Không tìm thấy bản ghi chấm công",
                                                ErrorCode.ATTENDANCE_RECORD_NOT_FOUND));
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
         * Validate thời gian điều chỉnh
         */
        private void validateAdjustmentTimes(CreateAdjustmentRequest request, AttendanceRecordEntity record) {
                LocalDateTime checkIn = request.getRequestedCheckIn() != null
                                ? request.getRequestedCheckIn()
                                : record.getOriginalCheckIn();
                LocalDateTime checkOut = request.getRequestedCheckOut() != null
                                ? request.getRequestedCheckOut()
                                : record.getOriginalCheckOut();

                // Nếu có cả check-in và check-out, check-in phải trước check-out
                if (checkIn != null && checkOut != null && !checkIn.isBefore(checkOut)) {
                        throw new BadRequestException(
                                        "Giờ check-in phải trước giờ check-out",
                                        ErrorCode.INVALID_ADJUSTMENT_TIME);
                }
        }

        /**
         * Validate thời gian điều chỉnh khi không có attendance record
         */
        private void validateAdjustmentTimesWithoutRecord(CreateAdjustmentRequest request) {
                LocalDateTime checkIn = request.getRequestedCheckIn();
                LocalDateTime checkOut = request.getRequestedCheckOut();

                // Nếu có cả check-in và check-out, check-in phải trước check-out
                if (checkIn != null && checkOut != null && !checkIn.isBefore(checkOut)) {
                        throw new BadRequestException(
                                        "Giờ check-in phải trước giờ check-out",
                                        ErrorCode.INVALID_ADJUSTMENT_TIME);
                }
        }

        /**
         * Cập nhật bản ghi chấm công sau khi yêu cầu điều chỉnh được duyệt
         */
        private void updateAttendanceRecord(AttendanceAdjustmentRequestEntity adjustment) {
                // Sử dụng AttendanceService để cập nhật và tính toán lại
                AdjustAttendanceRequest adjustRequest = new AdjustAttendanceRequest();

                // Sử dụng thời gian yêu cầu nếu có, ngược lại giữ nguyên thời gian gốc
                if (adjustment.getRequestedCheckIn() != null) {
                        adjustRequest.setCheckInTime(adjustment.getRequestedCheckIn());
                }
                if (adjustment.getRequestedCheckOut() != null) {
                        adjustRequest.setCheckOutTime(adjustment.getRequestedCheckOut());
                }

                // Tạo break adjustment nếu có breakRecordId
                if (adjustment.getBreakRecordId() != null &&
                                (adjustment.getRequestedBreakStart() != null
                                                || adjustment.getRequestedBreakEnd() != null)) {
                        AdjustAttendanceRequest.BreakAdjustment breakAdj = AdjustAttendanceRequest.BreakAdjustment
                                        .builder()
                                        .breakRecordId(adjustment.getBreakRecordId())
                                        .breakStartTime(adjustment.getRequestedBreakStart())
                                        .breakEndTime(adjustment.getRequestedBreakEnd())
                                        .build();
                        adjustRequest.setBreakAdjustments(java.util.List.of(breakAdj));
                }

                adjustRequest.setReason(
                                "Điều chỉnh theo yêu cầu #" + adjustment.getId() + ": " + adjustment.getReason());

                attendanceService.adjustAttendance(
                                adjustment.getAttendanceRecordId(),
                                adjustment.getApprovedBy(),
                                adjustRequest);
        }

        /**
         * Map entity sang response với đầy đủ thông tin
         */
        private AdjustmentRequestResponse mapToResponse(AttendanceAdjustmentRequestEntity entity) {
                // AttendanceRecord không có soft delete
                AttendanceRecordEntity attendanceRecord = null;
                if (entity.getAttendanceRecordId() != null) {
                        attendanceRecord = attendanceRecordRepository
                                        .findById(entity.getAttendanceRecordId())
                                        .orElse(null);
                }

                // Lấy break record nếu có breakRecordId
                // BreakRecord không có soft delete
                BreakRecordEntity breakRecord = null;
                if (entity.getBreakRecordId() != null) {
                        breakRecord = breakRecordRepository.findById(entity.getBreakRecordId())
                                        .orElse(null);
                }

                String approverName = entity.getApprovedBy() != null
                                ? getEmployeeName(entity.getApprovedBy())
                                : null;

                String assignedToName = entity.getAssignedTo() != null
                                ? getEmployeeName(entity.getAssignedTo())
                                : null;

                // Ưu tiên workDate từ entity, fallback sang attendanceRecord
                LocalDate workDate = entity.getWorkDate() != null
                                ? entity.getWorkDate()
                                : (attendanceRecord != null ? attendanceRecord.getWorkDate() : null);

                return adjustmentMapper.toResponse(
                                entity,
                                getEmployeeName(entity.getEmployeeId()),
                                approverName,
                                assignedToName,
                                workDate,
                                breakRecord);
        }
}
