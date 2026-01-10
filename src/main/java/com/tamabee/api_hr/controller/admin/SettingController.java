package com.tamabee.api_hr.controller.admin;

import com.tamabee.api_hr.dto.request.SettingUpdateRequest;
import com.tamabee.api_hr.dto.response.SettingResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý cấu hình hệ thống Tamabee
 * GET endpoints: ADMIN_TAMABEE, MANAGER_TAMABEE có quyền truy cập
 * PUT endpoint: Chỉ ADMIN_TAMABEE có quyền cập nhật
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_TAMABEE_ACCESS)
public class SettingController {

    private final ISettingService settingService;

    /**
     * Lấy danh sách tất cả settings
     * GET /api/admin/settings
     * Roles: ADMIN_TAMABEE, MANAGER_TAMABEE
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<SettingResponse>>> getAll() {
        List<SettingResponse> settings = settingService.getAll();
        return ResponseEntity.ok(BaseResponse.success(settings));
    }

    /**
     * Lấy setting theo key
     * GET /api/admin/settings/{key}
     * Roles: ADMIN_TAMABEE, MANAGER_TAMABEE
     */
    @GetMapping("/{key}")
    public ResponseEntity<BaseResponse<SettingResponse>> getByKey(@PathVariable String key) {
        SettingResponse setting = settingService.get(key);
        return ResponseEntity.ok(BaseResponse.success(setting));
    }

    /**
     * Cập nhật setting theo key
     * PUT /api/admin/settings/{key}
     * Roles: Chỉ ADMIN_TAMABEE
     */
    @PutMapping("/{key}")
    @PreAuthorize(RoleConstants.HAS_ADMIN_TAMABEE)
    public ResponseEntity<BaseResponse<SettingResponse>> update(
            @PathVariable String key,
            @Valid @RequestBody SettingUpdateRequest request) {
        SettingResponse setting = settingService.update(key, request);
        return ResponseEntity.ok(BaseResponse.success(setting, "Cập nhật cấu hình thành công"));
    }
}
