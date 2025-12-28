package com.tamabee.api_hr.controller.admin;

import com.tamabee.api_hr.dto.request.DepositFilterRequest;
import com.tamabee.api_hr.dto.request.RejectRequest;
import com.tamabee.api_hr.dto.response.DepositRequestResponse;
import com.tamabee.api_hr.enums.DepositStatus;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.admin.IDepositRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý yêu cầu nạp tiền cho Admin Tamabee
 * ADMIN_TAMABEE, MANAGER_TAMABEE có quyền đầy đủ (duyệt/từ chối)
 * EMPLOYEE_TAMABEE có quyền đọc (read-only) để hỗ trợ company
 */
@RestController
@RequestMapping("/api/admin/deposits")
@RequiredArgsConstructor
public class AdminDepositRequestController {

    private final IDepositRequestService depositRequestService;

    /**
     * Lấy danh sách tất cả yêu cầu nạp tiền (phân trang)
     * GET /api/admin/deposits
     * ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE có quyền đọc
     * 
     * @param page      số trang (mặc định 0)
     * @param size      số lượng mỗi trang (mặc định 20)
     * @param status    trạng thái yêu cầu (filter)
     * @param companyId ID của company (filter)
     */
    @GetMapping
    @PreAuthorize(RoleConstants.HAS_ALL_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<Page<DepositRequestResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) DepositStatus status,
            @RequestParam(required = false) Long companyId) {

        // Tạo filter request
        DepositFilterRequest filter = new DepositFilterRequest();
        filter.setStatus(status);
        filter.setCompanyId(companyId);

        Pageable pageable = PageRequest.of(page, size);
        Page<DepositRequestResponse> deposits = depositRequestService.getAll(filter, pageable);

        return ResponseEntity.ok(BaseResponse.success(deposits));
    }

    /**
     * Lấy chi tiết yêu cầu nạp tiền theo ID
     * GET /api/admin/deposits/{id}
     * ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE có quyền đọc
     */
    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ALL_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<DepositRequestResponse>> getById(@PathVariable Long id) {
        DepositRequestResponse deposit = depositRequestService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(deposit));
    }

    /**
     * Duyệt yêu cầu nạp tiền
     * POST /api/admin/deposits/{id}/approve
     * Sau khi kiểm tra thủ công chuyển khoản ngân hàng
     * Chỉ ADMIN_TAMABEE và MANAGER_TAMABEE có quyền
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize(RoleConstants.HAS_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<DepositRequestResponse>> approve(@PathVariable Long id) {
        DepositRequestResponse deposit = depositRequestService.approve(id);
        return ResponseEntity.ok(BaseResponse.success(deposit, "Duyệt yêu cầu nạp tiền thành công"));
    }

    /**
     * Từ chối yêu cầu nạp tiền
     * POST /api/admin/deposits/{id}/reject
     * Chỉ ADMIN_TAMABEE và MANAGER_TAMABEE có quyền
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize(RoleConstants.HAS_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<DepositRequestResponse>> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request) {
        DepositRequestResponse deposit = depositRequestService.reject(id, request);
        return ResponseEntity.ok(BaseResponse.success(deposit, "Từ chối yêu cầu nạp tiền thành công"));
    }
}
