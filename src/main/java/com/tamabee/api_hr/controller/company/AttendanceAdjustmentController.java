package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.company.RejectRequest;
import com.tamabee.api_hr.dto.response.attendance.AdjustmentRequestResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceAdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý yêu cầu điều chỉnh chấm công cho manager.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền phê duyệt/từ chối.
 */
@RestController
@RequestMapping("/api/company/attendance-adjustments")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class AttendanceAdjustmentController {

    private final IAttendanceAdjustmentService adjustmentService;
    private final UserRepository userRepository;

    /**
     * Lấy tất cả yêu cầu điều chỉnh của công ty (phân trang)
     * Admin xem tất cả, Manager chỉ xem yêu cầu được gán cho mình
     * GET /api/company/attendance-adjustments
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<AdjustmentRequestResponse>>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserEntity currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN_COMPANY;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdjustmentRequestResponse> requests = adjustmentService.getAllRequests(userId, isAdmin,
                pageable);
        return ResponseEntity.ok(BaseResponse.success(requests, "Lấy danh sách yêu cầu điều chỉnh thành công"));
    }

    /**
     * Lấy danh sách yêu cầu điều chỉnh đang chờ duyệt (phân trang)
     * Admin xem tất cả, Manager chỉ xem yêu cầu được gán cho mình
     * GET /api/company/attendance-adjustments/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<BaseResponse<Page<AdjustmentRequestResponse>>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserEntity currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN_COMPANY;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdjustmentRequestResponse> requests = adjustmentService.getPendingRequests(userId, isAdmin,
                pageable);
        return ResponseEntity.ok(BaseResponse.success(requests, "Lấy danh sách yêu cầu chờ duyệt thành công"));
    }

    /**
     * Lấy chi tiết yêu cầu điều chỉnh theo ID
     * GET /api/company/attendance-adjustments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<AdjustmentRequestResponse>> getRequestById(@PathVariable Long id) {
        AdjustmentRequestResponse request = adjustmentService.getRequestById(id);
        return ResponseEntity.ok(BaseResponse.success(request, "Lấy thông tin yêu cầu điều chỉnh thành công"));
    }

    /**
     * Phê duyệt yêu cầu điều chỉnh
     * PUT /api/company/attendance-adjustments/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<BaseResponse<AdjustmentRequestResponse>> approveAdjustment(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {
        Long managerId = getCurrentUserId();
        AdjustmentRequestResponse request = adjustmentService.approveAdjustment(id, managerId, comment);
        return ResponseEntity.ok(BaseResponse.success(request, "Phê duyệt yêu cầu điều chỉnh thành công"));
    }

    /**
     * Từ chối yêu cầu điều chỉnh
     * PUT /api/company/attendance-adjustments/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<BaseResponse<AdjustmentRequestResponse>> rejectAdjustment(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request) {
        Long managerId = getCurrentUserId();
        AdjustmentRequestResponse response = adjustmentService.rejectAdjustment(id, managerId,
                request.getRejectionReason());
        return ResponseEntity.ok(BaseResponse.success(response, "Từ chối yêu cầu điều chỉnh thành công"));
    }

    /**
     * Lấy userId của user đang đăng nhập
     */
    private Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Lấy thông tin user đang đăng nhập
     */
    private UserEntity getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));
    }
}
