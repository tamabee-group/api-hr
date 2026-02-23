package com.tamabee.api_hr.controller.company;

import java.time.Year;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.leave.BulkAllocateLeaveRequest;
import com.tamabee.api_hr.dto.response.leave.BulkAllocateResponse;
import com.tamabee.api_hr.dto.response.leave.LeaveBalanceSummaryResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.company.interfaces.ILeaveService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý số ngày phép của nhân viên.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền xem và cấp phát số ngày phép.
 */
@RestController
@RequestMapping("/api/company/leave-balances")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class LeaveBalanceController {

    private final ILeaveService leaveService;

    /**
     * Lấy danh sách số ngày phép của tất cả nhân viên (phân trang)
     * GET /api/company/leave-balances?year=2026&search=keyword&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<LeaveBalanceSummaryResponse>>> getAllLeaveBalances(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Mặc định năm hiện tại nếu không truyền
        Integer effectiveYear = year != null ? year : Year.now().getValue();

        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveBalanceSummaryResponse> balances = leaveService.getAllLeaveBalances(effectiveYear, search, pageable);

        return ResponseEntity.ok(BaseResponse.success(balances, "Lấy danh sách số ngày phép thành công"));
    }

    /**
     * Cấp phát số ngày phép hàng loạt cho nhiều nhân viên
     * POST /api/company/leave-balances/bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<BaseResponse<BulkAllocateResponse>> bulkAllocateLeaveBalance(
            @Valid @RequestBody BulkAllocateLeaveRequest request) {

        BulkAllocateResponse response = leaveService.bulkAllocateLeaveBalance(request);

        return ResponseEntity.ok(BaseResponse.created(response, "Cấp phát số ngày phép thành công"));
    }
}
