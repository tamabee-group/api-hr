package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.dto.request.AdjustAttendanceRequest;
import com.tamabee.api_hr.dto.request.AttendanceQueryRequest;
import com.tamabee.api_hr.dto.request.CheckInRequest;
import com.tamabee.api_hr.dto.request.CheckOutRequest;
import com.tamabee.api_hr.dto.response.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.AttendanceSummaryResponse;
import com.tamabee.api_hr.dto.response.WorkScheduleResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.ScheduleType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.AttendanceMapper;
import com.tamabee.api_hr.repository.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.BreakRecordRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.calculator.IBreakCalculator;
import com.tamabee.api_hr.service.calculator.ITimeRoundingCalculator;
import com.tamabee.api_hr.service.company.IAttendanceService;
import com.tamabee.api_hr.service.company.ICompanySettingsService;
import com.tamabee.api_hr.service.company.IWorkScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service implementation quản lý chấm công.
 * Áp dụng làm tròn giờ, tính toán giờ làm việc, phát hiện đi muộn/về sớm.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements IAttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final UserRepository userRepository;
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

        // Cập nhật break time nếu có
        if (request.getBreakStartTime() != null || request.getBreakEndTime() != null) {
            updateBreakRecord(entity, request.getBreakRecordId(), request.getBreakStartTime(),
                    request.getBreakEndTime());
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
        return attendanceMapper.toResponse(entity, getEmployeeName(entity.getEmployeeId()));
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceRecordResponse getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date) {
        return attendanceRecordRepository.findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, date)
                .map(entity -> attendanceMapper.toResponse(entity, getEmployeeName(employeeId)))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceRecordResponse> getAttendanceRecords(
            Long companyId, AttendanceQueryRequest request, Pageable pageable) {
        return attendanceRecordRepository.findByCompanyIdAndWorkDateBetween(
                companyId, request.getStartDate(), request.getEndDate(), pageable)
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
        // TODO: Implement device validation logic
        // Hiện tại chấp nhận tất cả device
        return deviceId != null && !deviceId.isBlank();
    }

    @Override
    public boolean validateLocation(Long companyId, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }

        // TODO: Implement geo-fence validation logic
        // Cần có bảng lưu vị trí công ty và bán kính cho phép
        // Hiện tại chấp nhận tất cả vị trí hợp lệ
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
}
