package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.request.CreateLeaveRequest;
import com.tamabee.api_hr.dto.response.LeaveBalanceResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

/**
 * Controller cho nhân viên quản lý yêu cầu nghỉ phép của mình.
 * Tất cả nhân viên công ty có quyền truy cập.
 */
@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
public class EmployeeLeaveController {

    private final ILeaveService leaveService;
    private final UserRepository userRepository;

    /**
     * Tạo yêu cầu nghỉ phép mới
     * POST /api/employee/leave-requests
     */
    @PostMapping("/leave-requests")
    public ResponseEntity<BaseResponse<LeaveRequestResponse>> createLeaveRequest(
            @Valid @RequestBody CreateLeaveRequest request) {
        UserEntity currentUser = getCurrentUser();
        LeaveRequestResponse response = leaveService.createLeaveRequest(
                currentUser.getId(),
                request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Tạo yêu cầu nghỉ phép thành công"));
    }

    /**
     * Lấy danh sách yêu cầu nghỉ phép của nhân viên (phân trang)
     * GET /api/employee/leave-requests
     */
    @GetMapping("/leave-requests")
    public ResponseEntity<BaseResponse<Page<LeaveRequestResponse>>> getMyLeaveRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UserEntity currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LeaveRequestResponse> requests = leaveService.getEmployeeLeaveRequests(
                currentUser.getId(), pageable);
        return ResponseEntity.ok(BaseResponse.success(requests, "Lấy danh sách yêu cầu nghỉ phép thành công"));
    }

    /**
     * Lấy chi tiết yêu cầu nghỉ phép theo ID
     * GET /api/employee/leave-requests/{id}
     */
    @GetMapping("/leave-requests/{id}")
    public ResponseEntity<BaseResponse<LeaveRequestResponse>> getLeaveRequestById(@PathVariable Long id) {
        LeaveRequestResponse request = leaveService.getLeaveRequestById(id);
        return ResponseEntity.ok(BaseResponse.success(request, "Lấy thông tin yêu cầu nghỉ phép thành công"));
    }

    /**
     * Hủy yêu cầu nghỉ phép đang chờ duyệt
     * DELETE /api/employee/leave-requests/{id}
     */
    @DeleteMapping("/leave-requests/{id}")
    public ResponseEntity<BaseResponse<LeaveRequestResponse>> cancelLeaveRequest(@PathVariable Long id) {
        UserEntity currentUser = getCurrentUser();
        LeaveRequestResponse response = leaveService.cancelLeaveRequest(id, currentUser.getId());
        return ResponseEntity.ok(BaseResponse.success(response, "Hủy yêu cầu nghỉ phép thành công"));
    }

    /**
     * Lấy số ngày phép còn lại của nhân viên
     * GET /api/employee/leave-balance?year=2025
     */
    @GetMapping("/leave-balance")
    public ResponseEntity<BaseResponse<List<LeaveBalanceResponse>>> getLeaveBalance(
            @RequestParam(required = false) Integer year) {
        UserEntity currentUser = getCurrentUser();
        // Mặc định lấy năm hiện tại nếu không truyền
        int targetYear = year != null ? year : Year.now().getValue();
        List<LeaveBalanceResponse> balances = leaveService.getLeaveBalance(currentUser.getId(), targetYear);
        return ResponseEntity.ok(BaseResponse.success(balances, "Lấy số ngày phép còn lại thành công"));
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
