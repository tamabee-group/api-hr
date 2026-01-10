package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.request.attendance.AttendanceQueryRequest;
import com.tamabee.api_hr.dto.request.attendance.CheckInRequest;
import com.tamabee.api_hr.dto.request.attendance.CheckOutRequest;
import com.tamabee.api_hr.dto.request.attendance.StartBreakRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.attendance.AttendanceSummaryResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Controller cho Employee Attendance API.
 * Cung cấp API để nhân viên chấm công và quản lý giờ giải lao.
 * Tất cả nhân viên công ty đều có quyền truy cập.
 */
@RestController
@RequestMapping("/api/employee/attendance")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
public class EmployeeAttendanceController {

    private final IAttendanceService attendanceService;
    private final UserRepository userRepository;

    /**
     * Lấy trạng thái chấm công hôm nay của nhân viên đang đăng nhập.
     * Bao gồm cả thông tin attendance và tất cả break records.
     * GET /api/employee/attendance/today
     */
    @GetMapping("/today")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> getTodayAttendance() {
        Long employeeId = getCurrentUserId();
        AttendanceRecordResponse response = attendanceService.getTodayAttendance(employeeId);
        return ResponseEntity.ok(BaseResponse.success(response, "Lấy thông tin chấm công hôm nay thành công"));
    }

    /**
     * Lấy danh sách chấm công của nhân viên (phân trang)
     * GET /api/employee/attendance
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<AttendanceRecordResponse>>> getMyAttendanceRecords(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UserEntity currentUser = getCurrentUser();

        AttendanceQueryRequest request = AttendanceQueryRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "workDate"));
        Page<AttendanceRecordResponse> records = attendanceService.getEmployeeAttendanceRecords(
                currentUser.getId(), request, pageable);
        return ResponseEntity.ok(BaseResponse.success(records, "Lấy danh sách chấm công thành công"));
    }

    /**
     * Lấy danh sách chấm công của nhân viên theo tháng (cho calendar view)
     * GET /api/employee/attendance/month?year=2024&month=1
     */
    @GetMapping("/month")
    public ResponseEntity<BaseResponse<Page<AttendanceRecordResponse>>> getMyAttendanceByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        UserEntity currentUser = getCurrentUser();

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        AttendanceQueryRequest request = AttendanceQueryRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Pageable pageable = PageRequest.of(0, 31, Sort.by(Sort.Direction.ASC, "workDate"));
        Page<AttendanceRecordResponse> records = attendanceService.getEmployeeAttendanceRecords(
                currentUser.getId(), request, pageable);
        return ResponseEntity.ok(BaseResponse.success(records, "Lấy danh sách chấm công theo tháng thành công"));
    }

    /**
     * Lấy tổng hợp chấm công của nhân viên trong một kỳ
     * GET /api/employee/attendance/summary?period=2024-01
     */
    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<AttendanceSummaryResponse>> getMyAttendanceSummary(
            @RequestParam YearMonth period) {
        UserEntity currentUser = getCurrentUser();
        AttendanceSummaryResponse summary = attendanceService.getAttendanceSummary(
                currentUser.getId(),
                period);
        return ResponseEntity.ok(BaseResponse.success(summary, "Lấy tổng hợp chấm công thành công"));
    }

    /**
     * Lấy chấm công theo ngày cụ thể của nhân viên đang đăng nhập.
     * Bao gồm cả thông tin attendance và tất cả break records.
     * GET /api/employee/attendance/{date}
     */
    @GetMapping("/{date}")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> getAttendanceByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long employeeId = getCurrentUserId();
        AttendanceRecordResponse response = attendanceService.getAttendanceByEmployeeAndDate(employeeId, date);
        return ResponseEntity.ok(BaseResponse.success(response, "Lấy thông tin chấm công thành công"));
    }

    /**
     * Check-in cho nhân viên đang đăng nhập.
     * Áp dụng các validation và settings của công ty.
     * POST /api/employee/attendance/check-in
     */
    @PostMapping("/check-in")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> checkIn(
            @RequestBody(required = false) CheckInRequest request) {
        UserEntity currentUser = getCurrentUser();
        // Nếu request null, tạo request rỗng
        if (request == null) {
            request = new CheckInRequest();
        }
        AttendanceRecordResponse response = attendanceService.checkIn(
                currentUser.getId(), request);
        return ResponseEntity.ok(BaseResponse.success(response, "Check-in thành công"));
    }

    /**
     * Check-out cho nhân viên đang đăng nhập.
     * Tính toán working minutes, overtime, late/early leave.
     * POST /api/employee/attendance/check-out
     */
    @PostMapping("/check-out")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> checkOut(
            @RequestBody(required = false) CheckOutRequest request) {
        UserEntity currentUser = getCurrentUser();
        // Nếu request null, tạo request rỗng
        if (request == null) {
            request = new CheckOutRequest();
        }
        AttendanceRecordResponse response = attendanceService.checkOut(
                currentUser.getId(), request);
        return ResponseEntity.ok(BaseResponse.success(response, "Check-out thành công"));
    }

    /**
     * Bắt đầu giờ giải lao cho nhân viên đang đăng nhập.
     * Validate số lần break trong ngày và break periods.
     * POST /api/employee/attendance/break/start
     */
    @PostMapping("/break/start")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> startBreak(
            @RequestBody(required = false) StartBreakRequest request) {
        Long employeeId = getCurrentUserId();
        // Nếu request null, tạo request rỗng
        if (request == null) {
            request = new StartBreakRequest();
        }
        AttendanceRecordResponse response = attendanceService.startBreak(employeeId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Bắt đầu giờ giải lao thành công"));
    }

    /**
     * Kết thúc giờ giải lao cho nhân viên đang đăng nhập.
     * Tính toán effective break minutes dựa trên min/max settings.
     * POST /api/employee/attendance/break/{id}/end
     */
    @PostMapping("/break/{id}/end")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> endBreak(@PathVariable Long id) {
        Long employeeId = getCurrentUserId();
        AttendanceRecordResponse response = attendanceService.endBreak(employeeId, id);
        return ResponseEntity.ok(BaseResponse.success(response, "Kết thúc giờ giải lao thành công"));
    }

    /**
     * Lấy ID của user đang đăng nhập
     */
    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
        return user.getId();
    }

    /**
     * Lấy thông tin user đang đăng nhập
     */
    private UserEntity getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
    }
}
