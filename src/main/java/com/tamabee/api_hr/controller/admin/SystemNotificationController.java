package com.tamabee.api_hr.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.tamabee.api_hr.dto.request.CreateSystemNotificationRequest;
import com.tamabee.api_hr.dto.response.SystemNotificationResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.admin.interfaces.ISystemNotificationService;
import com.tamabee.api_hr.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý thông báo hệ thống (System Notification).
 * Tamabee Admin/Manager tạo và xem danh sách, user authenticated xem chi tiết.
 */
@RestController
@RequestMapping("/api/admin/system-notifications")
@RequiredArgsConstructor
public class SystemNotificationController {

    private final ISystemNotificationService systemNotificationService;
    private final SecurityUtil securityUtil;

    /**
     * Tạo thông báo hệ thống mới và gửi đến target audience cross-tenant.
     * POST /api/admin/system-notifications
     */
    @PostMapping
    @PreAuthorize(RoleConstants.HAS_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<SystemNotificationResponse>> create(
            @Valid @RequestBody CreateSystemNotificationRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        String userName = securityUtil.getCurrentUserName();
        SystemNotificationResponse response = systemNotificationService.create(request, userId, userName);
        return ResponseEntity.ok(BaseResponse.created(response, "Tạo thông báo hệ thống thành công"));
    }

    /**
     * Lấy danh sách thông báo hệ thống đã tạo (phân trang).
     * GET /api/admin/system-notifications
     */
    @GetMapping
    @PreAuthorize(RoleConstants.HAS_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<Page<SystemNotificationResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SystemNotificationResponse> notifications = systemNotificationService.getAll(pageable);
        return ResponseEntity.ok(BaseResponse.success(notifications));
    }

    /**
     * Lấy chi tiết thông báo hệ thống theo ID (nội dung 3 ngôn ngữ).
     * User authenticated cần fetch nội dung đa ngôn ngữ khi xem notification detail.
     * GET /api/admin/system-notifications/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<SystemNotificationResponse>> getById(@PathVariable Long id) {
        SystemNotificationResponse response = systemNotificationService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
