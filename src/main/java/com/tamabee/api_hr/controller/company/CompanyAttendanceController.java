package com.tamabee.api_hr.controller.company;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.attendance.AdjustAttendanceRequest;
import com.tamabee.api_hr.dto.request.attendance.AttendanceQueryRequest;
import com.tamabee.api_hr.dto.request.attendance.CreateAttendanceRequest;
import com.tamabee.api_hr.dto.response.attendance.AdjustmentRequestResponse;
import com.tamabee.api_hr.dto.response.attendance.AttendanceRecordResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceAdjustmentService;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý chấm công cho admin/manager công ty.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/attendance")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class CompanyAttendanceController {

    private final IAttendanceService attendanceService;
    private final IAttendanceAdjustmentService adjustmentService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách chấm công của công ty (phân trang)
     * GET /api/company/attendance
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<AttendanceRecordResponse>>> getAttendanceRecords(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        AttendanceQueryRequest request = AttendanceQueryRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .employeeId(employeeId)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "workDate"));
        Page<AttendanceRecordResponse> records = attendanceService.getAttendanceRecords(request, pageable);
        return ResponseEntity.ok(BaseResponse.success(records, "Lấy danh sách chấm công thành công"));
    }

    /**
     * Lấy danh sách chấm công có vị trí (cho trang locations)
     * GET /api/company/attendance/with-location
     */
    @GetMapping("/with-location")
    public ResponseEntity<BaseResponse<Page<AttendanceRecordResponse>>> getAttendanceRecordsWithLocation(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AttendanceQueryRequest request = AttendanceQueryRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "workDate"));
        Page<AttendanceRecordResponse> records = attendanceService.getAttendanceRecordsWithLocation(request, pageable);
        return ResponseEntity.ok(BaseResponse.success(records, "Lấy danh sách vị trí chấm công thành công"));
    }

    /**
     * Lấy chi tiết bản ghi chấm công theo ID
     * GET /api/company/attendance/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> getAttendanceRecordById(@PathVariable Long id) {
        AttendanceRecordResponse record = attendanceService.getAttendanceRecordById(id);
        return ResponseEntity.ok(BaseResponse.success(record, "Lấy thông tin chấm công thành công"));
    }

    /**
     * Lấy lịch sử điều chỉnh của một bản ghi chấm công
     * GET /api/company/attendance/{recordId}/adjustment-history
     */
    @GetMapping("/{recordId}/adjustment-history")
    public ResponseEntity<BaseResponse<List<AdjustmentRequestResponse>>> getAdjustmentHistory(
            @PathVariable Long recordId) {
        List<AdjustmentRequestResponse> history = adjustmentService.getAdjustmentHistoryByAttendanceRecord(recordId);
        return ResponseEntity.ok(BaseResponse.success(history, "Lấy lịch sử điều chỉnh thành công"));
    }

    /**
     * Điều chỉnh bản ghi chấm công (bởi manager)
     * PUT /api/company/attendance/{id}/adjust
     */
    @PutMapping("/{id}/adjust")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> adjustAttendance(
            @PathVariable Long id,
            @Valid @RequestBody AdjustAttendanceRequest request) {
        Long adjustedBy = getCurrentUserId();
        AttendanceRecordResponse record = attendanceService.adjustAttendance(id, adjustedBy, request);
        return ResponseEntity.ok(BaseResponse.success(record, "Điều chỉnh chấm công thành công"));
    }

    /**
     * Tạo bản ghi chấm công mới cho nhân viên (bởi manager)
     * POST /api/company/attendance/create
     */
    @PostMapping("/create")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> createAttendanceRecord(
            @Valid @RequestBody CreateAttendanceRequest request) {
        Long createdBy = getCurrentUserId();
        AttendanceRecordResponse record = attendanceService.createAttendanceRecord(createdBy, request);
        return ResponseEntity.ok(BaseResponse.created(record, "Tạo bản ghi chấm công thành công"));
    }

    /**
     * Xóa bản ghi chấm công (bởi admin/manager)
     * DELETE /api/company/attendance/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteAttendanceRecord(@PathVariable Long id) {
        Long deletedBy = getCurrentUserId();
        attendanceService.deleteAttendanceRecord(id, deletedBy);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa bản ghi chấm công thành công"));
    }

    /**
     * Lấy userId của user đang đăng nhập
     */
    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
        return user.getId();
    }
}
