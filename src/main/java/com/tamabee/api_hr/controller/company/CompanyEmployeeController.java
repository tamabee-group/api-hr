package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.CreateCompanyEmployeeRequest;
import com.tamabee.api_hr.dto.request.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.ICompanyEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller quản lý nhân viên công ty
 * Dành cho ADMIN_COMPANY và MANAGER_COMPANY
 */
@RestController
@RequestMapping("/api/company/employees")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class CompanyEmployeeController {

    private final ICompanyEmployeeService companyEmployeeService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách nhân viên công ty (phân trang)
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<UserResponse>>> getEmployees(Pageable pageable) {
        Long companyId = getCurrentUserCompanyId();
        Page<UserResponse> employees = companyEmployeeService.getCompanyEmployees(companyId, pageable);
        return ResponseEntity.ok(BaseResponse.success(employees, "Lấy danh sách nhân viên thành công"));
    }

    /**
     * Lấy thông tin chi tiết nhân viên theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<UserResponse>> getEmployee(@PathVariable Long id) {
        Long companyId = getCurrentUserCompanyId();
        UserResponse employee = companyEmployeeService.getCompanyEmployee(companyId, id);
        return ResponseEntity.ok(BaseResponse.success(employee, "Lấy thông tin nhân viên thành công"));
    }

    /**
     * Tạo nhân viên mới cho công ty
     */
    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ADMIN_COMPANY)
    public ResponseEntity<BaseResponse<UserResponse>> createEmployee(
            @Valid @RequestBody CreateCompanyEmployeeRequest request) {
        Long companyId = getCurrentUserCompanyId();
        UserResponse employee = companyEmployeeService.createCompanyEmployee(companyId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.created(employee, "Tạo nhân viên thành công"));
    }

    /**
     * Cập nhật thông tin nhân viên
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<UserResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        Long companyId = getCurrentUserCompanyId();
        UserResponse employee = companyEmployeeService.updateCompanyEmployee(companyId, id, request);
        return ResponseEntity.ok(BaseResponse.success(employee, "Cập nhật thông tin nhân viên thành công"));
    }

    /**
     * Upload avatar cho nhân viên
     */
    @PostMapping("/{id}/avatar")
    public ResponseEntity<BaseResponse<String>> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("avatar") MultipartFile file) {
        Long companyId = getCurrentUserCompanyId();
        String avatarUrl = companyEmployeeService.uploadEmployeeAvatar(companyId, id, file);
        return ResponseEntity.ok(BaseResponse.success(avatarUrl, "Tải ảnh đại diện thành công"));
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
