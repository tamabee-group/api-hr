package com.tamabee.api_hr.controller.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.tamabee.api_hr.dto.request.attendance.CreateAttendanceKioskRequest;
import com.tamabee.api_hr.dto.request.attendance.UpdateAttendanceKioskRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceKioskResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceKioskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý máy chấm công cố định (CRUD - Admin only).
 */
@RestController
@RequestMapping("/api/company/settings/kiosks")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
public class AttendanceKioskController {

    private final IAttendanceKioskService kioskService;

    @GetMapping
    public ResponseEntity<BaseResponse<Page<AttendanceKioskResponse>>> getKiosks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AttendanceKioskResponse> kiosks = kioskService.getKiosks(pageable);
        return ResponseEntity.ok(BaseResponse.success(kiosks, "Lấy danh sách kiosk thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<AttendanceKioskResponse>> getKiosk(@PathVariable Long id) {
        AttendanceKioskResponse kiosk = kioskService.getKiosk(id);
        return ResponseEntity.ok(BaseResponse.success(kiosk, "Lấy thông tin kiosk thành công"));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<AttendanceKioskResponse>> createKiosk(
            @Valid @RequestBody CreateAttendanceKioskRequest request) {
        AttendanceKioskResponse kiosk = kioskService.createKiosk(request);
        return ResponseEntity.ok(BaseResponse.created(kiosk, "Tạo kiosk thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<AttendanceKioskResponse>> updateKiosk(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttendanceKioskRequest request) {
        AttendanceKioskResponse kiosk = kioskService.updateKiosk(id, request);
        return ResponseEntity.ok(BaseResponse.success(kiosk, "Cập nhật kiosk thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteKiosk(@PathVariable Long id) {
        kioskService.deleteKiosk(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa kiosk thành công"));
    }
}
