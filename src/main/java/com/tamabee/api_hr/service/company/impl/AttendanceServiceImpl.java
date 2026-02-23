package com.tamabee.api_hr.service.company.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.constants.NotificationCode;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.request.attendance.AdjustAttendanceRequest;
import com.tamabee.api_hr.dto.request.attendance.AttendanceQueryRequest;
import com.tamabee.api_hr.dto.request.attendance.CheckInRequest;
import com.tamabee.api_hr.dto.request.attendance.CheckOutRequest;
import com.tamabee.api_hr.dto.request.attendance.CreateAttendanceRequest;
import com.tamabee.api_hr.dto.request.attendance.EndBreakRequest;
import com.tamabee.api_hr.dto.request.attendance.StartBreakRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceAlertResponse;
import com.tamabee.api_hr.dto.response.attendance.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.attendance.AttendanceSummaryResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftInfoResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.entity.attendance.ShiftAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.NotificationType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.AttendanceMapper;
import com.tamabee.api_hr.repository.attendance.AttendanceLocationRepository;
import com.tamabee.api_hr.repository.attendance.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.attendance.BreakRecordRepository;
import com.tamabee.api_hr.repository.attendance.ShiftAssignmentRepository;
import com.tamabee.api_hr.repository.attendance.ShiftTemplateRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.calculator.interfaces.IBreakCalculator;
import com.tamabee.api_hr.service.calculator.interfaces.ITimeRoundingCalculator;
import com.tamabee.api_hr.service.company.cache.ICachedCompanySettingsService;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceService;
import com.tamabee.api_hr.service.core.interfaces.INotificationService;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation quản lý chấm công.
 * Áp dụng làm tròn giờ, tính toán giờ làm việc, phát hiện đi muộn/về sớm.
 * Bao gồm cả quản lý break records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements IAttendanceService {

    private final AttendanceLocationRepository attendanceLocationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final UserRepository userRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final ICachedCompanySettingsService cachedSettingsService;
    private final ITimeRoundingCalculator timeRoundingCalculator;
    private final IBreakCalculator breakCalculator;
    private final AttendanceMapper attendanceMapper;
    private final INotificationService notificationService;
    private final com.tamabee.api_hr.repository.leave.HolidayRepository holidayRepository;
    private final com.tamabee.api_hr.repository.leave.LeaveRequestRepository leaveRequestRepository;

    // ==================== Check-in/Check-out ====================

    @Override
    @Transactional
    public AttendanceRecordResponse checkIn(Long employeeId, CheckInRequest request) {
        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        LocalDateTime now = LocalDateTime.now(zone);
        log.info("[TIMEZONE-DEBUG] checkIn - region={}, zone={}, today={}, now={}",
                RegionContext.getCurrentRegion(), zone, today, now);

        AttendanceConfig config = cachedSettingsService.getAttendanceConfig();

        // Kiểm tra GPS bắt buộc (chỉ validate có gửi tọa độ, không chặn ngoài phạm vi — frontend xử lý cảnh báo)
        if (Boolean.TRUE.equals(config.getRequireGeoLocation())) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new BadRequestException("Vui lòng bật chia sẻ vị trí để chấm công", ErrorCode.OUTSIDE_GEOFENCE);
            }
        }

        // Kiểm tra đã có attendance record hôm nay chưa
        Optional<AttendanceRecordEntity> existingRecord = attendanceRecordRepository
                .findByEmployeeIdAndWorkDate(employeeId, today);

        AttendanceRecordEntity entity;

        if (existingRecord.isPresent()) {
            // Đã có record - đây là check-in ca thứ 2 trở đi
            entity = existingRecord.get();

            // Kiểm tra đã check-out ca trước chưa
            if (entity.getOriginalCheckOut() == null) {
                throw new ConflictException("Chưa check-out ca trước, không thể check-in ca mới", 
                        ErrorCode.ALREADY_CHECKED_IN);
            }

            // Lưu thời điểm checkout trước đó
            LocalDateTime previousCheckOut = entity.getOriginalCheckOut();
            
            // Tạo break record: breakStart = previousCheckOut, breakEnd = now
            createBreakRecordBetweenShifts(entity, previousCheckOut, now);

            // Reset checkout về null (coi như chưa checkout, đang làm việc tiếp)
            entity.setOriginalCheckOut(null);
            entity.setRoundedCheckOut(null);

            // Áp dụng làm tròn check-in cho ca tiếp theo
            if (Boolean.TRUE.equals(config.getEnableCheckInRounding()) && config.getCheckInRounding() != null) {
                LocalDateTime roundedCheckIn = timeRoundingCalculator.roundTime(now, config.getCheckInRounding());
                entity.setRoundedCheckIn(roundedCheckIn);
                log.info("Làm tròn check-in ca tiếp theo: {} -> {}", now, roundedCheckIn);
            }

            log.info("Nhân viên {} check-in ca tiếp theo lúc {}, tạo break từ {} đến {}, reset checkout", 
                    employeeId, now, previousCheckOut, now);

        } else {
            // Chưa có record - đây là check-in ca đầu tiên
            entity = new AttendanceRecordEntity();
            entity.setEmployeeId(employeeId);
            entity.setWorkDate(today);
            entity.setOriginalCheckIn(now);
            entity.setStatus(AttendanceStatus.PRESENT);

            // Lưu location info
            entity.setCheckInLatitude(request.getLatitude());
            entity.setCheckInLongitude(request.getLongitude());
            entity.setCheckInOutOfRange(Boolean.TRUE.equals(request.getOutOfRange()));

            // Áp dụng làm tròn nếu được bật (dùng individual toggle)
            LocalDateTime roundedCheckIn = now;
            if (Boolean.TRUE.equals(config.getEnableCheckInRounding()) && config.getCheckInRounding() != null) {
                roundedCheckIn = timeRoundingCalculator.roundTime(now, config.getCheckInRounding());
            }
            entity.setRoundedCheckIn(roundedCheckIn);

            // Tính số phút đi muộn dựa trên shift assignment đầu tiên
            ShiftInfoResponse shiftInfo = getFirstShiftInfo(employeeId, today);
            if (shiftInfo != null && shiftInfo.getScheduledStart() != null) {
                int lateMinutes = calculateLateMinutes(roundedCheckIn, shiftInfo.getScheduledStart(), config);
                entity.setLateMinutes(lateMinutes);
            }

            log.info("Nhân viên {} check-in ca đầu tiên lúc {}", employeeId, now);
        }

        entity = attendanceRecordRepository.save(entity);
        return attendanceMapper.toResponse(entity, getEmployeeName(employeeId));
    }

    @Override
    @Transactional
    public AttendanceRecordResponse checkOut(Long employeeId, CheckOutRequest request) {
        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        LocalDateTime now = LocalDateTime.now(zone);
        log.info("[TIMEZONE-DEBUG] checkOut - region={}, zone={}, today={}, now={}",
                RegionContext.getCurrentRegion(), zone, today, now);

        // Tìm bản ghi check-in hôm nay
        // AttendanceRecord không có soft delete
        AttendanceRecordEntity entity = attendanceRecordRepository
                .findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseThrow(
                        () -> new BadRequestException("Chưa check-in, không thể check-out", ErrorCode.NOT_CHECKED_IN));

        // Kiểm tra đã check-out chưa
        if (entity.getOriginalCheckOut() != null) {
            throw new ConflictException("Đã check-out hôm nay", ErrorCode.ALREADY_CHECKED_OUT);
        }

        // Lấy cấu hình chấm công
        AttendanceConfig config = cachedSettingsService.getAttendanceConfig();

        // Kiểm tra GPS bắt buộc (chỉ validate có gửi tọa độ, không chặn ngoài phạm vi — frontend xử lý cảnh báo)
        if (Boolean.TRUE.equals(config.getRequireGeoLocation())) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new BadRequestException("Vui lòng bật chia sẻ vị trí để chấm công", ErrorCode.OUTSIDE_GEOFENCE);
            }
        }

        // Cập nhật check-out
        entity.setOriginalCheckOut(now);
        entity.setCheckOutLatitude(request.getLatitude());
        entity.setCheckOutLongitude(request.getLongitude());
        entity.setCheckOutOutOfRange(Boolean.TRUE.equals(request.getOutOfRange()));

        // Áp dụng làm tròn nếu được bật (dùng individual toggle)
        LocalDateTime roundedCheckOut = now;
        if (Boolean.TRUE.equals(config.getEnableCheckOutRounding()) && config.getCheckOutRounding() != null) {
            roundedCheckOut = timeRoundingCalculator.roundTime(now, config.getCheckOutRounding());
        }
        entity.setRoundedCheckOut(roundedCheckOut);

        // Tính toán giờ làm việc và về sớm dựa trên shift assignment
        ShiftInfoResponse shiftInfo = getShiftInfo(employeeId, today);
        calculateWorkingHours(entity, shiftInfo, config);

        entity = attendanceRecordRepository.save(entity);
        log.info("Nhân viên {} đã check-out lúc {}", employeeId, now);

        return attendanceMapper.toResponse(entity, getEmployeeName(employeeId));
    }

    // ==================== Adjustment ====================

    @Override
    @Transactional
    public AttendanceRecordResponse adjustAttendance(Long recordId, Long adjustedBy, AdjustAttendanceRequest request) {
        return adjustAttendance(recordId, adjustedBy, request, true);
    }

    @Override
    @Transactional
    public AttendanceRecordResponse adjustAttendance(Long recordId, Long adjustedBy, AdjustAttendanceRequest request,
            boolean sendNotification) {
        AttendanceRecordEntity entity = findRecordById(recordId);

        // Lưu thời gian gốc nếu chưa có adjustment
        if (entity.getAdjustedAt() == null) {
            // Giữ nguyên original times
        }

        // Cập nhật thời gian mới
        if (request.getCheckInTime() != null) {
            entity.setOriginalCheckIn(request.getCheckInTime());
        }
        if (request.getCheckOutTime() != null) {
            entity.setOriginalCheckOut(request.getCheckOutTime());
        }

        // Cập nhật nhiều break records nếu có
        if (request.getBreakAdjustments() != null && !request.getBreakAdjustments().isEmpty()) {
            for (AdjustAttendanceRequest.BreakAdjustment breakAdj : request.getBreakAdjustments()) {
                if (breakAdj.getBreakRecordId() != null) {
                    updateBreakRecord(entity, breakAdj.getBreakRecordId(),
                            breakAdj.getBreakStartTime(), breakAdj.getBreakEndTime());
                }
            }
        }

        // Lấy cấu hình và tính toán lại
        AttendanceConfig config = cachedSettingsService.getAttendanceConfig();

        // Áp dụng làm tròn (dùng individual toggles)
        if (request.getCheckInTime() != null && Boolean.TRUE.equals(config.getEnableCheckInRounding()) && config.getCheckInRounding() != null) {
            entity.setRoundedCheckIn(timeRoundingCalculator.roundTime(
                    entity.getOriginalCheckIn(), config.getCheckInRounding()));
        } else if (request.getCheckInTime() != null) {
            entity.setRoundedCheckIn(entity.getOriginalCheckIn());
        }
        if (request.getCheckOutTime() != null && Boolean.TRUE.equals(config.getEnableCheckOutRounding()) && config.getCheckOutRounding() != null) {
            entity.setRoundedCheckOut(timeRoundingCalculator.roundTime(
                    entity.getOriginalCheckOut(), config.getCheckOutRounding()));
        } else if (request.getCheckOutTime() != null) {
            entity.setRoundedCheckOut(entity.getOriginalCheckOut());
        }

        // Tính toán lại giờ làm việc dựa trên shift assignment
        ShiftInfoResponse shiftInfo = getShiftInfo(entity.getEmployeeId(), entity.getWorkDate());
        if (shiftInfo != null) {
            // Tính lại late minutes
            if (entity.getRoundedCheckIn() != null && shiftInfo.getScheduledStart() != null) {
                int lateMinutes = calculateLateMinutes(entity.getRoundedCheckIn(), 
                        shiftInfo.getScheduledStart(), config);
                entity.setLateMinutes(lateMinutes);
            }
        }
        // Tính lại working hours và early leave (kể cả khi không có shift)
        if (entity.getRoundedCheckOut() != null) {
            calculateWorkingHours(entity, shiftInfo, config);
        }

        // Lưu audit info
        entity.setAdjustmentReason(request.getReason());
        entity.setAdjustedBy(adjustedBy);
        entity.setAdjustedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));

        entity = attendanceRecordRepository.save(entity);
        log.info("Bản ghi chấm công {} đã được điều chỉnh bởi {}", recordId, adjustedBy);

        // Gửi thông báo cho nhân viên (chỉ khi admin/manager chủ động sửa, không gửi khi duyệt yêu cầu)
        if (sendNotification) {
            notifyEmployeeAttendanceAdjusted(entity.getEmployeeId(), adjustedBy, entity.getWorkDate());
        }

        return attendanceMapper.toResponse(entity, getEmployeeName(entity.getEmployeeId()));
    }

    // ==================== Create Attendance Record ====================

    @Override
    @Transactional
    public AttendanceRecordResponse createAttendanceRecord(Long createdBy, CreateAttendanceRequest request) {
        // Kiểm tra nhân viên tồn tại
        UserEntity employee = userRepository.findByIdAndDeletedFalse(request.getEmployeeId())
                .orElseThrow(() -> NotFoundException.user(request.getEmployeeId()));

        // Kiểm tra đã có record cho ngày này chưa
        Optional<AttendanceRecordEntity> existing = attendanceRecordRepository
                .findByEmployeeIdAndWorkDate(request.getEmployeeId(), request.getWorkDate());
        if (existing.isPresent()) {
            throw new ConflictException(
                    "Nhân viên đã có bản ghi chấm công cho ngày này",
                    ErrorCode.ATTENDANCE_ALREADY_EXISTS);
        }

        // Tạo attendance record
        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setEmployeeId(request.getEmployeeId());
        entity.setWorkDate(request.getWorkDate());
        entity.setOriginalCheckIn(request.getCheckInTime());
        entity.setOriginalCheckOut(request.getCheckOutTime());
        entity.setStatus(AttendanceStatus.PRESENT);
        entity.setAdjustmentReason(request.getReason());
        entity.setAdjustedBy(createdBy);
        entity.setAdjustedAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));

        // Áp dụng làm tròn
        AttendanceConfig config = cachedSettingsService.getAttendanceConfig();
        if (request.getCheckInTime() != null) {
            if (Boolean.TRUE.equals(config.getEnableCheckInRounding()) && config.getCheckInRounding() != null) {
                entity.setRoundedCheckIn(timeRoundingCalculator.roundTime(
                        request.getCheckInTime(), config.getCheckInRounding()));
            } else {
                entity.setRoundedCheckIn(request.getCheckInTime());
            }
        }
        if (request.getCheckOutTime() != null) {
            if (Boolean.TRUE.equals(config.getEnableCheckOutRounding()) && config.getCheckOutRounding() != null) {
                entity.setRoundedCheckOut(timeRoundingCalculator.roundTime(
                        request.getCheckOutTime(), config.getCheckOutRounding()));
            } else {
                entity.setRoundedCheckOut(request.getCheckOutTime());
            }
        }

        entity = attendanceRecordRepository.save(entity);

        // Tạo break records nếu có
        int totalBreakMinutes = 0;
        if (request.getBreakItems() != null && !request.getBreakItems().isEmpty()) {
            int breakNumber = 1;
            for (CreateAttendanceRequest.BreakItem item : request.getBreakItems()) {
                if (item.getBreakStartTime() != null && item.getBreakEndTime() != null) {
                    BreakRecordEntity breakRecord = new BreakRecordEntity();
                    breakRecord.setAttendanceRecordId(entity.getId());
                    breakRecord.setEmployeeId(request.getEmployeeId());
                    breakRecord.setWorkDate(request.getWorkDate());
                    breakRecord.setBreakNumber(breakNumber++);
                    breakRecord.setBreakStart(item.getBreakStartTime());
                    breakRecord.setBreakEnd(item.getBreakEndTime());

                    int breakMinutes = (int) java.time.Duration.between(
                            item.getBreakStartTime(), item.getBreakEndTime()).toMinutes();
                    breakRecord.setActualBreakMinutes(breakMinutes);
                    breakRecord.setEffectiveBreakMinutes(breakMinutes);

                    breakRecordRepository.save(breakRecord);
                    totalBreakMinutes += breakMinutes;
                }
            }
        }

        entity.setTotalBreakMinutes(totalBreakMinutes);
        entity.setEffectiveBreakMinutes(totalBreakMinutes);

        // Tính toán giờ làm việc
        ShiftInfoResponse shiftInfo = getShiftInfo(request.getEmployeeId(), request.getWorkDate());
        if (shiftInfo != null && entity.getRoundedCheckIn() != null) {
            int lateMinutes = calculateLateMinutes(entity.getRoundedCheckIn(),
                    shiftInfo.getScheduledStart(), config);
            entity.setLateMinutes(lateMinutes);
        }
        if (entity.getRoundedCheckOut() != null) {
            calculateWorkingHours(entity, shiftInfo, config);
        }

        entity = attendanceRecordRepository.save(entity);
        log.info("Manager {} đã tạo bản ghi chấm công {} cho nhân viên {} ngày {}",
                createdBy, entity.getId(), request.getEmployeeId(), request.getWorkDate());

        // Gửi thông báo cho nhân viên
        notifyEmployeeAttendanceAdjusted(request.getEmployeeId(), createdBy, request.getWorkDate());

        return attendanceMapper.toResponse(entity, getEmployeeName(entity.getEmployeeId()));
    }

    /**
     * Cập nhật break record khi điều chỉnh chấm công.
     * Nếu breakRecordId được chỉ định, chỉ cập nhật break record đó.
     * Nếu không, cập nhật break record đầu tiên hoặc tạo mới.
     */
    private void updateBreakRecord(AttendanceRecordEntity attendance, Long breakRecordId,
            LocalDateTime breakStart, LocalDateTime breakEnd) {
        BreakRecordEntity breakRecord;

        if (breakRecordId != null) {
            // Cập nhật break record cụ thể được chỉ định
            // BreakRecord không có soft delete
            breakRecord = breakRecordRepository.findById(breakRecordId)
                    .orElseThrow(() -> new NotFoundException(
                            "Không tìm thấy bản ghi giờ giải lao",
                            ErrorCode.BREAK_RECORD_NOT_FOUND));

            // Validate break record thuộc về attendance record này
            if (!breakRecord.getAttendanceRecordId().equals(attendance.getId())) {
                throw new BadRequestException(
                        "Bản ghi giờ giải lao không thuộc về bản ghi chấm công này",
                        ErrorCode.INVALID_BREAK_RECORD);
            }
        } else {
            // Tìm break record hiện tại hoặc tạo mới
            // BreakRecord không có soft delete
            List<BreakRecordEntity> breakRecords = breakRecordRepository
                    .findByAttendanceRecordId(attendance.getId());

            if (breakRecords.isEmpty()) {
                // Tạo mới nếu chưa có
                breakRecord = new BreakRecordEntity();
                breakRecord.setAttendanceRecordId(attendance.getId());
                breakRecord.setEmployeeId(attendance.getEmployeeId());
                breakRecord.setWorkDate(attendance.getWorkDate());
                breakRecord.setBreakNumber(1);
            } else {
                // Cập nhật record đầu tiên
                breakRecord = breakRecords.get(0);
            }
        }

        // Cập nhật thời gian break
        if (breakStart != null) {
            breakRecord.setBreakStart(breakStart);
        }
        if (breakEnd != null) {
            breakRecord.setBreakEnd(breakEnd);
        }

        // Tính toán lại break minutes
        if (breakRecord.getBreakStart() != null && breakRecord.getBreakEnd() != null) {
            long breakMinutes = java.time.Duration.between(
                    breakRecord.getBreakStart(), breakRecord.getBreakEnd()).toMinutes();
            breakRecord.setActualBreakMinutes((int) breakMinutes);
            breakRecord.setEffectiveBreakMinutes((int) breakMinutes);
        }

        breakRecordRepository.save(breakRecord);

        // Cập nhật tổng break minutes trong attendance record
        updateTotalBreakMinutes(attendance);
    }

    /**
     * Cập nhật tổng thời gian break trong attendance record từ tất cả break records
     */
    private void updateTotalBreakMinutes(AttendanceRecordEntity attendance) {
        // BreakRecord không có soft delete
        List<BreakRecordEntity> allBreaks = breakRecordRepository
                .findByAttendanceRecordId(attendance.getId());

        int totalBreakMinutes = allBreaks.stream()
                .filter(b -> b.getActualBreakMinutes() != null)
                .mapToInt(BreakRecordEntity::getActualBreakMinutes)
                .sum();

        attendance.setTotalBreakMinutes(totalBreakMinutes);
        attendance.setEffectiveBreakMinutes(totalBreakMinutes);
    }

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public AttendanceRecordResponse getAttendanceRecordById(Long recordId) {
        AttendanceRecordEntity entity = findRecordById(recordId);
        return buildFullResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceRecordResponse getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date) {
        // AttendanceRecord không có soft delete
        return attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, date)
                .map(this::buildFullResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceRecordResponse getTodayAttendance(Long employeeId) {
        log.info("Getting today attendance for employee: {}", employeeId);
        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        log.info("[TIMEZONE-DEBUG] getTodayAttendance - region={}, zone={}, today={}",
                RegionContext.getCurrentRegion(), zone, today);
        return getAttendanceByEmployeeAndDate(employeeId, today);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceRecordResponse> getAttendanceRecords(
            AttendanceQueryRequest request, Pageable pageable) {
        // Mặc định lấy tháng hiện tại nếu không có date filter
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null && endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        } else if (startDate == null) {
            startDate = endDate.withDayOfMonth(1);
        } else if (endDate == null) {
            endDate = YearMonth.from(startDate).atEndOfMonth();
        }

        Page<AttendanceRecordEntity> page = attendanceRecordRepository.findByWorkDateBetween(startDate, endDate,
                pageable);

        // Batch fetch break records cho tất cả records trong page
        List<Long> recordIds = page.getContent().stream().map(AttendanceRecordEntity::getId).toList();
        Map<Long, List<BreakRecordEntity>> breakRecordsByAttendanceId = recordIds.isEmpty()
                ? Map.of()
                : breakRecordRepository.findByAttendanceRecordIdIn(recordIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(BreakRecordEntity::getAttendanceRecordId));

        return page.map(entity -> {
            AttendanceRecordResponse response = attendanceMapper.toResponse(entity,
                    getEmployeeName(entity.getEmployeeId()));
            List<BreakRecordEntity> breaks = breakRecordsByAttendanceId.getOrDefault(entity.getId(), List.of());
            response.setBreakRecords(
                    breaks.stream().map(attendanceMapper::toBreakRecordResponse).toList());
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceRecordResponse> getEmployeeAttendanceRecords(
            Long employeeId, AttendanceQueryRequest request, Pageable pageable) {
        return attendanceRecordRepository.findByEmployeeIdAndWorkDateBetweenPaged(
                employeeId, request.getStartDate(), request.getEndDate(), pageable)
                .map(entity -> attendanceMapper.toResponse(entity, getEmployeeName(employeeId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceRecordResponse> getAttendanceRecordsWithLocation(
            AttendanceQueryRequest request, Pageable pageable) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null && endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        } else if (startDate == null) {
            startDate = endDate.withDayOfMonth(1);
        } else if (endDate == null) {
            endDate = YearMonth.from(startDate).atEndOfMonth();
        }

        Page<AttendanceRecordEntity> page = attendanceRecordRepository
                .findWithLocationByWorkDateBetween(startDate, endDate, pageable);

        List<Long> recordIds = page.getContent().stream().map(AttendanceRecordEntity::getId).toList();
        Map<Long, List<BreakRecordEntity>> breakRecordsByAttendanceId = recordIds.isEmpty()
                ? Map.of()
                : breakRecordRepository.findByAttendanceRecordIdIn(recordIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(BreakRecordEntity::getAttendanceRecordId));

        return page.map(entity -> {
            AttendanceRecordResponse response = attendanceMapper.toResponse(entity,
                    getEmployeeName(entity.getEmployeeId()));
            List<BreakRecordEntity> breaks = breakRecordsByAttendanceId.getOrDefault(entity.getId(), List.of());
            response.setBreakRecords(
                    breaks.stream().map(attendanceMapper::toBreakRecordResponse).toList());
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceRecordResponse> getEmployeeAttendanceByMonth(Long employeeId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        AttendanceQueryRequest request = AttendanceQueryRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Pageable pageable = PageRequest.of(0, 31, Sort.by(Sort.Direction.ASC, "workDate"));
        return getEmployeeAttendanceRecords(employeeId, request, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceSummary(Long employeeId, YearMonth period) {
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();

        List<AttendanceRecordEntity> records = attendanceRecordRepository
                .findByEmployeeIdAndWorkDateBetween(employeeId, startDate, endDate);

        String employeeName = getEmployeeName(employeeId);
        return attendanceMapper.toSummaryResponse(employeeId, employeeName, period, records);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceAlertResponse> getAttendanceAlerts(Long employeeId, YearMonth period) {
        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = period.atDay(1);
        // Chỉ kiểm tra đến hôm qua (không alert ngày hôm nay vì chưa kết thúc)
        LocalDate endDate = period.atEndOfMonth().isBefore(today) ? period.atEndOfMonth() : today.minusDays(1);

        if (startDate.isAfter(endDate)) {
            return List.of();
        }

        List<AttendanceAlertResponse> alerts = new ArrayList<>();

        // Lấy attendance records trong khoảng
        List<AttendanceRecordEntity> records = attendanceRecordRepository
                .findByEmployeeIdAndWorkDateBetween(employeeId, startDate, endDate);
        Map<LocalDate, AttendanceRecordEntity> recordMap = records.stream()
                .collect(Collectors.toMap(AttendanceRecordEntity::getWorkDate, r -> r, (a, b) -> a));

        // Lấy holidays trong khoảng
        var holidays = holidayRepository.findByDateBetween(startDate, endDate);
        Set<LocalDate> holidayDates = holidays.stream()
                .map(h -> h.getDate())
                .collect(Collectors.toSet());

        // Lấy approved leaves trong khoảng
        var approvedLeaves = leaveRequestRepository
                .findApprovedByEmployeeIdAndDateRange(employeeId, startDate, endDate);

        // Lấy attendance config (saturdayOff, sundayOff)
        var attendanceConfig = cachedSettingsService.getAttendanceConfig();
        boolean saturdayOff = Boolean.TRUE.equals(attendanceConfig.getSaturdayOff());
        boolean sundayOff = Boolean.TRUE.equals(attendanceConfig.getSundayOff());

        // Duyệt từng ngày
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();

            // Bỏ qua ngày nghỉ cuối tuần
            if (saturdayOff && dow == DayOfWeek.SATURDAY) {
                continue;
            }
            if (sundayOff && dow == DayOfWeek.SUNDAY) {
                continue;
            }

            // Bỏ qua ngày lễ
            if (holidayDates.contains(date)) {
                continue;
            }

            AttendanceRecordEntity record = recordMap.get(date);

            // Kiểm tra có đơn nghỉ phép approved cho ngày này không
            final LocalDate checkDate = date;
            var leaveForDate = approvedLeaves.stream()
                    .filter(l -> !l.getStartDate().isAfter(checkDate) && !l.getEndDate().isBefore(checkDate))
                    .findFirst();

            if (record == null) {
                // Không có attendance record
                if (leaveForDate.isPresent()) {
                    // Có đơn nghỉ phép → hiển thị thông tin
                    alerts.add(AttendanceAlertResponse.builder()
                            .date(date)
                            .alertType("ON_LEAVE")
                            .detail(leaveForDate.get().getLeaveType().name())
                            .build());
                } else {
                    // Không có đơn nghỉ phép → cảnh báo chưa chấm công
                    alerts.add(AttendanceAlertResponse.builder()
                            .date(date)
                            .alertType("NO_ATTENDANCE")
                            .build());
                }
                continue;
            }

            // Có record → kiểm tra lỗi
            // 1. Thiếu check-out
            if (record.getOriginalCheckIn() != null && record.getOriginalCheckOut() == null) {
                alerts.add(AttendanceAlertResponse.builder()
                        .date(date)
                        .alertType("MISSING_CHECKOUT")
                        .build());
            }

            // 2. Break không đủ
            if (Boolean.FALSE.equals(record.getBreakCompliant())) {
                alerts.add(AttendanceAlertResponse.builder()
                        .date(date)
                        .alertType("INSUFFICIENT_BREAK")
                        .build());
            }
        }

        return alerts;
    }

    // ==================== Validation ====================

    @Override
    public boolean validateLocation(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }

        // Validate tọa độ hợp lệ
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return false;
        }

        // Lấy danh sách vị trí chấm công đang hoạt động
        var activeLocations = attendanceLocationRepository.findByDeletedFalseAndIsActiveTrue();

        // Nếu chưa cấu hình vị trí nào → cho phép chấm công ở mọi nơi
        if (activeLocations.isEmpty()) {
            return true;
        }

        // Kiểm tra vị trí nằm trong bán kính cho phép của ít nhất 1 location
        for (var location : activeLocations) {
            double distance = calculateHaversineDistance(
                    latitude, longitude,
                    location.getLatitude(), location.getLongitude());
            if (distance <= location.getRadiusMeters()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tính khoảng cách giữa 2 tọa độ GPS bằng công thức Haversine (đơn vị: mét)
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Bán kính trái đất (mét)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    @Transactional
    public void deleteAttendanceRecord(Long recordId, Long deletedBy) {
        AttendanceRecordEntity entity = findRecordById(recordId);

        // Xóa break records liên quan
        List<BreakRecordEntity> breakRecords = breakRecordRepository.findByAttendanceRecordId(recordId);
        if (!breakRecords.isEmpty()) {
            breakRecordRepository.deleteAll(breakRecords);
        }

        // Xóa attendance record
        attendanceRecordRepository.delete(entity);

        log.info("Bản ghi chấm công {} (ngày {}) đã được xóa bởi user {}",
                recordId, entity.getWorkDate(), deletedBy);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Tìm bản ghi chấm công theo ID
     * AttendanceRecord không có soft delete
     */
    private AttendanceRecordEntity findRecordById(Long recordId) {
        return attendanceRecordRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy bản ghi chấm công", ErrorCode.ATTENDANCE_RECORD_NOT_FOUND));
    }

    /**
     * Lấy tên nhân viên
     */
    private String getEmployeeName(Long employeeId) {
        return userRepository.findWithProfileByIdAndDeletedFalse(employeeId)
                .map(user -> user.getProfile() != null ? user.getProfile().getName() : user.getEmail())
                .orElse("Unknown");
    }

    /**
     * Gửi thông báo cho nhân viên khi admin/manager điều chỉnh chấm công
     */
    private void notifyEmployeeAttendanceAdjusted(Long employeeId, Long adjustedBy, LocalDate workDate) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("workDate", workDate.toString());
            params.put("adjustedBy", getEmployeeName(adjustedBy));

            notificationService.createNotification(
                    employeeId,
                    NotificationCode.ATTENDANCE_ADJUSTED,
                    params,
                    "/me/attendance/" + workDate,
                    NotificationType.ADJUSTMENT
            );
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo điều chỉnh chấm công cho nhân viên {}: {}", employeeId, e.getMessage());
        }
    }

    /**
     * Tính số phút đi muộn
     */
    private int calculateLateMinutes(LocalDateTime checkInTime, LocalTime scheduleStartTime,
            AttendanceConfig config) {
        if (scheduleStartTime == null) {
            return 0;
        }

        // Thêm grace period
        int graceMinutes = config.getLateGraceMinutes() != null ? config.getLateGraceMinutes() : 0;
        LocalTime graceEndTime = scheduleStartTime.plusMinutes(graceMinutes);

        LocalTime checkInLocalTime = checkInTime.toLocalTime();
        if (checkInLocalTime.isAfter(graceEndTime)) {
            return (int) ChronoUnit.MINUTES.between(scheduleStartTime, checkInLocalTime);
        }

        return 0;
    }

    /**
     * Tính toán giờ làm việc, tăng ca, và về sớm
     * Tích hợp break calculation theo break policy
     */
    private void calculateWorkingHours(AttendanceRecordEntity entity, ShiftInfoResponse shiftInfo,
            AttendanceConfig config) {
        LocalDateTime checkIn = entity.getRoundedCheckIn();
        LocalDateTime checkOut = entity.getRoundedCheckOut();

        if (checkIn == null || checkOut == null) {
            return;
        }

        // Lấy break config
        BreakConfig breakConfig = cachedSettingsService.getBreakConfig();

        // Tính tổng số phút làm việc (gross)
        long totalMinutes = ChronoUnit.MINUTES.between(checkIn, checkOut);
        if (totalMinutes < 0) {
            totalMinutes = 0;
        }

        // Tính break minutes
        int actualBreakMinutes = 0;
        int effectiveBreakMinutes = 0;
        boolean isNightShift = false;
        int workingHours = (int) (totalMinutes / 60);

        if (breakConfig != null && Boolean.TRUE.equals(breakConfig.getBreakEnabled())) {
            // Lấy break records nếu có tracking
            // BreakRecord không có soft delete
            List<BreakRecordEntity> breakRecords = breakRecordRepository
                    .findByAttendanceRecordId(entity.getId());

            if (!breakRecords.isEmpty()) {
                // Sử dụng actual break từ records
                actualBreakMinutes = breakCalculator.calculateTotalBreakMinutes(breakRecords);
            }

            // Kiểm tra night shift
            if (shiftInfo != null && shiftInfo.getScheduledStart() != null && shiftInfo.getScheduledEnd() != null) {
                isNightShift = breakCalculator.isNightShift(
                        shiftInfo.getScheduledStart(), shiftInfo.getScheduledEnd(), breakConfig);
            }

            // Tính effective break (với min/max capping)
            effectiveBreakMinutes = breakCalculator.calculateEffectiveBreakMinutes(
                    actualBreakMinutes, breakConfig, workingHours, isNightShift);

            // Lưu break info vào entity
            entity.setTotalBreakMinutes(actualBreakMinutes);
            entity.setEffectiveBreakMinutes(effectiveBreakMinutes);

            // Giải lao luôn bị trừ, không cần kiểm tra compliance phức tạp
            entity.setBreakCompliant(true);
        } else {
            // Không có break config hoặc break không được bật - sử dụng default
            actualBreakMinutes = 60; // Default 1 hour
            effectiveBreakMinutes = actualBreakMinutes;
            entity.setTotalBreakMinutes(actualBreakMinutes);
            entity.setEffectiveBreakMinutes(effectiveBreakMinutes);
            entity.setBreakCompliant(true);
        }

        // Tính net working minutes (sau khi trừ break nếu unpaid)
        int netWorkingMinutes;
        if (breakConfig != null) {
            netWorkingMinutes = breakCalculator.calculateNetWorkingMinutes(
                    (int) totalMinutes, effectiveBreakMinutes, breakConfig);
        } else {
            // Không có break config - trừ break mặc định
            netWorkingMinutes = (int) totalMinutes - effectiveBreakMinutes;
            if (netWorkingMinutes < 0) {
                netWorkingMinutes = 0;
            }
        }
        entity.setWorkingMinutes(netWorkingMinutes);

        // Tính số phút về sớm
        if (shiftInfo != null && shiftInfo.getScheduledEnd() != null && config != null) {
            int graceMinutes = config.getEarlyLeaveGraceMinutes() != null ? config.getEarlyLeaveGraceMinutes() : 0;
            LocalTime graceStartTime = shiftInfo.getScheduledEnd().minusMinutes(graceMinutes);

            LocalTime checkOutLocalTime = checkOut.toLocalTime();
            if (checkOutLocalTime.isBefore(graceStartTime)) {
                int earlyMinutes = (int) ChronoUnit.MINUTES.between(checkOutLocalTime, shiftInfo.getScheduledEnd());
                entity.setEarlyLeaveMinutes(earlyMinutes);
            } else {
                entity.setEarlyLeaveMinutes(0);
            }
        } else {
            entity.setEarlyLeaveMinutes(0);
        }

        // Tính số phút tăng ca (làm việc vượt quá giờ chuẩn)
        int standardMinutes = getStandardWorkingMinutes(shiftInfo);
        if (netWorkingMinutes > standardMinutes) {
            entity.setOvertimeMinutes(netWorkingMinutes - standardMinutes);
        } else {
            entity.setOvertimeMinutes(0);
        }
    }

    /**
     * Lấy số phút làm việc chuẩn trong ngày từ shift info
     */
    private int getStandardWorkingMinutes(ShiftInfoResponse shiftInfo) {
        if (shiftInfo == null || shiftInfo.getScheduledStart() == null || shiftInfo.getScheduledEnd() == null) {
            return 8 * 60; // Default 8 hours
        }

        LocalTime startTime = shiftInfo.getScheduledStart();
        LocalTime endTime = shiftInfo.getScheduledEnd();
        
        // Lấy break minutes từ config
        BreakConfig breakConfig = cachedSettingsService.getBreakConfig();
        int breakMinutes = breakConfig != null && breakConfig.getDefaultBreakMinutes() != null
                ? breakConfig.getDefaultBreakMinutes() : 60;

        int totalMinutes = (int) ChronoUnit.MINUTES.between(startTime, endTime);
        return Math.max(0, totalMinutes - breakMinutes);
    }

    // ==================== Break Management ====================

    @Override
    @Transactional
    public AttendanceRecordResponse startBreak(Long employeeId, StartBreakRequest request) {
        log.info("Start break for employee: {}", employeeId);

        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        LocalDateTime now = LocalDateTime.now(zone);
        log.info("[TIMEZONE-DEBUG] startBreak - region={}, zone={}, today={}, now={}",
                RegionContext.getCurrentRegion(), zone, today, now);

        // Kiểm tra user tồn tại
        UserEntity user = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new NotFoundException(
                        "Employee not found with id: " + employeeId,
                        ErrorCode.USER_NOT_FOUND));

        // Lấy attendance record hôm nay
        // AttendanceRecord không có soft delete
        AttendanceRecordEntity attendance = attendanceRecordRepository
                .findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseThrow(() -> new NotFoundException(
                        "Employee has not checked in today",
                        ErrorCode.NOT_CHECKED_IN));

        // Kiểm tra đã check-out chưa (không cho phép break sau khi check-out)
        if (attendance.getOriginalCheckOut() != null) {
            throw new BadRequestException(
                    "Cannot start break after check-out",
                    ErrorCode.ALREADY_CHECKED_OUT);
        }

        // Lấy break config
        BreakConfig breakConfig = cachedSettingsService.getBreakConfig();

        // Kiểm tra break có được bật không
        if (breakConfig == null || !Boolean.TRUE.equals(breakConfig.getBreakEnabled())) {
            throw new BadRequestException(
                    "Break is not enabled for this company",
                    ErrorCode.INVALID_BREAK_CONFIG);
        }

        // Kiểm tra có break đang active không
        Optional<BreakRecordEntity> activeBreak = breakRecordRepository
                .findActiveBreakByEmployeeIdAndWorkDate(employeeId, today);

        if (activeBreak.isPresent()) {
            throw new ConflictException(
                    "There is already an active break",
                    ErrorCode.BREAK_ALREADY_ACTIVE);
        }

        // Kiểm tra số lần break trong ngày
        // BreakRecord không có soft delete
        long breakCount = breakRecordRepository.countByAttendanceRecordId(attendance.getId());

        Integer maxBreaks = breakConfig.getMaxBreaksPerDay() != null
                ? breakConfig.getMaxBreaksPerDay()
                : 3;

        if (breakCount >= maxBreaks) {
            throw new BadRequestException(
                    String.format("Maximum breaks per day (%d) reached", maxBreaks),
                    ErrorCode.MAX_BREAKS_REACHED);
        }

        // Lấy break number tiếp theo
        Integer maxBreakNumber = breakRecordRepository.findMaxBreakNumberByAttendanceRecordId(attendance.getId());
        int nextBreakNumber = (maxBreakNumber != null ? maxBreakNumber : 0) + 1;

        // Tạo break record mới
        BreakRecordEntity breakRecord = new BreakRecordEntity();
        breakRecord.setAttendanceRecordId(attendance.getId());
        breakRecord.setEmployeeId(employeeId);
        breakRecord.setWorkDate(today);
        breakRecord.setBreakNumber(nextBreakNumber);
        breakRecord.setBreakStart(now);
        breakRecord.setNotes(request != null ? request.getNotes() : null);

        // Lưu location info
        if (request != null) {
            breakRecord.setBreakStartLatitude(request.getLatitude());
            breakRecord.setBreakStartLongitude(request.getLongitude());
            breakRecord.setBreakStartOutOfRange(Boolean.TRUE.equals(request.getOutOfRange()));
        }

        breakRecordRepository.save(breakRecord);

        log.info("Break started for employee: {} at {}, break number: {}", employeeId, now, nextBreakNumber);

        // Return full response
        return buildFullResponse(attendance);
    }

    @Override
    @Transactional
    public AttendanceRecordResponse endBreak(Long employeeId, Long breakRecordId, EndBreakRequest request) {
        log.info("End break for employee: {}, breakRecordId: {}", employeeId, breakRecordId);

        LocalDateTime now = LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));

        // Kiểm tra user tồn tại
        UserEntity user = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new NotFoundException(
                        "Employee not found with id: " + employeeId,
                        ErrorCode.USER_NOT_FOUND));

        // Lấy break record
        // BreakRecord không có soft delete
        BreakRecordEntity breakRecord = breakRecordRepository.findById(breakRecordId)
                .orElseThrow(() -> new NotFoundException(
                        "Break record not found with id: " + breakRecordId,
                        ErrorCode.BREAK_RECORD_NOT_FOUND));

        // Kiểm tra break record thuộc về employee này không
        if (!breakRecord.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException(
                    "Break record does not belong to this employee",
                    ErrorCode.INVALID_BREAK_RECORD);
        }

        // Kiểm tra break đã kết thúc chưa
        if (breakRecord.getBreakEnd() != null) {
            throw new ConflictException(
                    "Break has already ended",
                    ErrorCode.BREAK_ALREADY_ACTIVE);
        }

        // Lấy break config (giữ lại để log nếu cần)

        // Update break record
        breakRecord.setBreakEnd(now);

        // Calculate actual break minutes
        long actualMinutes = ChronoUnit.MINUTES.between(breakRecord.getBreakStart(), now);
        breakRecord.setActualBreakMinutes((int) actualMinutes);

        // Calculate effective break minutes (đơn giản: effective = actual)
        int effectiveMinutes = (int) actualMinutes;

        breakRecord.setEffectiveBreakMinutes(effectiveMinutes);

        // Lưu location info
        if (request != null) {
            breakRecord.setBreakEndLatitude(request.getLatitude());
            breakRecord.setBreakEndLongitude(request.getLongitude());
            breakRecord.setBreakEndOutOfRange(Boolean.TRUE.equals(request.getOutOfRange()));
        }

        breakRecordRepository.save(breakRecord);

        log.info("Break ended for employee: {} at {}, actual: {} min, effective: {} min",
                employeeId, now, actualMinutes, effectiveMinutes);

        // Lấy attendance record để return full response
        // AttendanceRecord không có soft delete
        AttendanceRecordEntity attendance = attendanceRecordRepository
                .findById(breakRecord.getAttendanceRecordId())
                .orElseThrow(() -> new NotFoundException(
                        "Attendance record not found",
                        ErrorCode.ATTENDANCE_RECORD_NOT_FOUND));

        // Cập nhật tổng break minutes trong attendance
        updateTotalBreakMinutes(attendance);
        attendanceRecordRepository.save(attendance);

        return buildFullResponse(attendance);
    }

    // ==================== Full Response Builder ====================

    /**
     * Build full response với break records, shift info và applied settings
     */
    private AttendanceRecordResponse buildFullResponse(AttendanceRecordEntity entity) {
        String employeeName = getEmployeeName(entity.getEmployeeId());

        // Lấy break records
        // BreakRecord không có soft delete
        List<BreakRecordEntity> breakRecords = breakRecordRepository
                .findByAttendanceRecordId(entity.getId());

        // Lấy shift info
        ShiftInfoResponse shiftInfo = getShiftInfo(entity.getEmployeeId(), entity.getWorkDate());

        // Lấy configs
        AttendanceConfig attendanceConfig = cachedSettingsService.getAttendanceConfig();
        BreakConfig breakConfig = cachedSettingsService.getBreakConfig();

        return attendanceMapper.toFullResponse(entity, employeeName, breakRecords, shiftInfo,
                attendanceConfig, breakConfig);
    }

    /**
     * Lấy thông tin ca làm việc cho nhân viên vào ngày cụ thể.
     * Nếu có ShiftAssignment → dùng giờ ca; nếu không → dùng Company_Setting mặc định.
     * Nếu nhiều ca → khớp ca có giờ bắt đầu gần nhất với thời điểm chấm công.
     */
    private ShiftInfoResponse getShiftInfo(Long employeeId, LocalDate date) {
        // ShiftAssignment không có soft delete
        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository
                .findByEmployeeIdAndWorkDate(employeeId, date);

        if (assignments.isEmpty()) {
            // Không có assignment → dùng Company_Setting mặc định
            AttendanceConfig config = cachedSettingsService.getAttendanceConfig();
            return ShiftInfoResponse.builder()
                    .shiftName("Default")
                    .scheduledStart(config.getDefaultWorkStartTime())
                    .scheduledEnd(config.getDefaultWorkEndTime())
                    .build();
        }

        // Nếu nhiều ca → khớp ca có giờ bắt đầu gần nhất với thời điểm chấm công
        ShiftAssignmentEntity assignment;
        if (assignments.size() == 1) {
            assignment = assignments.get(0);
        } else {
            // Lấy thời điểm check-in thực tế để khớp ca
            Optional<AttendanceRecordEntity> record = attendanceRecordRepository
                    .findByEmployeeIdAndWorkDate(employeeId, date);
            LocalTime checkInTime = record
                    .map(r -> r.getOriginalCheckIn() != null ? r.getOriginalCheckIn().toLocalTime() : null)
                    .orElse(null);

            assignment = findClosestAssignment(assignments, checkInTime);
        }

        Optional<ShiftTemplateEntity> templateOpt = shiftTemplateRepository
                .findByIdAndDeletedFalse(assignment.getShiftTemplateId());

        if (templateOpt.isEmpty()) {
            // Template bị xóa → fallback về Company_Setting mặc định
            AttendanceConfig config = cachedSettingsService.getAttendanceConfig();
            return ShiftInfoResponse.builder()
                    .shiftName("Default")
                    .scheduledStart(config.getDefaultWorkStartTime())
                    .scheduledEnd(config.getDefaultWorkEndTime())
                    .build();
        }

        ShiftTemplateEntity template = templateOpt.get();

        return ShiftInfoResponse.builder()
                .shiftTemplateId(template.getId())
                .shiftName(template.getName())
                .scheduledStart(template.getStartTime())
                .scheduledEnd(template.getEndTime())
                .multiplier(template.getMultiplier())
                .build();
    }


    /**
     * Lấy thông tin ca đầu tiên trong ngày (dùng cho tính late minutes).
     * Nếu có ShiftAssignment → dùng ca có giờ bắt đầu gần nhất với thời điểm chấm công.
     * Nếu không có assignment → dùng Company_Setting mặc định.
     */
    private ShiftInfoResponse getFirstShiftInfo(Long employeeId, LocalDate date) {
        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository
                .findByEmployeeIdAndWorkDate(employeeId, date);

        if (assignments.isEmpty()) {
            // Không có assignment → dùng Company_Setting mặc định
            AttendanceConfig config = cachedSettingsService.getAttendanceConfig();
            return ShiftInfoResponse.builder()
                    .shiftName("Default")
                    .scheduledStart(config.getDefaultWorkStartTime())
                    .scheduledEnd(config.getDefaultWorkEndTime())
                    .build();
        }

        // Nếu nhiều ca → khớp ca có giờ bắt đầu gần nhất với thời điểm chấm công
        // Lấy thời điểm check-in thực tế
        Optional<AttendanceRecordEntity> record = attendanceRecordRepository
                .findByEmployeeIdAndWorkDate(employeeId, date);
        LocalTime checkInTime = record
                .map(r -> r.getOriginalCheckIn() != null ? r.getOriginalCheckIn().toLocalTime() : null)
                .orElse(null);

        ShiftAssignmentEntity bestAssignment = findClosestAssignment(assignments, checkInTime);

        Optional<ShiftTemplateEntity> templateOpt = shiftTemplateRepository
                .findByIdAndDeletedFalse(bestAssignment.getShiftTemplateId());

        if (templateOpt.isEmpty()) {
            // Template bị xóa → fallback về Company_Setting mặc định
            AttendanceConfig config = cachedSettingsService.getAttendanceConfig();
            return ShiftInfoResponse.builder()
                    .shiftName("Default")
                    .scheduledStart(config.getDefaultWorkStartTime())
                    .scheduledEnd(config.getDefaultWorkEndTime())
                    .build();
        }

        ShiftTemplateEntity template = templateOpt.get();

        return ShiftInfoResponse.builder()
                .shiftTemplateId(template.getId())
                .shiftName(template.getName())
                .scheduledStart(template.getStartTime())
                .scheduledEnd(template.getEndTime())
                .multiplier(template.getMultiplier())
                .build();
    }

    /**
     * Tìm ShiftAssignment có giờ bắt đầu gần nhất với thời điểm chấm công.
     * Nếu checkInTime null → trả về assignment có giờ bắt đầu sớm nhất.
     */
    private ShiftAssignmentEntity findClosestAssignment(List<ShiftAssignmentEntity> assignments, LocalTime checkInTime) {
        if (assignments.size() == 1) {
            return assignments.get(0);
        }

        // Map assignment → template start time
        record AssignmentWithStart(ShiftAssignmentEntity assignment, LocalTime startTime) {}

        List<AssignmentWithStart> withStartTimes = assignments.stream()
                .map(a -> {
                    Optional<ShiftTemplateEntity> tpl = shiftTemplateRepository
                            .findByIdAndDeletedFalse(a.getShiftTemplateId());
                    return tpl.map(t -> new AssignmentWithStart(a, t.getStartTime())).orElse(null);
                })
                .filter(item -> item != null)
                .toList();

        if (withStartTimes.isEmpty()) {
            return assignments.get(0);
        }

        if (checkInTime == null) {
            // Không có check-in → lấy ca sớm nhất
            return withStartTimes.stream()
                    .min((a, b) -> a.startTime().compareTo(b.startTime()))
                    .map(AssignmentWithStart::assignment)
                    .orElse(assignments.get(0));
        }

        // Tìm ca có giờ bắt đầu gần nhất với thời điểm chấm công
        return withStartTimes.stream()
                .min((a, b) -> {
                    long diffA = Math.abs(ChronoUnit.MINUTES.between(checkInTime, a.startTime()));
                    long diffB = Math.abs(ChronoUnit.MINUTES.between(checkInTime, b.startTime()));
                    return Long.compare(diffA, diffB);
                })
                .map(AssignmentWithStart::assignment)
                .orElse(assignments.get(0));
    }

    /**
     * Tạo break record cho khoảng thời gian giữa 2 ca
     */
    private void createBreakRecordBetweenShifts(AttendanceRecordEntity attendance, 
            LocalDateTime breakStart, LocalDateTime breakEnd) {
        // Kiểm tra giới hạn số lần break
        BreakConfig breakConfig = cachedSettingsService.getBreakConfig();
        Integer maxBreaks = breakConfig.getMaxBreaksPerDay() != null 
                ? breakConfig.getMaxBreaksPerDay() 
                : 3;
        
        long currentBreakCount = breakRecordRepository.countByAttendanceRecordId(attendance.getId());
        if (currentBreakCount >= maxBreaks) {
            throw new BadRequestException(
                    String.format("Đã đạt giới hạn %d lần giải lao trong ngày", maxBreaks),
                    ErrorCode.MAX_BREAKS_REACHED);
        }

        // Lấy break number tiếp theo
        Integer maxBreakNumber = breakRecordRepository.findMaxBreakNumberByAttendanceRecordId(attendance.getId());
        int nextBreakNumber = (maxBreakNumber != null ? maxBreakNumber : 0) + 1;

        // Tạo break record
        BreakRecordEntity breakRecord = new BreakRecordEntity();
        breakRecord.setAttendanceRecordId(attendance.getId());
        breakRecord.setEmployeeId(attendance.getEmployeeId());
        breakRecord.setWorkDate(attendance.getWorkDate());
        breakRecord.setBreakNumber(nextBreakNumber);
        breakRecord.setBreakStart(breakStart);
        breakRecord.setBreakEnd(breakEnd);

        // Tính break minutes
        long breakMinutes = ChronoUnit.MINUTES.between(breakStart, breakEnd);
        breakRecord.setActualBreakMinutes((int) breakMinutes);
        breakRecord.setEffectiveBreakMinutes((int) breakMinutes);
        breakRecord.setNotes("Tự động tạo giữa các ca");

        breakRecordRepository.save(breakRecord);

        // Cập nhật tổng break minutes trong attendance
        updateTotalBreakMinutes(attendance);

        log.info("Tạo break record tự động từ {} đến {} ({} phút)", 
                breakStart, breakEnd, breakMinutes);
    }
}
