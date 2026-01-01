package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.request.AttendanceQueryRequest;
import com.tamabee.api_hr.dto.request.CheckInRequest;
import com.tamabee.api_hr.dto.request.CheckOutRequest;
import com.tamabee.api_hr.dto.response.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.AttendanceSummaryResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Controller cho nhân viên chấm công.
 * Tất cả nhân viên công ty có quyền truy cập.
 */
@RestController
@RequestMapping("/api/employee/attendance")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
public class EmployeeAttendanceController {

        private final IAttendanceService attendanceService;
        private final UserRepository userRepository;

        /**
         * Nhân viên check-in
         * POST /api/employee/attendance/check-in
         */
        @PostMapping("/check-in")
        public ResponseEntity<BaseResponse<AttendanceRecordResponse>> checkIn(
                        @Valid @RequestBody CheckInRequest request) {
                UserEntity currentUser = getCurrentUser();
                AttendanceRecordResponse record = attendanceService.checkIn(
                                currentUser.getId(),
                                currentUser.getCompanyId(),
                                request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(BaseResponse.created(record, "Check-in thành công"));
        }

        /**
         * Nhân viên check-out
         * POST /api/employee/attendance/check-out
         */
        @PostMapping("/check-out")
        public ResponseEntity<BaseResponse<AttendanceRecordResponse>> checkOut(
                        @Valid @RequestBody CheckOutRequest request) {
                UserEntity currentUser = getCurrentUser();
                AttendanceRecordResponse record = attendanceService.checkOut(
                                currentUser.getId(),
                                currentUser.getCompanyId(),
                                request);
                return ResponseEntity.ok(BaseResponse.success(record, "Check-out thành công"));
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

                // Tính ngày đầu và cuối tháng
                YearMonth yearMonth = YearMonth.of(year, month);
                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth();

                AttendanceQueryRequest request = AttendanceQueryRequest.builder()
                                .startDate(startDate)
                                .endDate(endDate)
                                .build();

                // Lấy tất cả records trong tháng (max 31 ngày)
                Pageable pageable = PageRequest.of(0, 31, Sort.by(Sort.Direction.ASC, "workDate"));
                Page<AttendanceRecordResponse> records = attendanceService.getEmployeeAttendanceRecords(
                                currentUser.getId(), request, pageable);
                return ResponseEntity
                                .ok(BaseResponse.success(records, "Lấy danh sách chấm công theo tháng thành công"));
        }

        /**
         * Lấy trạng thái chấm công hôm nay của nhân viên
         * GET /api/employee/attendance/today
         */
        @GetMapping("/today")
        public ResponseEntity<BaseResponse<AttendanceRecordResponse>> getTodayStatus() {
                UserEntity currentUser = getCurrentUser();
                AttendanceRecordResponse record = attendanceService.getAttendanceByEmployeeAndDate(
                                currentUser.getId(), LocalDate.now());
                return ResponseEntity.ok(BaseResponse.success(record, "Lấy trạng thái chấm công hôm nay thành công"));
        }

        /**
         * Lấy chi tiết chấm công của nhân viên theo ngày
         * GET /api/employee/attendance/{date}
         */
        @GetMapping("/{date}")
        public ResponseEntity<BaseResponse<AttendanceRecordResponse>> getMyAttendanceByDate(
                        @PathVariable LocalDate date) {
                UserEntity currentUser = getCurrentUser();
                AttendanceRecordResponse record = attendanceService.getAttendanceByEmployeeAndDate(
                                currentUser.getId(), date);
                return ResponseEntity.ok(BaseResponse.success(record, "Lấy thông tin chấm công thành công"));
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
                                currentUser.getCompanyId(),
                                period);
                return ResponseEntity.ok(BaseResponse.success(summary, "Lấy tổng hợp chấm công thành công"));
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
