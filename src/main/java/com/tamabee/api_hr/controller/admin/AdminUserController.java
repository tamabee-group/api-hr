package com.tamabee.api_hr.controller.admin;

import com.tamabee.api_hr.dto.request.CreateTamabeeUserRequest;
import com.tamabee.api_hr.dto.request.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.admin.IEmployeeManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_TAMABEE', 'MANAGER_TAMABEE')")
public class AdminUserController {
    
    private final IEmployeeManagerService employeeManagerService;
    
    @GetMapping
    public BaseResponse<Page<UserResponse>> getTamabeeUsers(Pageable pageable) {
        Page<UserResponse> users = employeeManagerService.getTamabeeUsers(pageable);
        return BaseResponse.success(users, "Lấy danh sách người dùng thành công");
    }
    
    @GetMapping("/{id}")
    public BaseResponse<UserResponse> getTamabeeUser(@PathVariable Long id) {
        UserResponse user = employeeManagerService.getTamabeeUser(id);
        return BaseResponse.success(user, "Lấy thông tin người dùng thành công");
    }
    
    @PostMapping
    public BaseResponse<UserResponse> createTamabeeUser(@Valid @RequestBody CreateTamabeeUserRequest request) {
        UserResponse user = employeeManagerService.createTamabeeUser(request);
        return BaseResponse.success(user, "Tạo người dùng thành công");
    }
    
    @PutMapping("/{id}")
    public BaseResponse<UserResponse> updateUserProfile(@PathVariable Long id, @Valid @RequestBody UpdateUserProfileRequest request) {
        UserResponse user = employeeManagerService.updateUserProfile(id, request);
        return BaseResponse.success(user, "Cập nhật thông tin thành công");
    }
    
    @PostMapping("/{id}/avatar")
    public BaseResponse<String> uploadAvatar(@PathVariable Long id, @RequestParam("avatar") MultipartFile file) {
        String avatarUrl = employeeManagerService.uploadAvatar(id, file);
        return BaseResponse.success(avatarUrl, "Tải ảnh đại diện thành công");
    }
}
