package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.BreakPeriod;
import com.tamabee.api_hr.dto.request.StartBreakRequest;
import com.tamabee.api_hr.dto.response.BreakRecordResponse;
import com.tamabee.api_hr.dto.response.BreakSummaryResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.BreakRecordRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.calculator.IBreakCalculator;
import com.tamabee.api_hr.service.calculator.LegalBreakRequirements;
import com.tamabee.api_hr.service.company.IBreakService;
import com.tamabee.api_hr.service.company.ICompanySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementation quản lý giờ giải lao.
 * Hỗ trợ ghi nhận break start/end, validation, và tính toán legal minimum.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BreakServiceImpl implements IBreakService {

    private final BreakRecordRepository breakRecordRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;
    private final ICompanySettingsService companySettingsService;
    private final IBreakCalculator breakCalculator;
    private final LegalBreakRequirements legalBreakRequirements;

    // ==================== Break Recording ====================

    @Override
    @Transactional
    public BreakRecordResponse startBreak(Long employeeId, Long companyId, StartBreakRequest request) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Lấy cấu hình break
        BreakConfig config = companySettingsService.getBreakConfig(companyId);

        // Kiểm tra break có được bật không
        if (!Boolean.TRUE.equals(config.getBreakEnabled())) {
            throw new BadRequestException("Giờ giải lao không được bật cho công ty này", ErrorCode.INVALID_CONFIG);
        }

        // Kiểm tra fixed break mode - không cần tracking
        if (Boolean.TRUE.equals(config.getFixedBreakMode())) {
            throw new BadRequestException("Công ty sử dụng fixed break mode, không cần ghi nhận giờ giải lao",
                    ErrorCode.INVALID_CONFIG);
        }

        // Kiểm tra break tracking có được bật không
        if (!Boolean.TRUE.equals(config.getBreakTrackingEnabled())) {
            throw new BadRequestException("Tracking giờ giải lao không được bật", ErrorCode.INVALID_CONFIG);
        }

        // Tìm bản ghi chấm công hôm nay
        AttendanceRecordEntity attendance = attendanceRecordRepository
                .findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, today)
                .orElseThrow(() -> new BadRequestException("Chưa check-in, không thể bắt đầu giải lao",
                        ErrorCode.NOT_CHECKED_IN));

        // Kiểm tra đã check-out chưa
        if (attendance.getOriginalCheckOut() != null) {
            throw new BadRequestException("Đã check-out, không thể bắt đầu giải lao",
                    ErrorCode.ALREADY_CHECKED_OUT);
        }

        // Kiểm tra có break đang active không (breakEnd is null)
        Optional<BreakRecordEntity> activeBreak = breakRecordRepository
                .findActiveBreakByEmployeeIdAndWorkDate(employeeId, today);
        if (activeBreak.isPresent()) {
            throw new BadRequestException("Đã có giờ giải lao đang diễn ra", ErrorCode.BREAK_ALREADY_ACTIVE);
        }

        // Kiểm tra số lần break đã đạt giới hạn chưa
        int maxBreaksPerDay = config.getMaxBreaksPerDay() != null
                ? config.getMaxBreaksPerDay()
                : 3;
        long currentBreakCount = breakRecordRepository.countByAttendanceRecordIdAndDeletedFalse(attendance.getId());
        if (currentBreakCount >= maxBreaksPerDay) {
            throw new BadRequestException("Đã đạt số lần giải lao tối đa trong ngày", ErrorCode.MAX_BREAKS_REACHED);
        }

        // Kiểm tra break mới không overlap với các completed breaks
        validateNoOverlappingBreaks(attendance.getId(), now);

        // Lấy breakNumber tiếp theo = max + 1
        Integer maxBreakNumber = breakRecordRepository.findMaxBreakNumberByAttendanceRecordId(attendance.getId());
        int nextBreakNumber = (maxBreakNumber != null ? maxBreakNumber : 0) + 1;

        // Tạo bản ghi break mới
        BreakRecordEntity breakRecord = new BreakRecordEntity();
        breakRecord.setAttendanceRecordId(attendance.getId());
        breakRecord.setEmployeeId(employeeId);
        breakRecord.setCompanyId(companyId);
        breakRecord.setWorkDate(today);
        breakRecord.setBreakStart(now);
        breakRecord.setBreakNumber(nextBreakNumber);
        breakRecord.setNotes(request != null ? request.getNotes() : null);

        breakRecord = breakRecordRepository.save(breakRecord);
        log.info("Nhân viên {} bắt đầu giải lao #{} lúc {}", employeeId, nextBreakNumber, now);

        return toResponse(breakRecord);
    }

    @Override
    @Transactional
    public BreakRecordResponse endBreak(Long employeeId, Long breakRecordId) {
        LocalDateTime now = LocalDateTime.now();

        // Tìm bản ghi break
        BreakRecordEntity breakRecord = breakRecordRepository.findByIdAndDeletedFalse(breakRecordId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bản ghi giải lao", ErrorCode.NOT_FOUND));

        // Kiểm tra quyền sở hữu
        if (!breakRecord.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException("Không có quyền kết thúc giờ giải lao này", ErrorCode.ACCESS_DENIED);
        }

        // Kiểm tra đã kết thúc chưa
        if (breakRecord.getBreakEnd() != null) {
            throw new BadRequestException("Giờ giải lao đã kết thúc", ErrorCode.BAD_REQUEST);
        }

        // Kiểm tra break start có tồn tại không
        if (breakRecord.getBreakStart() == null) {
            throw new BadRequestException("Giờ giải lao chưa bắt đầu", ErrorCode.BAD_REQUEST);
        }

        // Lấy cấu hình break
        BreakConfig config = companySettingsService.getBreakConfig(breakRecord.getCompanyId());

        // Cập nhật thời gian kết thúc
        breakRecord.setBreakEnd(now);

        // Tính thời gian giải lao thực tế
        int actualMinutes = breakCalculator.calculateBreakMinutes(breakRecord.getBreakStart(), now);
        breakRecord.setActualBreakMinutes(actualMinutes);

        // Kiểm tra night shift
        boolean isNightShift = isNightShiftBreak(breakRecord.getBreakStart(), config);

        // Tính thời gian giải lao hiệu lực (sau khi áp dụng min/max)
        int workingHours = 8; // Mặc định 8 giờ, có thể tính từ attendance record
        int effectiveMinutes = breakCalculator.calculateEffectiveBreakMinutes(
                actualMinutes, config, workingHours, isNightShift);
        breakRecord.setEffectiveBreakMinutes(effectiveMinutes);

        breakRecord = breakRecordRepository.save(breakRecord);

        // Cập nhật attendance record
        updateAttendanceBreakInfo(breakRecord.getAttendanceRecordId(), config);

        log.info("Nhân viên {} kết thúc giải lao lúc {}, thời gian: {} phút",
                employeeId, now, actualMinutes);

        return toResponse(breakRecord);
    }

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public List<BreakRecordResponse> getBreakRecordsByAttendance(Long attendanceRecordId) {
        List<BreakRecordEntity> records = breakRecordRepository
                .findByAttendanceRecordIdAndDeletedFalse(attendanceRecordId);

        return records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BreakSummaryResponse getBreakSummary(Long employeeId, LocalDate date) {
        List<BreakRecordEntity> records = breakRecordRepository
                .findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, date);

        // Lấy thông tin nhân viên
        String employeeName = getEmployeeName(employeeId);

        // Lấy attendance record để biết company
        AttendanceRecordEntity attendance = attendanceRecordRepository
                .findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, date)
                .orElse(null);

        if (attendance == null) {
            return BreakSummaryResponse.builder()
                    .employeeId(employeeId)
                    .employeeName(employeeName)
                    .workDate(date)
                    .totalActualBreakMinutes(0)
                    .totalEffectiveBreakMinutes(0)
                    .breakCount(0)
                    .breakCompliant(true)
                    .breakRecords(List.of())
                    .build();
        }

        // Lấy cấu hình break
        BreakConfig config = companySettingsService.getBreakConfig(attendance.getCompanyId());

        // Tính tổng thời gian giải lao
        int totalActual = records.stream()
                .mapToInt(r -> r.getActualBreakMinutes() != null ? r.getActualBreakMinutes() : 0)
                .sum();

        int totalEffective = records.stream()
                .mapToInt(r -> r.getEffectiveBreakMinutes() != null ? r.getEffectiveBreakMinutes() : 0)
                .sum();

        // Tính minimum break required
        int workingHours = attendance.getWorkingMinutes() != null
                ? attendance.getWorkingMinutes() / 60
                : 8;
        int minimumRequired = getEffectiveMinimumBreak(attendance.getCompanyId(), workingHours);

        // Kiểm tra compliance
        boolean compliant = totalEffective >= minimumRequired;

        List<BreakRecordResponse> breakResponses = records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return BreakSummaryResponse.builder()
                .employeeId(employeeId)
                .employeeName(employeeName)
                .workDate(date)
                .totalActualBreakMinutes(totalActual)
                .totalEffectiveBreakMinutes(totalEffective)
                .breakCount(records.size())
                .breakType(config.getBreakType())
                .breakCompliant(compliant)
                .minimumBreakRequired(minimumRequired)
                .breakRecords(breakResponses)
                .build();
    }

    // ==================== Validation ====================

    /**
     * Kiểm tra thời gian break mới không overlap với các completed breaks.
     * Một break được coi là overlap nếu thời gian bắt đầu nằm trong khoảng
     * [breakStart, breakEnd] của break khác.
     */
    private void validateNoOverlappingBreaks(Long attendanceRecordId, LocalDateTime newBreakStart) {
        List<BreakRecordEntity> completedBreaks = breakRecordRepository
                .findCompletedBreaksByAttendanceRecordId(attendanceRecordId);

        for (BreakRecordEntity existingBreak : completedBreaks) {
            if (existingBreak.getBreakStart() != null && existingBreak.getBreakEnd() != null) {
                // Check if newBreakStart falls within existing break period
                if (!newBreakStart.isBefore(existingBreak.getBreakStart())
                        && !newBreakStart.isAfter(existingBreak.getBreakEnd())) {
                    throw new BadRequestException(
                            "Giờ giải lao bị trùng với giờ giải lao khác",
                            ErrorCode.BREAK_OVERLAP);
                }
            }
        }
    }

    @Override
    public void validateBreakDuration(Long companyId, Integer breakMinutes) {
        if (breakMinutes == null || breakMinutes < 0) {
            throw new BadRequestException("Thời gian giải lao không hợp lệ", ErrorCode.BAD_REQUEST);
        }

        BreakConfig config = companySettingsService.getBreakConfig(companyId);

        // Kiểm tra minimum
        if (config.getMinimumBreakMinutes() != null && breakMinutes < config.getMinimumBreakMinutes()) {
            throw new BadRequestException("Thời gian giải lao dưới mức tối thiểu", ErrorCode.BAD_REQUEST);
        }

        // Kiểm tra maximum
        if (config.getMaximumBreakMinutes() != null && breakMinutes > config.getMaximumBreakMinutes()) {
            throw new BadRequestException("Thời gian giải lao vượt quá mức tối đa", ErrorCode.BAD_REQUEST);
        }
    }

    @Override
    public Integer getLegalMinimumBreak(String locale, Integer workingHours) {
        if (workingHours == null || workingHours <= 0) {
            return 0;
        }
        return legalBreakRequirements.getMinimumBreak(locale, workingHours, false);
    }

    @Override
    public Integer getEffectiveMinimumBreak(Long companyId, Integer workingHours) {
        if (workingHours == null || workingHours <= 0) {
            return 0;
        }

        BreakConfig config = companySettingsService.getBreakConfig(companyId);

        // Lấy legal minimum
        int legalMinimum = legalBreakRequirements.getMinimumBreak(
                config.getLocale(), workingHours, false);

        // Nếu sử dụng legal minimum
        if (Boolean.TRUE.equals(config.getUseLegalMinimum())) {
            // Trả về max của legal và company config
            int companyMinimum = config.getMinimumBreakMinutes() != null
                    ? config.getMinimumBreakMinutes()
                    : 0;
            return Math.max(legalMinimum, companyMinimum);
        }

        // Nếu không sử dụng legal minimum, trả về company config
        return config.getMinimumBreakMinutes() != null ? config.getMinimumBreakMinutes() : 0;
    }

    // ==================== Calculation ====================

    @Override
    @Transactional(readOnly = true)
    public Integer calculateTotalBreakMinutes(Long attendanceRecordId) {
        List<BreakRecordEntity> breaks = breakRecordRepository
                .findByAttendanceRecordIdAndDeletedFalse(attendanceRecordId);

        return breaks.stream()
                .mapToInt(b -> b.getActualBreakMinutes() != null ? b.getActualBreakMinutes() : 0)
                .sum();
    }

    // ==================== Fixed Break Mode ====================

    /**
     * Tự động tạo break records cho fixed break mode.
     * Được gọi khi check-out hoặc khi tính payroll.
     */
    @Transactional
    public void autoCreateFixedBreakRecords(Long attendanceRecordId) {
        AttendanceRecordEntity attendance = attendanceRecordRepository.findByIdAndDeletedFalse(attendanceRecordId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bản ghi chấm công",
                        ErrorCode.ATTENDANCE_RECORD_NOT_FOUND));

        BreakConfig config = companySettingsService.getBreakConfig(attendance.getCompanyId());

        // Chỉ áp dụng cho fixed break mode
        if (!Boolean.TRUE.equals(config.getFixedBreakMode())) {
            return;
        }

        // Kiểm tra đã có break records chưa
        List<BreakRecordEntity> existingBreaks = breakRecordRepository
                .findByAttendanceRecordIdAndDeletedFalse(attendanceRecordId);
        if (!existingBreaks.isEmpty()) {
            return; // Đã có break records
        }

        // Tạo break records từ fixed break periods
        List<BreakPeriod> fixedPeriods = config.getFixedBreakPeriods();
        if (fixedPeriods == null || fixedPeriods.isEmpty()) {
            // Sử dụng default break
            createDefaultFixedBreakRecord(attendance, config);
        } else {
            // Tạo từ configured periods
            for (BreakPeriod period : fixedPeriods) {
                createFixedBreakRecord(attendance, period, config);
            }
        }

        // Cập nhật attendance record
        updateAttendanceBreakInfo(attendanceRecordId, config);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Tạo break record mặc định cho fixed break mode
     */
    private void createDefaultFixedBreakRecord(AttendanceRecordEntity attendance, BreakConfig config) {
        int defaultMinutes = config.getDefaultBreakMinutes() != null
                ? config.getDefaultBreakMinutes()
                : 60;

        // Lấy breakNumber tiếp theo
        Integer maxBreakNumber = breakRecordRepository.findMaxBreakNumberByAttendanceRecordId(attendance.getId());
        int nextBreakNumber = (maxBreakNumber != null ? maxBreakNumber : 0) + 1;

        BreakRecordEntity breakRecord = new BreakRecordEntity();
        breakRecord.setAttendanceRecordId(attendance.getId());
        breakRecord.setEmployeeId(attendance.getEmployeeId());
        breakRecord.setCompanyId(attendance.getCompanyId());
        breakRecord.setWorkDate(attendance.getWorkDate());
        breakRecord.setBreakNumber(nextBreakNumber);
        breakRecord.setActualBreakMinutes(defaultMinutes);
        breakRecord.setEffectiveBreakMinutes(defaultMinutes);
        breakRecord.setNotes("Auto-created (fixed break mode)");

        breakRecordRepository.save(breakRecord);
    }

    /**
     * Tạo break record từ fixed break period
     */
    private void createFixedBreakRecord(AttendanceRecordEntity attendance, BreakPeriod period, BreakConfig config) {
        LocalDate workDate = attendance.getWorkDate();

        LocalDateTime breakStart = period.getStartTime() != null
                ? LocalDateTime.of(workDate, period.getStartTime())
                : null;
        LocalDateTime breakEnd = period.getEndTime() != null
                ? LocalDateTime.of(workDate, period.getEndTime())
                : null;

        int durationMinutes = period.getDurationMinutes() != null
                ? period.getDurationMinutes()
                : 60;

        // Lấy breakNumber tiếp theo
        Integer maxBreakNumber = breakRecordRepository.findMaxBreakNumberByAttendanceRecordId(attendance.getId());
        int nextBreakNumber = (maxBreakNumber != null ? maxBreakNumber : 0) + 1;

        BreakRecordEntity breakRecord = new BreakRecordEntity();
        breakRecord.setAttendanceRecordId(attendance.getId());
        breakRecord.setEmployeeId(attendance.getEmployeeId());
        breakRecord.setCompanyId(attendance.getCompanyId());
        breakRecord.setWorkDate(workDate);
        breakRecord.setBreakNumber(nextBreakNumber);
        breakRecord.setBreakStart(breakStart);
        breakRecord.setBreakEnd(breakEnd);
        breakRecord.setActualBreakMinutes(durationMinutes);
        breakRecord.setEffectiveBreakMinutes(durationMinutes);
        breakRecord.setNotes("Auto-created: " + (period.getName() != null ? period.getName() : "Fixed break"));

        breakRecordRepository.save(breakRecord);
    }

    /**
     * Cập nhật thông tin break trong attendance record
     */
    private void updateAttendanceBreakInfo(Long attendanceRecordId, BreakConfig config) {
        AttendanceRecordEntity attendance = attendanceRecordRepository.findByIdAndDeletedFalse(attendanceRecordId)
                .orElse(null);

        if (attendance == null) {
            return;
        }

        List<BreakRecordEntity> breaks = breakRecordRepository
                .findByAttendanceRecordIdAndDeletedFalse(attendanceRecordId);

        // Tính tổng break
        int totalBreak = breaks.stream()
                .mapToInt(b -> b.getActualBreakMinutes() != null ? b.getActualBreakMinutes() : 0)
                .sum();

        int effectiveBreak = breaks.stream()
                .mapToInt(b -> b.getEffectiveBreakMinutes() != null ? b.getEffectiveBreakMinutes() : 0)
                .sum();

        attendance.setTotalBreakMinutes(totalBreak);
        attendance.setEffectiveBreakMinutes(effectiveBreak);
        attendance.setBreakType(config.getBreakType());

        // Kiểm tra compliance
        int workingHours = attendance.getWorkingMinutes() != null
                ? attendance.getWorkingMinutes() / 60
                : 8;
        int minimumRequired = getEffectiveMinimumBreak(attendance.getCompanyId(), workingHours);
        attendance.setBreakCompliant(effectiveBreak >= minimumRequired);

        attendanceRecordRepository.save(attendance);
    }

    /**
     * Kiểm tra break có nằm trong night shift không
     */
    private boolean isNightShiftBreak(LocalDateTime breakStart, BreakConfig config) {
        if (breakStart == null) {
            return false;
        }

        LocalTime breakTime = breakStart.toLocalTime();
        LocalTime nightStart = config.getNightShiftStartTime() != null
                ? config.getNightShiftStartTime()
                : LocalTime.of(22, 0);
        LocalTime nightEnd = config.getNightShiftEndTime() != null
                ? config.getNightShiftEndTime()
                : LocalTime.of(5, 0);

        // Night shift spans midnight
        if (nightStart.isAfter(nightEnd)) {
            return breakTime.isAfter(nightStart) || breakTime.isBefore(nightEnd);
        }

        return breakTime.isAfter(nightStart) && breakTime.isBefore(nightEnd);
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
     * Chuyển entity thành response
     */
    private BreakRecordResponse toResponse(BreakRecordEntity entity) {
        return BreakRecordResponse.builder()
                .id(entity.getId())
                .attendanceRecordId(entity.getAttendanceRecordId())
                .employeeId(entity.getEmployeeId())
                .employeeName(getEmployeeName(entity.getEmployeeId()))
                .companyId(entity.getCompanyId())
                .workDate(entity.getWorkDate())
                .breakNumber(entity.getBreakNumber())
                .breakStart(entity.getBreakStart())
                .breakEnd(entity.getBreakEnd())
                .actualBreakMinutes(entity.getActualBreakMinutes())
                .effectiveBreakMinutes(entity.getEffectiveBreakMinutes())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
