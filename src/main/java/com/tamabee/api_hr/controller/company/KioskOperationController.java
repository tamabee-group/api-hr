package com.tamabee.api_hr.controller.company;

import java.util.List;

import com.tamabee.api_hr.dto.response.attendance.KioskEmployeeStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.attendance.KioskCheckInRequest;
import com.tamabee.api_hr.dto.request.attendance.KioskLoginRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.attendance.KioskActivityResponse;
import com.tamabee.api_hr.dto.response.attendance.KioskLoginResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceKioskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller cho các thao tác kiosk (đăng nhập, chấm công, giải lao).
 * Yêu cầu đăng nhập hệ thống (JWT) + xác thực kiosk bằng PIN.
 */
@RestController
@RequestMapping("/api/company/kiosk")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
public class KioskOperationController {

    private final IAttendanceKioskService kioskService;

    /**
     * Đăng nhập kiosk bằng PIN
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<KioskLoginResponse>> login(
            @Valid @RequestBody KioskLoginRequest request) {
        KioskLoginResponse response = kioskService.login(request);
        return ResponseEntity.ok(BaseResponse.success(response, "Đăng nhập kiosk thành công"));
    }

    /**
     * Chấm công vào qua kiosk
     */
    @PostMapping("/{kioskId}/check-in")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> checkIn(
            @PathVariable Long kioskId,
            @Valid @RequestBody KioskCheckInRequest request) {
        AttendanceRecordResponse response = kioskService.kioskCheckIn(kioskId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Check-in thành công"));
    }

    /**
     * Chấm công ra qua kiosk
     */
    @PostMapping("/{kioskId}/check-out")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> checkOut(
            @PathVariable Long kioskId,
            @Valid @RequestBody KioskCheckInRequest request) {
        AttendanceRecordResponse response = kioskService.kioskCheckOut(kioskId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Check-out thành công"));
    }

    /**
     * Bắt đầu giải lao qua kiosk
     */
    @PostMapping("/{kioskId}/break/start")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> startBreak(
            @PathVariable Long kioskId,
            @Valid @RequestBody KioskCheckInRequest request) {
        AttendanceRecordResponse response = kioskService.kioskStartBreak(kioskId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Bắt đầu giải lao thành công"));
    }

    /**
     * Kết thúc giải lao qua kiosk
     */
    @PostMapping("/{kioskId}/break/end")
    public ResponseEntity<BaseResponse<AttendanceRecordResponse>> endBreak(
            @PathVariable Long kioskId,
            @Valid @RequestBody KioskCheckInRequest request) {
        AttendanceRecordResponse response = kioskService.kioskEndBreak(kioskId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Kết thúc giải lao thành công"));
    }

    /**
     * Lấy hoạt động gần đây
     */
    @GetMapping("/{kioskId}/activities")
    public ResponseEntity<BaseResponse<List<KioskActivityResponse>>> getRecentActivities(
            @PathVariable Long kioskId,
            @RequestParam(defaultValue = "20") int limit) {
        List<KioskActivityResponse> activities = kioskService.getRecentActivities(kioskId, limit);
        return ResponseEntity.ok(BaseResponse.success(activities, "Lấy hoạt động gần đây thành công"));
    }

    /**
     * Lấy danh sách nhân viên kèm trạng thái chấm công hôm nay
     */
    @GetMapping("/employees/status")
    public ResponseEntity<BaseResponse<List<KioskEmployeeStatusResponse>>> getEmployeeStatuses() {
        List<KioskEmployeeStatusResponse> statuses = kioskService.getEmployeeStatuses();
        return ResponseEntity.ok(BaseResponse.success(statuses, "Lấy trạng thái nhân viên thành công"));
    }
}
