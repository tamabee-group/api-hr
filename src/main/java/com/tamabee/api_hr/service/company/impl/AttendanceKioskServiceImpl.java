package com.tamabee.api_hr.service.company.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.dto.request.attendance.CheckInRequest;
import com.tamabee.api_hr.dto.request.attendance.CheckOutRequest;
import com.tamabee.api_hr.dto.request.attendance.CreateAttendanceKioskRequest;
import com.tamabee.api_hr.dto.request.attendance.KioskCheckInRequest;
import com.tamabee.api_hr.dto.request.attendance.KioskLoginRequest;
import com.tamabee.api_hr.dto.request.attendance.StartBreakRequest;
import com.tamabee.api_hr.dto.request.attendance.UpdateAttendanceKioskRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceKioskResponse;
import com.tamabee.api_hr.dto.response.attendance.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.attendance.KioskActivityResponse;
import com.tamabee.api_hr.dto.response.attendance.KioskEmployeeStatusResponse;
import com.tamabee.api_hr.dto.response.attendance.KioskLoginResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.entity.company.AttendanceKioskEntity;
import com.tamabee.api_hr.entity.company.AttendanceLocationEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.CheckInSource;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.AttendanceKioskMapper;
import com.tamabee.api_hr.repository.attendance.AttendanceKioskRepository;
import com.tamabee.api_hr.repository.attendance.AttendanceLocationRepository;
import com.tamabee.api_hr.repository.attendance.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.attendance.BreakRecordRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceKioskService;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceService;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý máy chấm công cố định (kiosk).
 * Hỗ trợ CRUD kiosk, đăng nhập bằng PIN, chấm công bằng mã nhân viên.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceKioskServiceImpl implements IAttendanceKioskService {

    private final AttendanceKioskRepository kioskRepository;
    private final AttendanceLocationRepository locationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final UserRepository userRepository;
    private final AttendanceKioskMapper kioskMapper;
    private final IAttendanceService attendanceService;

    // ==================== CRUD (Admin) ====================

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceKioskResponse> getKiosks(Pageable pageable) {
        return kioskRepository.findByDeletedFalse(pageable)
                .map(entity -> kioskMapper.toResponse(entity, getLocationName(entity.getLocationId())));
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceKioskResponse getKiosk(Long id) {
        AttendanceKioskEntity entity = findKiosk(id);
        return kioskMapper.toResponse(entity, getLocationName(entity.getLocationId()));
    }

    @Override
    @Transactional
    public AttendanceKioskResponse createKiosk(CreateAttendanceKioskRequest request) {
        // Validate location tồn tại
        validateLocation(request.getLocationId());

        // Validate PIN unique
        if (kioskRepository.existsByPinCodeAndDeletedFalse(request.getPinCode())) {
            throw new BadRequestException("Mã PIN đã được sử dụng", ErrorCode.KIOSK_PIN_EXISTS);
        }

        AttendanceKioskEntity entity = kioskMapper.toEntity(request);
        entity = kioskRepository.save(entity);

        log.info("Đã tạo kiosk: {} (id={})", entity.getName(), entity.getId());
        return kioskMapper.toResponse(entity, getLocationName(entity.getLocationId()));
    }

    @Override
    @Transactional
    public AttendanceKioskResponse updateKiosk(Long id, UpdateAttendanceKioskRequest request) {
        AttendanceKioskEntity entity = findKiosk(id);

        // Validate location nếu có thay đổi
        if (request.getLocationId() != null) {
            validateLocation(request.getLocationId());
        }

        // Validate PIN unique nếu có thay đổi
        if (request.getPinCode() != null && !request.getPinCode().equals(entity.getPinCode())) {
            if (kioskRepository.existsByPinCodeAndDeletedFalse(request.getPinCode())) {
                throw new BadRequestException("Mã PIN đã được sử dụng", ErrorCode.KIOSK_PIN_EXISTS);
            }
        }

        kioskMapper.updateEntity(entity, request);
        entity = kioskRepository.save(entity);

        log.info("Đã cập nhật kiosk: {} (id={})", entity.getName(), id);
        return kioskMapper.toResponse(entity, getLocationName(entity.getLocationId()));
    }

    @Override
    @Transactional
    public void deleteKiosk(Long id) {
        AttendanceKioskEntity entity = findKiosk(id);
        entity.setDeleted(true);
        kioskRepository.save(entity);
        log.info("Đã xóa kiosk: {} (id={})", entity.getName(), id);
    }

    // ==================== Kiosk Operations ====================

    @Override
    @Transactional
    public KioskLoginResponse login(KioskLoginRequest request) {
        AttendanceKioskEntity kiosk = kioskRepository
                .findByPinCodeAndDeletedFalseAndIsActiveTrue(request.getPinCode())
                .orElseThrow(() -> new BadRequestException("Mã PIN không đúng", ErrorCode.KIOSK_INVALID_PIN));

        // Cập nhật thời gian hoạt động
        kiosk.setLastActiveAt(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
        kioskRepository.save(kiosk);

        AttendanceLocationEntity location = locationRepository.findByIdAndDeletedFalse(kiosk.getLocationId())
                .orElseThrow(() -> NotFoundException.location(kiosk.getLocationId()));

        return KioskLoginResponse.builder()
                .kioskId(kiosk.getId())
                .kioskName(kiosk.getName())
                .locationId(location.getId())
                .locationName(location.getName())
                .locationAddress(location.getAddress())
                .build();
    }

    @Override
    @Transactional
    public AttendanceRecordResponse kioskCheckIn(Long kioskId, KioskCheckInRequest request) {
        AttendanceKioskEntity kiosk = findActiveKiosk(kioskId);
        UserEntity employee = findEmployeeByCode(request.getEmployeeCode());
        AttendanceLocationEntity location = findLocation(kiosk.getLocationId());

        // Dùng tọa độ của location thay vì GPS
        CheckInRequest checkInRequest = CheckInRequest.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();

        AttendanceRecordResponse response = attendanceService.checkIn(employee.getId(), checkInRequest);

        // Cập nhật source và kioskId
        updateCheckInSource(employee.getId(), kioskId);

        log.info("Kiosk check-in: nhân viên {} qua kiosk {}", employee.getEmployeeCode(), kiosk.getName());
        return response;
    }

    @Override
    @Transactional
    public AttendanceRecordResponse kioskCheckOut(Long kioskId, KioskCheckInRequest request) {
        AttendanceKioskEntity kiosk = findActiveKiosk(kioskId);
        UserEntity employee = findEmployeeByCode(request.getEmployeeCode());
        AttendanceLocationEntity location = findLocation(kiosk.getLocationId());

        CheckOutRequest checkOutRequest = CheckOutRequest.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();

        AttendanceRecordResponse response = attendanceService.checkOut(employee.getId(), checkOutRequest);

        log.info("Kiosk check-out: nhân viên {} qua kiosk {}", employee.getEmployeeCode(), kiosk.getName());
        return response;
    }

    @Override
    @Transactional
    public AttendanceRecordResponse kioskStartBreak(Long kioskId, KioskCheckInRequest request) {
        AttendanceKioskEntity kiosk = findActiveKiosk(kioskId);
        UserEntity employee = findEmployeeByCode(request.getEmployeeCode());

        AttendanceRecordResponse response = attendanceService.startBreak(employee.getId(), new StartBreakRequest());

        log.info("Kiosk break start: nhân viên {} qua kiosk {}", employee.getEmployeeCode(), kiosk.getName());
        return response;
    }

    @Override
    @Transactional
    public AttendanceRecordResponse kioskEndBreak(Long kioskId, KioskCheckInRequest request) {
        AttendanceKioskEntity kiosk = findActiveKiosk(kioskId);
        UserEntity employee = findEmployeeByCode(request.getEmployeeCode());

        // Tìm break record đang active
        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        log.info("[TIMEZONE-DEBUG] kioskEndBreak - region={}, zone={}, today={}",
                RegionContext.getCurrentRegion(), zone, today);

        BreakRecordEntity activeBreak = breakRecordRepository
                .findActiveBreakByEmployeeIdAndWorkDate(employee.getId(), today)
                .orElseThrow(() -> new BadRequestException(
                        "Không có giờ giải lao đang diễn ra", ErrorCode.NO_ACTIVE_BREAK));

        AttendanceRecordResponse response = attendanceService.endBreak(employee.getId(), activeBreak.getId(), null);

        log.info("Kiosk break end: nhân viên {} qua kiosk {}", employee.getEmployeeCode(), kiosk.getName());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KioskActivityResponse> getRecentActivities(Long kioskId, int limit) {
        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        log.info("[TIMEZONE-DEBUG] getRecentActivities - region={}, zone={}, today={}",
                RegionContext.getCurrentRegion(), zone, today);
        List<KioskActivityResponse> activities = new ArrayList<>();

        // Lấy attendance records hôm nay (sắp xếp theo thời gian mới nhất)
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AttendanceRecordEntity> records = attendanceRecordRepository
                .findByWorkDate(today, pageable);

        for (AttendanceRecordEntity record : records) {
            String employeeName = getEmployeeName(record.getEmployeeId());

            // Check-in
            if (record.getOriginalCheckIn() != null) {
                activities.add(KioskActivityResponse.builder()
                        .employeeName(employeeName)
                        .action("CHECK_IN")
                        .timestamp(record.getOriginalCheckIn())
                        .build());
            }

            // Check-out
            if (record.getOriginalCheckOut() != null) {
                activities.add(KioskActivityResponse.builder()
                        .employeeName(employeeName)
                        .action("CHECK_OUT")
                        .timestamp(record.getOriginalCheckOut())
                        .build());
            }
        }

        // Lấy break records hôm nay
        List<BreakRecordEntity> breaks = breakRecordRepository.findByWorkDate(today);
        for (BreakRecordEntity br : breaks) {
            String employeeName = getEmployeeName(br.getEmployeeId());

            if (br.getBreakStart() != null) {
                activities.add(KioskActivityResponse.builder()
                        .employeeName(employeeName)
                        .action("BREAK_START")
                        .timestamp(br.getBreakStart())
                        .build());
            }

            if (br.getBreakEnd() != null) {
                activities.add(KioskActivityResponse.builder()
                        .employeeName(employeeName)
                        .action("BREAK_END")
                        .timestamp(br.getBreakEnd())
                        .build());
            }
        }

        // Sắp xếp theo thời gian mới nhất và giới hạn
        activities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return activities.size() > limit ? activities.subList(0, limit) : activities;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KioskEmployeeStatusResponse> getEmployeeStatuses() {
        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        log.info("[TIMEZONE-DEBUG] getEmployeeStatuses - region={}, zone={}, today={}",
                RegionContext.getCurrentRegion(), zone, today);
        List<UserEntity> employees = userRepository.findByDeletedFalse();

        // Lấy tất cả attendance records hôm nay
        Map<Long, AttendanceRecordEntity> attendanceMap = attendanceRecordRepository
                .findAllByWorkDate(today).stream()
                .collect(Collectors.toMap(AttendanceRecordEntity::getEmployeeId, r -> r, (a, b) -> a));

        // Lấy tất cả break records hôm nay
        Map<Long, List<BreakRecordEntity>> breakMap = breakRecordRepository
                .findByWorkDate(today).stream()
                .collect(Collectors.groupingBy(BreakRecordEntity::getEmployeeId));

        return employees.stream().map(emp -> {
            AttendanceRecordEntity record = attendanceMap.get(emp.getId());
            List<BreakRecordEntity> breaks = breakMap.getOrDefault(emp.getId(), List.of());

            // Xác định trạng thái
            String status;
            if (record == null) {
                status = "NOT_CHECKED_IN";
            } else if (record.getOriginalCheckOut() != null) {
                status = "OFFLINE";
            } else if (breaks.stream().anyMatch(b -> b.getBreakEnd() == null)) {
                status = "BREAK";
            } else {
                status = "ONLINE";
            }

            List<KioskEmployeeStatusResponse.BreakPeriod> breakPeriods = breaks.stream()
                    .map(b -> KioskEmployeeStatusResponse.BreakPeriod.builder()
                            .start(b.getBreakStart())
                            .end(b.getBreakEnd())
                            .build())
                    .collect(Collectors.toList());

            String name = emp.getProfile() != null ? emp.getProfile().getName() : emp.getEmail();
            String avatar = emp.getProfile() != null ? emp.getProfile().getAvatar() : null;
            String deptName = emp.getProfile() != null && emp.getProfile().getDepartmentEntity() != null
                    ? emp.getProfile().getDepartmentEntity().getName() : null;

            return KioskEmployeeStatusResponse.builder()
                    .employeeId(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .name(name)
                    .avatar(avatar)
                    .departmentName(deptName)
                    .status(status)
                    .checkInTime(record != null ? record.getOriginalCheckIn() : null)
                    .checkOutTime(record != null ? record.getOriginalCheckOut() : null)
                    .breaks(breakPeriods)
                    .build();
        }).collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    private AttendanceKioskEntity findKiosk(Long id) {
        return kioskRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> NotFoundException.kiosk(id));
    }

    private AttendanceKioskEntity findActiveKiosk(Long kioskId) {
        AttendanceKioskEntity kiosk = findKiosk(kioskId);
        if (!Boolean.TRUE.equals(kiosk.getIsActive())) {
            throw new BadRequestException("Máy chấm công đã bị vô hiệu hóa", ErrorCode.KIOSK_INACTIVE);
        }
        return kiosk;
    }

    private UserEntity findEmployeeByCode(String employeeCode) {
        return userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode)
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy nhân viên với mã: " + employeeCode,
                        ErrorCode.KIOSK_EMPLOYEE_NOT_FOUND));
    }

    private AttendanceLocationEntity findLocation(Long locationId) {
        return locationRepository.findByIdAndDeletedFalse(locationId)
                .orElseThrow(() -> NotFoundException.location(locationId));
    }

    private void validateLocation(Long locationId) {
        if (!locationRepository.findByIdAndDeletedFalse(locationId).isPresent()) {
            throw NotFoundException.location(locationId);
        }
    }

    private String getLocationName(Long locationId) {
        return locationRepository.findByIdAndDeletedFalse(locationId)
                .map(AttendanceLocationEntity::getName)
                .orElse(null);
    }

    private String getEmployeeName(Long employeeId) {
        return userRepository.findWithProfileByIdAndDeletedFalse(employeeId)
                .map(user -> user.getProfile() != null ? user.getProfile().getName() : user.getEmail())
                .orElse("Unknown");
    }

    /**
     * Cập nhật checkInSource và kioskId cho attendance record hôm nay
     */
    private void updateCheckInSource(Long employeeId, Long kioskId) {
        ZoneId zone = RegionUtil.getTimezone(RegionContext.getCurrentRegion());
        LocalDate today = LocalDate.now(zone);
        log.info("[TIMEZONE-DEBUG] updateCheckInSource - region={}, zone={}, today={}",
                RegionContext.getCurrentRegion(), zone, today);
        attendanceRecordRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .ifPresent(record -> {
                    record.setCheckInSource(CheckInSource.KIOSK);
                    record.setKioskId(kioskId);
                    attendanceRecordRepository.save(record);
                });
    }
}
