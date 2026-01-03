package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.dto.request.AdjustAttendanceRequest;
import com.tamabee.api_hr.dto.request.AttendanceQueryRequest;
import com.tamabee.api_hr.dto.request.CheckInRequest;
import com.tamabee.api_hr.dto.request.CheckOutRequest;
import com.tamabee.api_hr.dto.request.StartBreakRequest;
import com.tamabee.api_hr.dto.response.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.AttendanceSummaryResponse;
import com.tamabee.api_hr.dto.response.ShiftInfoResponse;
import com.tamabee.api_hr.dto.response.WorkScheduleResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.entity.attendance.ShiftAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.ScheduleType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.AttendanceMapper;
import com.tamabee.api_hr.repository.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.BreakRecordRepository;
import com.tamabee.api_hr.repository.ShiftAssignmentRepository;
import com.tamabee.api_hr.repository.ShiftTemplateRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.calculator.IBreakCalculator;
import com.tamabee.api_hr.service.calculator.ITimeRoundingCalculator;
import com.tamabee.api_hr.service.company.IAttendanceService;
import com.tamabee.api_hr.service.company.ICompanySettingsService;
import com.tamabee.api_hr.service.company.IWorkScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation quản lý chấm công.
 * Áp dụng làm tròn giờ, tính toán giờ làm việc, phát hiện đi muộn/về sớm.
 * Bao gồm cả quản lý break records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements IAttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final UserRepository userRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final ICompanySettingsService companySettingsService;
    private final IWorkScheduleService workScheduleService;
    private final ITimeRoundingCalculator timeRoundingCalculator;
    private final IBreakCalculator breakCalculator;
    private final AttendanceMapper attendanceMapper;

    // ==================== Check-in/Check-out ====================

    @Override
    @Transactional
    public AttendanceRecordResponse checkIn(Long employeeId, Long companyId, CheckInRequest request) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Kiểm tra đã check-in chưa
        if (attendanceRecordRepository.existsByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, today)) {
            throw new ConflictException("Đã check-in hôm nay", ErrorCode.ALREADY_CHECKED_IN);
        }

        // Lấy cấu hình chấm công
        AttendanceConfig config = companySettingsService.getAttendanceConfig(companyId);

        // Validate device nếu yêu cầu
        if (Boolean.TRUE.equals(config.getRequireDeviceRegistration())) {
            if (!validateDevice(companyId, request.getDeviceId())) {
                throw new BadRequestException("Thiết bị chưa được đăng ký", ErrorCode.INVALID_DEVICE);
            }
        }

        // Validate location nếu yêu cầu
        if (Boolean.TRUE.equals(config.getRequireGeoLocation())) {
            if (!validateLocation(companyId, request.getLatitude(), request.getLongitude())) {
                throw new BadRequestException("Vị trí nằm ngoài khu vực cho phép", ErrorCode.OUTSIDE_GEOFENCE);
            }
        }

        // Tạo bản ghi chấm công
        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setEmployeeId(employeeId);
        entity.setCompanyId(companyId);
        entity.setWorkDate(today);
        entity.setOriginalCheckIn(now);
        entity.setStatus(AttendanceStatus.PRESENT);

        // Lưu device và location info
        entity.setCheckInDeviceId(request.getDeviceId());
        entity.setCheckInLatitude(request.getLatitude());
        entity.setCheckInLongitude(request.getLongitude());

        // Áp dụng làm tròn nếu được bật
        LocalDateTime roundedCheckIn = now;
        if (Boolean.TRUE.equals(config.getEnableRounding()) && config.getCheckInRounding() != null) {
            roundedCheckIn = timeRoundingCalculator.roundTime(now, config.getCheckInRounding());
        }
        entity.setRoundedCheckIn(roundedCheckIn);

        // Tính số phút đi muộn
        WorkScheduleResponse schedule = workScheduleService.getEffectiveSchedule(employeeId, companyId, today);
        if (schedule != null) {
            int lateMinutes = calculateLateMinutes(roundedCheckIn, schedule, config);
            entity.setLateMinutes(lateMinutes);
        }

        entity = attendanceRecordRepository.save(entity);
        log.info("Nhân viên {} đã check-in lúc {}", employeeId, now);

        return attendanceMapper.toResponse(entity, getEmployeeName(employeeId));
    }

    @Override
    @Transactional
    public AttendanceRecordResponse checkOut(Long employeeId, Long companyId, CheckOutRequest request) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Tìm bản ghi check-in hôm nay
        AttendanceRecordEntity entity = attendanceRecordRepository
                .findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, today)
                .orElseThrow(
                        () -> new BadRequestException("Chưa check-in, không thể check-out", ErrorCode.NOT_CHECKED_IN));

        // Kiểm tra đã check-out chưa
        if (entity.getOriginalCheckOut() != null) {
            throw new ConflictException("Đã check-out hôm nay", ErrorCode.ALREADY_CHECKED_OUT);
        }

        // Lấy cấu hình chấm công
        AttendanceConfig config = companySettingsService.getAttendanceConfig(companyId);

        // Validate device nếu yêu cầu
        if (Boolean.TRUE.equals(config.getRequireDeviceRegistration())) {
            if (!validateDevice(companyId, request.getDeviceId())) {
                throw new BadRequestException("Thiết bị chưa được đăng ký", ErrorCode.INVALID_DEVICE);
            }
        }

        // Validate location nếu yêu cầu
        if (Boolean.TRUE.equals(config.getRequireGeoLocation())) {
            if (!validateLocation(companyId, request.getLatitude(), request.getLongitude())) {
                throw new BadRequestException("Vị trí nằm ngoài khu vực cho phép", ErrorCode.OUTSIDE_GEOFENCE);
            }
        }

        // Cập nhật check-out
        entity.setOriginalCheckOut(now);
        entity.setCheckOutDeviceId(request.getDeviceId());
        entity.setCheckOutLatitude(request.getLatitude());
        entity.setCheckOutLongitude(request.getLongitude());

        // Áp dụng làm tròn nếu được bật
        LocalDateTime roundedCheckOut = now;
        if (Boolean.TRUE.equals(config.getEnableRounding()) && config.getCheckOutRounding() != null) {
            roundedCheckOut = timeRoundingCalculator.roundTime(now, config.getCheckOutRounding());
        }
        entity.setRoundedCheckOut(roundedCheckOut);

        // Tính toán giờ làm việc và về sớm
        WorkScheduleResponse schedule = workScheduleService.getEffectiveSchedule(employeeId, companyId, today);
        if (schedule != null) {
            calculateWorkingHours(entity, schedule, config);
        }

        entity = attendanceRecordRepository.save(entity);
        log.info("Nhân viên {} đã check-out lúc {}", employeeId, now);

        return attendanceMapper.toResponse(entity, getEmployeeName(employeeId));
    }

    // ==================== Adjustment ====================

    @Override
    @Transactional
    public AttendanceRecordResponse adjustAttendance(Long recordId, Long adjustedBy, AdjustAttendanceRequest request) {
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
        AttendanceConfig config = companySettingsService.getAttendanceConfig(entity.getCompanyId());

        // Áp dụng làm tròn
        if (Boolean.TRUE.equals(config.getEnableRounding())) {
            if (request.getCheckInTime() != null && config.getCheckInRounding() != null) {
                entity.setRoundedCheckIn(timeRoundingCalculator.roundTime(
                        entity.getOriginalCheckIn(), config.getCheckInRounding()));
            }
            if (request.getCheckOutTime() != null && config.getCheckOutRounding() != null) {
                entity.setRoundedCheckOut(timeRoundingCalculator.roundTime(
                        entity.getOriginalCheckOut(), config.getCheckOutRounding()));
            }
        } else {
            entity.setRoundedCheckIn(entity.getOriginalCheckIn());
            entity.setRoundedCheckOut(entity.getOriginalCheckOut());
        }

        // Tính toán lại giờ làm việc
        WorkScheduleResponse schedule = workScheduleService.getEffectiveSchedule(
                entity.getEmployeeId(), entity.getCompanyId(), entity.getWorkDate());
        if (schedule != null) {
            // Tính lại late minutes
            if (entity.getRoundedCheckIn() != null) {
                int lateMinutes = calculateLateMinutes(entity.getRoundedCheckIn(), schedule, config);
                entity.setLateMinutes(lateMinutes);
            }
            // Tính lại working hours và early leave
            if (entity.getRoundedCheckOut() != null) {
                calculateWorkingHours(entity, schedule, config);
            }
        }

        // Lưu audit info
        entity.setAdjustmentReason(request.getReason());
        entity.setAdjustedBy(adjustedBy);
        entity.setAdjustedAt(LocalDateTime.now());

        entity = attendanceRecordRepository.save(entity);
        log.info("Bản ghi chấm công {} đã được điều chỉnh bởi {}", recordId, adjustedBy);

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
            breakRecord = breakRecordRepository.findByIdAndDeletedFalse(breakRecordId)
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
            List<BreakRecordEntity> breakRecords = breakRecordRepository
                    .findByAttendanceRecordIdAndDeletedFalse(attendance.getId());

            if (breakRecords.isEmpty()) {
                // Tạo mới nếu chưa có
                breakRecord = new BreakRecordEntity();
                breakRecord.setAttendanceRecordId(attendance.getId());
                breakRecord.setEmployeeId(attendance.getEmployeeId());
                breakRecord.setCompanyId(attendance.getCompanyId());
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
        List<BreakRecordEntity> allBreaks = breakRecordRepository
                .findByAttendanceRecordIdAndDeletedFalse(attendance.getId());

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
        return attendanceRecordRepository.findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, date)
                .map(this::buildFullResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceRecordResponse getTodayAttendance(Long employeeId) {
        log.info("Getting today attendance for employee: {}", employeeId);
        LocalDate today = LocalDate.now();
        return getAttendanceByEmployeeAndDate(employeeId, today);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceRecordResponse> getAttendanceRecords(
            Long companyId, AttendanceQueryRequest request, Pageable pageable) {
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

        return attendanceRecordRepository.findByCompanyIdAndWorkDateBetween(
                companyId, startDate, endDate, pageable)
                .map(entity -> attendanceMapper.toResponse(entity, getEmployeeName(entity.getEmployeeId())));
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
    public AttendanceSummaryResponse getAttendanceSummary(Long employeeId, Long companyId, YearMonth period) {
        LocalDate startDate = period.atDay(1);
        LocalDate endDate = period.atEndOfMonth();

        List<AttendanceRecordEntity> records = attendanceRecordRepository
                .findByEmployeeIdAndWorkDateBetween(employeeId, startDate, endDate);

        String employeeName = getEmployeeName(employeeId);
        return attendanceMapper.toSummaryResponse(employeeId, employeeName, period, records);
    }

    // ==================== Validation ====================

    @Override
    public boolean validateDevice(Long companyId, String deviceId) {
        // Validate device ID không rỗng
        // Trong tương lai có thể mở rộng để kiểm tra device đã đăng ký trong bảng
        // registered_devices
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }

        // Device ID hợp lệ nếu có độ dài tối thiểu (UUID hoặc device fingerprint)
        return deviceId.length() >= 8;
    }

    @Override
    public boolean validateLocation(Long companyId, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }

        // Validate tọa độ hợp lệ
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return false;
        }

        // Geo-fence validation: Kiểm tra vị trí nằm trong bán kính cho phép của công ty
        // Hiện tại chấp nhận tất cả vị trí hợp lệ vì chưa có latitude/longitude trong
        // CompanyEntity
        // Khi mở rộng: lấy company location từ DB và tính khoảng cách bằng Haversine
        // formula
        return true;
    }

    // ==================== Private Helper Methods ====================

    /**
     * Tìm bản ghi chấm công theo ID
     */
    private AttendanceRecordEntity findRecordById(Long recordId) {
        return attendanceRecordRepository.findByIdAndDeletedFalse(recordId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy bản ghi chấm công", ErrorCode.ATTENDANCE_RECORD_NOT_FOUND));
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
     * Tính số phút đi muộn
     */
    private int calculateLateMinutes(LocalDateTime checkInTime, WorkScheduleResponse schedule,
            AttendanceConfig config) {
        LocalTime scheduleStartTime = getScheduleStartTime(schedule, checkInTime.toLocalDate());
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
    private void calculateWorkingHours(AttendanceRecordEntity entity, WorkScheduleResponse schedule,
            AttendanceConfig config) {
        LocalDateTime checkIn = entity.getRoundedCheckIn();
        LocalDateTime checkOut = entity.getRoundedCheckOut();

        if (checkIn == null || checkOut == null) {
            return;
        }

        // Lấy break config - có thể null nếu chưa cấu hình
        BreakConfig breakConfig = companySettingsService.getBreakConfig(entity.getCompanyId());

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
            List<BreakRecordEntity> breakRecords = breakRecordRepository
                    .findByAttendanceRecordIdAndDeletedFalse(entity.getId());

            if (Boolean.TRUE.equals(breakConfig.getBreakTrackingEnabled()) && !breakRecords.isEmpty()) {
                // Sử dụng actual break từ records
                actualBreakMinutes = breakCalculator.calculateTotalBreakMinutes(breakRecords);
            } else {
                // Sử dụng default break từ schedule hoặc config
                actualBreakMinutes = getBreakMinutes(schedule, entity.getWorkDate());
            }

            // Kiểm tra night shift
            LocalTime shiftStart = getScheduleStartTime(schedule, entity.getWorkDate());
            LocalTime shiftEnd = getScheduleEndTime(schedule, entity.getWorkDate());
            isNightShift = breakCalculator.isNightShift(shiftStart, shiftEnd, breakConfig);

            // Tính effective break (với min/max capping)
            effectiveBreakMinutes = breakCalculator.calculateEffectiveBreakMinutes(
                    actualBreakMinutes, breakConfig, workingHours, isNightShift);

            // Lưu break info vào entity
            entity.setTotalBreakMinutes(actualBreakMinutes);
            entity.setEffectiveBreakMinutes(effectiveBreakMinutes);
            entity.setBreakType(breakConfig.getBreakType());

            // Kiểm tra break compliance
            String locale = breakConfig.getLocale() != null ? breakConfig.getLocale() : "ja";
            int legalMinimum = breakCalculator.getLegalMinimumBreak(locale, workingHours, isNightShift);
            entity.setBreakCompliant(actualBreakMinutes >= legalMinimum);
        } else {
            // Không có break config hoặc break không được bật - sử dụng default
            actualBreakMinutes = getBreakMinutes(schedule, entity.getWorkDate());
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
            if (netWorkingMinutes < 0)
                netWorkingMinutes = 0;
        }
        entity.setWorkingMinutes(netWorkingMinutes);

        // Tính số phút về sớm
        LocalTime scheduleEndTime = getScheduleEndTime(schedule, entity.getWorkDate());
        if (scheduleEndTime != null && config != null) {
            int graceMinutes = config.getEarlyLeaveGraceMinutes() != null ? config.getEarlyLeaveGraceMinutes() : 0;
            LocalTime graceStartTime = scheduleEndTime.minusMinutes(graceMinutes);

            LocalTime checkOutLocalTime = checkOut.toLocalTime();
            if (checkOutLocalTime.isBefore(graceStartTime)) {
                int earlyMinutes = (int) ChronoUnit.MINUTES.between(checkOutLocalTime, scheduleEndTime);
                entity.setEarlyLeaveMinutes(earlyMinutes);
            } else {
                entity.setEarlyLeaveMinutes(0);
            }
        } else {
            entity.setEarlyLeaveMinutes(0);
        }

        // Tính số phút tăng ca (làm việc vượt quá giờ chuẩn)
        int standardMinutes = getStandardWorkingMinutes(schedule, entity.getWorkDate());
        if (netWorkingMinutes > standardMinutes) {
            entity.setOvertimeMinutes(netWorkingMinutes - standardMinutes);
        } else {
            entity.setOvertimeMinutes(0);
        }
    }

    /**
     * Lấy giờ bắt đầu làm việc từ schedule
     */
    private LocalTime getScheduleStartTime(WorkScheduleResponse schedule, LocalDate date) {
        if (schedule == null || schedule.getScheduleData() == null) {
            return LocalTime.of(9, 0); // Default
        }

        WorkScheduleData data = schedule.getScheduleData();

        if (schedule.getType() == ScheduleType.FIXED) {
            return data.getDefaultStartTime() != null ? data.getDefaultStartTime() : LocalTime.of(9, 0);
        }

        if (schedule.getType() == ScheduleType.FLEXIBLE && data.getDailySchedules() != null) {
            String dayKey = date.getDayOfWeek().name();
            WorkScheduleData.DailySchedule daily = data.getDailySchedules().get(dayKey);
            if (daily != null && daily.getStartTime() != null) {
                return daily.getStartTime();
            }
        }

        return data.getDefaultStartTime() != null ? data.getDefaultStartTime() : LocalTime.of(9, 0);
    }

    /**
     * Lấy giờ kết thúc làm việc từ schedule
     */
    private LocalTime getScheduleEndTime(WorkScheduleResponse schedule, LocalDate date) {
        if (schedule == null || schedule.getScheduleData() == null) {
            return LocalTime.of(18, 0); // Default
        }

        WorkScheduleData data = schedule.getScheduleData();

        if (schedule.getType() == ScheduleType.FIXED) {
            return data.getDefaultEndTime() != null ? data.getDefaultEndTime() : LocalTime.of(18, 0);
        }

        if (schedule.getType() == ScheduleType.FLEXIBLE && data.getDailySchedules() != null) {
            String dayKey = date.getDayOfWeek().name();
            WorkScheduleData.DailySchedule daily = data.getDailySchedules().get(dayKey);
            if (daily != null && daily.getEndTime() != null) {
                return daily.getEndTime();
            }
        }

        return data.getDefaultEndTime() != null ? data.getDefaultEndTime() : LocalTime.of(18, 0);
    }

    /**
     * Lấy số phút nghỉ từ schedule
     */
    private int getBreakMinutes(WorkScheduleResponse schedule, LocalDate date) {
        if (schedule == null || schedule.getScheduleData() == null) {
            return 60; // Default 1 hour
        }

        WorkScheduleData data = schedule.getScheduleData();

        if (schedule.getType() == ScheduleType.FLEXIBLE && data.getDailySchedules() != null) {
            String dayKey = date.getDayOfWeek().name();
            WorkScheduleData.DailySchedule daily = data.getDailySchedules().get(dayKey);
            if (daily != null && daily.getBreakMinutes() != null) {
                return daily.getBreakMinutes();
            }
        }

        return data.getDefaultBreakMinutes() != null ? data.getDefaultBreakMinutes() : 60;
    }

    /**
     * Lấy số phút làm việc chuẩn trong ngày
     */
    private int getStandardWorkingMinutes(WorkScheduleResponse schedule, LocalDate date) {
        LocalTime startTime = getScheduleStartTime(schedule, date);
        LocalTime endTime = getScheduleEndTime(schedule, date);
        int breakMinutes = getBreakMinutes(schedule, date);

        if (startTime != null && endTime != null) {
            int totalMinutes = (int) ChronoUnit.MINUTES.between(startTime, endTime);
            return Math.max(0, totalMinutes - breakMinutes);
        }

        return 8 * 60; // Default 8 hours
    }

    // ==================== Break Management ====================

    @Override
    @Transactional
    public AttendanceRecordResponse startBreak(Long employeeId, StartBreakRequest request) {
        log.info("Start break for employee: {}", employeeId);

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Kiểm tra user tồn tại
        UserEntity user = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new NotFoundException(
                        "Employee not found with id: " + employeeId,
                        ErrorCode.USER_NOT_FOUND));

        // Lấy attendance record hôm nay
        AttendanceRecordEntity attendance = attendanceRecordRepository
                .findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, today)
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
        BreakConfig breakConfig = companySettingsService.getBreakConfig(user.getCompanyId());

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
        long breakCount = breakRecordRepository.countByAttendanceRecordIdAndDeletedFalse(attendance.getId());

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
        breakRecord.setCompanyId(user.getCompanyId());
        breakRecord.setWorkDate(today);
        breakRecord.setBreakNumber(nextBreakNumber);
        breakRecord.setBreakStart(now);
        breakRecord.setNotes(request != null ? request.getNotes() : null);

        breakRecordRepository.save(breakRecord);

        log.info("Break started for employee: {} at {}, break number: {}", employeeId, now, nextBreakNumber);

        // Return full response
        return buildFullResponse(attendance);
    }

    @Override
    @Transactional
    public AttendanceRecordResponse endBreak(Long employeeId, Long breakRecordId) {
        log.info("End break for employee: {}, breakRecordId: {}", employeeId, breakRecordId);

        LocalDateTime now = LocalDateTime.now();

        // Kiểm tra user tồn tại
        UserEntity user = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new NotFoundException(
                        "Employee not found with id: " + employeeId,
                        ErrorCode.USER_NOT_FOUND));

        // Lấy break record
        BreakRecordEntity breakRecord = breakRecordRepository.findByIdAndDeletedFalse(breakRecordId)
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

        // Lấy break config
        BreakConfig breakConfig = companySettingsService.getBreakConfig(user.getCompanyId());

        // Update break record
        breakRecord.setBreakEnd(now);

        // Calculate actual break minutes
        long actualMinutes = ChronoUnit.MINUTES.between(breakRecord.getBreakStart(), now);
        breakRecord.setActualBreakMinutes((int) actualMinutes);

        // Calculate effective break minutes (apply min/max settings)
        int effectiveMinutes = (int) actualMinutes;

        if (breakConfig != null) {
            Integer minBreak = breakConfig.getMinimumBreakMinutes();
            Integer maxBreak = breakConfig.getMaximumBreakMinutes();

            if (minBreak != null && effectiveMinutes < minBreak) {
                effectiveMinutes = minBreak;
            }

            if (maxBreak != null && effectiveMinutes > maxBreak) {
                effectiveMinutes = maxBreak;
            }
        }

        breakRecord.setEffectiveBreakMinutes(effectiveMinutes);

        breakRecordRepository.save(breakRecord);

        log.info("Break ended for employee: {} at {}, actual: {} min, effective: {} min",
                employeeId, now, actualMinutes, effectiveMinutes);

        // Lấy attendance record để return full response
        AttendanceRecordEntity attendance = attendanceRecordRepository
                .findByIdAndDeletedFalse(breakRecord.getAttendanceRecordId())
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
        List<BreakRecordEntity> breakRecords = breakRecordRepository
                .findByAttendanceRecordIdAndDeletedFalse(entity.getId());

        // Lấy shift info
        ShiftInfoResponse shiftInfo = getShiftInfo(entity.getEmployeeId(), entity.getWorkDate());

        // Lấy configs
        AttendanceConfig attendanceConfig = companySettingsService.getAttendanceConfig(entity.getCompanyId());
        BreakConfig breakConfig = companySettingsService.getBreakConfig(entity.getCompanyId());

        return attendanceMapper.toFullResponse(entity, employeeName, breakRecords, shiftInfo,
                attendanceConfig, breakConfig);
    }

    /**
     * Get shift info for employee on specific date
     */
    private ShiftInfoResponse getShiftInfo(Long employeeId, LocalDate date) {
        List<ShiftAssignmentEntity> assignments = shiftAssignmentRepository
                .findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, date);

        if (assignments.isEmpty()) {
            return null;
        }

        ShiftAssignmentEntity assignment = assignments.get(0);
        Optional<ShiftTemplateEntity> templateOpt = shiftTemplateRepository
                .findByIdAndDeletedFalse(assignment.getShiftTemplateId());

        if (templateOpt.isEmpty()) {
            return null;
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
}
