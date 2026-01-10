package com.tamabee.api_hr.controller.admin;

import com.tamabee.api_hr.dto.request.user.CreateTamabeeUserRequest;
import com.tamabee.api_hr.dto.request.user.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.user.UserResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.service.admin.interfaces.IEmployeeManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller quản lý nhân viên Tamabee
 * Chỉ ADMIN_TAMABEE có quyền truy cập
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ADMIN_TAMABEE)
public class AdminUserController {

    private final IEmployeeManagerService employeeManagerService;

    /**
     * Lấy danh sách nhân viên Tamabee (phân trang)
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<UserResponse>>> getTamabeeUsers(Pageable pageable) {
        Page<UserResponse> users = employeeManagerService.getTamabeeUsers(pageable);
        return ResponseEntity.ok(BaseResponse.success(users, "Lấy danh sách người dùng thành công"));
    }

    /**
     * Lấy thông tin chi tiết nhân viên Tamabee theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<UserResponse>> getTamabeeUser(@PathVariable Long id) {
        UserResponse user = employeeManagerService.getTamabeeUser(id);
        return ResponseEntity.ok(BaseResponse.success(user, "Lấy thông tin người dùng thành công"));
    }

    /**
     * Tạo nhân viên Tamabee mới
     */
    @PostMapping
    public ResponseEntity<BaseResponse<UserResponse>> createTamabeeUser(
            @Valid @RequestBody CreateTamabeeUserRequest request) {
        UserResponse user = employeeManagerService.createTamabeeUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.created(user, "Tạo người dùng thành công"));
    }

    /**
     * Cập nhật thông tin nhân viên Tamabee
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<UserResponse>> updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UserResponse user = employeeManagerService.updateUserProfile(id, request);
        return ResponseEntity.ok(BaseResponse.success(user, "Cập nhật thông tin thành công"));
    }

    /**
     * Upload avatar cho nhân viên Tamabee
     */
    @PostMapping("/{id}/avatar")
    public ResponseEntity<BaseResponse<String>> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("avatar") MultipartFile file) {
        String avatarUrl = employeeManagerService.uploadAvatar(id, file);
        return ResponseEntity.ok(BaseResponse.success(avatarUrl, "Tải ảnh đại diện thành công"));
    }
}
