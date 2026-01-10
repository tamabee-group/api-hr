package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.RejectRequest;
import com.tamabee.api_hr.dto.response.LeaveRequestResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.ILeaveService;
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
 * Controller quản lý yêu cầu nghỉ phép cho manager.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền phê duyệt/từ chối.
 */
@RestController
@RequestMapping("/api/company/leave-requests")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class LeaveController {

    private final ILeaveService leaveService;
    private final UserRepository userRepository;

    /**
     * Lấy tất cả yêu cầu nghỉ phép của công ty (phân trang)
     * GET /api/company/leave-requests
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<LeaveRequestResponse>>> getAllLeaveRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LeaveRequestResponse> requests = leaveService.getAllLeaveRequests(pageable);
        return ResponseEntity
                .ok(BaseResponse.success(requests, "Lấy danh sách yêu cầu nghỉ phép thành công"));
    }

    /**
     * Lấy danh sách yêu cầu nghỉ phép đang chờ duyệt (phân trang)
     * GET /api/company/leave-requests/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<BaseResponse<Page<LeaveRequestResponse>>> getPendingLeaveRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LeaveRequestResponse> requests = leaveService.getPendingLeaveRequests(pageable);
        return ResponseEntity
                .ok(BaseResponse.success(requests, "Lấy danh sách yêu cầu nghỉ phép chờ duyệt thành công"));
    }

    /**
     * Lấy chi tiết yêu cầu nghỉ phép theo ID
     * GET /api/company/leave-requests/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<LeaveRequestResponse>> getLeaveRequestById(@PathVariable Long id) {
        LeaveRequestResponse request = leaveService.getLeaveRequestById(id);
        return ResponseEntity.ok(BaseResponse.success(request, "Lấy thông tin yêu cầu nghỉ phép thành công"));
    }

    /**
     * Phê duyệt yêu cầu nghỉ phép
     * PUT /api/company/leave-requests/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<BaseResponse<LeaveRequestResponse>> approveLeave(@PathVariable Long id) {
        Long managerId = getCurrentUserId();
        LeaveRequestResponse request = leaveService.approveLeave(id, managerId);
        return ResponseEntity.ok(BaseResponse.success(request, "Phê duyệt yêu cầu nghỉ phép thành công"));
    }

    /**
     * Từ chối yêu cầu nghỉ phép
     * PUT /api/company/leave-requests/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<BaseResponse<LeaveRequestResponse>> rejectLeave(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request) {
        Long managerId = getCurrentUserId();
        LeaveRequestResponse response = leaveService.rejectLeave(id, managerId, request.getRejectionReason());
        return ResponseEntity.ok(BaseResponse.success(response, "Từ chối yêu cầu nghỉ phép thành công"));
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
