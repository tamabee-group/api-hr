package com.tamabee.api_hr.service.company.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.request.attendance.EndBreakRequest;
import com.tamabee.api_hr.dto.request.attendance.StartBreakRequest;
import com.tamabee.api_hr.dto.response.attendance.BreakRecordResponse;
import com.tamabee.api_hr.dto.response.attendance.BreakSummaryResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.attendance.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.attendance.BreakRecordRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IBreakService;
import com.tamabee.api_hr.service.company.interfaces.ICompanySettingsService;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý giờ giải lao.
 * Logic đơn giản: khi chấm giải lao, thời gian đó bị trừ khỏi giờ làm việc, không tính lương.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BreakServiceImpl implements IBreakService {

    private final BreakRecordRepository breakRecordRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;
    private final ICompanySettingsService companySettingsService;

    // ==================== Break Recording ====================

    @Override
    @Transactional
    public BreakRecordResponse startBreak(Long employeeId, StartBreakRequest request) {
        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        LocalDateTime now = LocalDateTime.now(zone);

        BreakConfig config = companySettingsService.getBreakConfig();

        // Kiểm tra break có được bật không
        if (!Boolean.TRUE.equals(config.getBreakEnabled())) {
            throw new BadRequestException("Giờ giải lao không được bật cho công ty này", ErrorCode.INVALID_CONFIG);
        }

        // Tìm bản ghi chấm công hôm nay
        AttendanceRecordEntity attendance = attendanceRecordRepository
                .findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseThrow(() -> new BadRequestException("Chưa check-in, không thể bắt đầu giải lao",
                        ErrorCode.NOT_CHECKED_IN));

        // Kiểm tra đã check-out chưa
        if (attendance.getOriginalCheckOut() != null) {
            throw new BadRequestException("Đã check-out, không thể bắt đầu giải lao",
                    ErrorCode.ALREADY_CHECKED_OUT);
        }

        // Kiểm tra có break đang active không
        Optional<BreakRecordEntity> activeBreak = breakRecordRepository
                .findActiveBreakByEmployeeIdAndWorkDate(employeeId, today);
        if (activeBreak.isPresent()) {
            throw new BadRequestException("Đã có giờ giải lao đang diễn ra", ErrorCode.BREAK_ALREADY_ACTIVE);
        }

        // Kiểm tra số lần break đã đạt giới hạn chưa
        int maxBreaksPerDay = config.getMaxBreaksPerDay() != null ? config.getMaxBreaksPerDay() : 3;
        long currentBreakCount = breakRecordRepository.countByAttendanceRecordId(attendance.getId());
        if (currentBreakCount >= maxBreaksPerDay) {
            throw new BadRequestException("Đã đạt số lần giải lao tối đa trong ngày", ErrorCode.MAX_BREAKS_REACHED);
        }

        // Kiểm tra break mới không overlap với các completed breaks
        validateNoOverlappingBreaks(attendance.getId(), now);

        // Lấy breakNumber tiếp theo
        Integer maxBreakNumber = breakRecordRepository.findMaxBreakNumberByAttendanceRecordId(attendance.getId());
        int nextBreakNumber = (maxBreakNumber != null ? maxBreakNumber : 0) + 1;

        // Tạo bản ghi break mới
        BreakRecordEntity breakRecord = new BreakRecordEntity();
        breakRecord.setAttendanceRecordId(attendance.getId());
        breakRecord.setEmployeeId(employeeId);
        breakRecord.setWorkDate(today);
        breakRecord.setBreakStart(now);
        breakRecord.setBreakNumber(nextBreakNumber);
        breakRecord.setNotes(request != null ? request.getNotes() : null);

        if (request != null) {
            breakRecord.setBreakStartLatitude(request.getLatitude());
            breakRecord.setBreakStartLongitude(request.getLongitude());
        }

        breakRecord = breakRecordRepository.save(breakRecord);
        log.info("Nhân viên {} bắt đầu giải lao #{} lúc {}", employeeId, nextBreakNumber, now);

        return toResponse(breakRecord);
    }

    @Override
    @Transactional
    public BreakRecordResponse endBreak(Long employeeId, Long breakRecordId, EndBreakRequest request) {
        LocalDateTime now = LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));

        BreakRecordEntity breakRecord = breakRecordRepository.findById(breakRecordId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bản ghi giải lao", ErrorCode.NOT_FOUND));

        if (!breakRecord.getEmployeeId().equals(employeeId)) {
            throw new BadRequestException("Không có quyền kết thúc giờ giải lao này", ErrorCode.ACCESS_DENIED);
        }

        if (breakRecord.getBreakEnd() != null) {
            throw new BadRequestException("Giờ giải lao đã kết thúc", ErrorCode.BAD_REQUEST);
        }

        if (breakRecord.getBreakStart() == null) {
            throw new BadRequestException("Giờ giải lao chưa bắt đầu", ErrorCode.BAD_REQUEST);
        }

        // Cập nhật thời gian kết thúc
        breakRecord.setBreakEnd(now);

        // Tính thời gian giải lao thực tế (phút)
        int actualMinutes = (int) ChronoUnit.MINUTES.between(breakRecord.getBreakStart(), now);
        breakRecord.setActualBreakMinutes(actualMinutes);
        // Effective = actual (đơn giản, không capping)
        breakRecord.setEffectiveBreakMinutes(actualMinutes);

        if (request != null) {
            breakRecord.setBreakEndLatitude(request.getLatitude());
            breakRecord.setBreakEndLongitude(request.getLongitude());
        }

        breakRecord = breakRecordRepository.save(breakRecord);

        // Cập nhật tổng break trong attendance record
        updateAttendanceBreakInfo(breakRecord.getAttendanceRecordId());

        log.info("Nhân viên {} kết thúc giải lao lúc {}, thời gian: {} phút",
                employeeId, now, actualMinutes);

        return toResponse(breakRecord);
    }

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public List<BreakRecordResponse> getBreakRecordsByAttendance(Long attendanceRecordId) {
        List<BreakRecordEntity> records = breakRecordRepository
                .findByAttendanceRecordId(attendanceRecordId);

        return records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BreakSummaryResponse getBreakSummary(Long employeeId, LocalDate date) {
        List<BreakRecordEntity> records = breakRecordRepository
                .findByEmployeeIdAndWorkDate(employeeId, date);

        String employeeName = getEmployeeName(employeeId);

        int totalMinutes = records.stream()
                .mapToInt(r -> r.getActualBreakMinutes() != null ? r.getActualBreakMinutes() : 0)
                .sum();

        List<BreakRecordResponse> breakResponses = records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return BreakSummaryResponse.builder()
                .employeeId(employeeId)
                .employeeName(employeeName)
                .workDate(date)
                .totalActualBreakMinutes(totalMinutes)
                .totalEffectiveBreakMinutes(totalMinutes)
                .breakCount(records.size())
                .breakRecords(breakResponses)
                .build();
    }

    // ==================== Validation ====================

    @Override
    public void validateBreakDuration(Integer breakMinutes) {
        if (breakMinutes == null || breakMinutes < 0) {
            throw new BadRequestException("Thời gian giải lao không hợp lệ", ErrorCode.BAD_REQUEST);
        }
    }

    @Override
    public Integer getLegalMinimumBreak(String region, Integer workingHours) {
        return 0; // Không còn dùng legal minimum
    }

    @Override
    public Integer getEffectiveMinimumBreak(Integer workingHours) {
        return 0; // Không còn dùng minimum break
    }

    // ==================== Calculation ====================

    @Override
    @Transactional(readOnly = true)
    public Integer calculateTotalBreakMinutes(Long attendanceRecordId) {
        List<BreakRecordEntity> breaks = breakRecordRepository
                .findByAttendanceRecordId(attendanceRecordId);

        return breaks.stream()
                .mapToInt(b -> b.getActualBreakMinutes() != null ? b.getActualBreakMinutes() : 0)
                .sum();
    }

    // ==================== Private Helper Methods ====================

    private void validateNoOverlappingBreaks(Long attendanceRecordId, LocalDateTime newBreakStart) {
        List<BreakRecordEntity> completedBreaks = breakRecordRepository
                .findCompletedBreaksByAttendanceRecordId(attendanceRecordId);

        for (BreakRecordEntity existingBreak : completedBreaks) {
            if (existingBreak.getBreakStart() != null && existingBreak.getBreakEnd() != null) {
                if (!newBreakStart.isBefore(existingBreak.getBreakStart())
                        && !newBreakStart.isAfter(existingBreak.getBreakEnd())) {
                    throw new BadRequestException(
                            "Giờ giải lao bị trùng với giờ giải lao khác",
                            ErrorCode.BREAK_OVERLAP);
                }
            }
        }
    }

    /**
     * Cập nhật tổng break trong attendance record.
     * Thời gian giải lao luôn bị trừ khỏi giờ làm việc.
     */
    private void updateAttendanceBreakInfo(Long attendanceRecordId) {
        AttendanceRecordEntity attendance = attendanceRecordRepository.findById(attendanceRecordId)
                .orElse(null);

        if (attendance == null) {
            return;
        }

        List<BreakRecordEntity> breaks = breakRecordRepository
                .findByAttendanceRecordId(attendanceRecordId);

        int totalBreak = breaks.stream()
                .mapToInt(b -> b.getActualBreakMinutes() != null ? b.getActualBreakMinutes() : 0)
                .sum();

        attendance.setTotalBreakMinutes(totalBreak);
        attendance.setEffectiveBreakMinutes(totalBreak);
        attendance.setBreakCompliant(true);

        attendanceRecordRepository.save(attendance);
    }

    private String getEmployeeName(Long employeeId) {
        return userRepository.findWithProfileByIdAndDeletedFalse(employeeId)
                .map(user -> user.getProfile() != null ? user.getProfile().getName() : user.getEmail())
                .orElse("Unknown");
    }

    private BreakRecordResponse toResponse(BreakRecordEntity entity) {
        return BreakRecordResponse.builder()
                .id(entity.getId())
                .breakNumber(entity.getBreakNumber())
                .breakStart(entity.getBreakStart())
                .breakEnd(entity.getBreakEnd())
                .actualBreakMinutes(entity.getActualBreakMinutes())
                .effectiveBreakMinutes(entity.getEffectiveBreakMinutes())
                .notes(entity.getNotes())
                .isActive(entity.getBreakEnd() == null)
                .breakStartLatitude(entity.getBreakStartLatitude())
                .breakStartLongitude(entity.getBreakStartLongitude())
                .breakEndLatitude(entity.getBreakEndLatitude())
                .breakEndLongitude(entity.getBreakEndLongitude())
                .build();
    }
}
