package com.tamabee.api_hr.controller.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
import com.tamabee.api_hr.dto.request.attendance.CreateAttendanceLocationRequest;
import com.tamabee.api_hr.dto.request.attendance.UpdateAttendanceLocationRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceLocationResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceLocationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý vị trí chấm công.
 * Chỉ ADMIN_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/settings/locations")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
public class AttendanceLocationController {

    private final IAttendanceLocationService attendanceLocationService;

    /**
     * Lấy danh sách vị trí chấm công (phân trang)
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<AttendanceLocationResponse>>> getLocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<AttendanceLocationResponse> locations = attendanceLocationService.getLocations(pageable);
        return ResponseEntity.ok(BaseResponse.success(locations, "Lấy danh sách vị trí chấm công thành công"));
    }

    /**
     * Lấy chi tiết vị trí chấm công theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<AttendanceLocationResponse>> getLocation(@PathVariable Long id) {
        AttendanceLocationResponse location = attendanceLocationService.getLocation(id);
        return ResponseEntity.ok(BaseResponse.success(location, "Lấy thông tin vị trí chấm công thành công"));
    }

    /**
     * Tạo vị trí chấm công mới
     */
    @PostMapping
    public ResponseEntity<BaseResponse<AttendanceLocationResponse>> createLocation(
            @Valid @RequestBody CreateAttendanceLocationRequest request) {
        AttendanceLocationResponse location = attendanceLocationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(location, "Tạo vị trí chấm công thành công"));
    }

    /**
     * Cập nhật vị trí chấm công
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<AttendanceLocationResponse>> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttendanceLocationRequest request) {
        AttendanceLocationResponse location = attendanceLocationService.updateLocation(id, request);
        return ResponseEntity.ok(BaseResponse.success(location, "Cập nhật vị trí chấm công thành công"));
    }

    /**
     * Xóa vị trí chấm công (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteLocation(@PathVariable Long id) {
        attendanceLocationService.deleteLocation(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa vị trí chấm công thành công"));
    }
}
