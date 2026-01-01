package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.AttendanceQueryRequest;
import com.tamabee.api_hr.dto.response.AdjustmentRequestResponse;
import com.tamabee.api_hr.dto.response.AttendanceRecordResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IAttendanceAdjustmentService;
import com.tamabee.api_hr.service.company.IAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller quản lý chấm công cho admin/manager công ty.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/attendance")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class AttendanceController {

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
        Long companyId = getCurrentUserCompanyId();

        AttendanceQueryRequest request = AttendanceQueryRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .employeeId(employeeId)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "workDate"));
        Page<AttendanceRecordResponse> records = attendanceService.getAttendanceRecords(companyId, request, pageable);
        return ResponseEntity.ok(BaseResponse.success(records, "Lấy danh sách chấm công thành công"));
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
     * Lấy companyId của user đang đăng nhập
     */
    private Long getCurrentUserCompanyId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
        return user.getCompanyId();
    }
}
