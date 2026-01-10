package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.request.wallet.CommissionFilterRequest;
import com.tamabee.api_hr.dto.response.wallet.CommissionResponse;
import com.tamabee.api_hr.dto.response.wallet.CommissionSummaryResponse;
import com.tamabee.api_hr.enums.CommissionStatus;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.service.admin.interfaces.ICommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller quản lý hoa hồng cho nhân viên Tamabee
 * EMPLOYEE_TAMABEE có quyền xem commission của mình
 */
@RestController
@RequestMapping("/api/employee/commissions")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_EMPLOYEE_TAMABEE)
public class EmployeeCommissionController {

    private final ICommissionService commissionService;

    /**
     * Lấy danh sách commissions của nhân viên hiện tại (phân trang)
     * GET /api/employee/commissions
     * 
     * @param page     số trang (mặc định 0)
     * @param size     số lượng mỗi trang (mặc định 20)
     * @param sortBy   sắp xếp theo field (mặc định createdAt)
     * @param sortDir  hướng sắp xếp (asc/desc, mặc định desc)
     * @param status   filter theo status (PENDING, PAID)
     * @param fromDate filter từ ngày
     * @param toDate   filter đến ngày
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<CommissionResponse>>> getMyCommissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) CommissionStatus status,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate) {

        // Tạo filter request (không có employeeCode vì sẽ lấy từ JWT)
        CommissionFilterRequest filter = new CommissionFilterRequest();
        filter.setStatus(status);
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CommissionResponse> commissions = commissionService.getMyCommissions(filter, pageable);
        return ResponseEntity.ok(BaseResponse.success(commissions));
    }

    /**
     * Lấy tổng hợp hoa hồng của nhân viên hiện tại
     * GET /api/employee/commissions/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<CommissionSummaryResponse>> getMySummary() {
        CommissionSummaryResponse summary = commissionService.getMySummary();
        return ResponseEntity.ok(BaseResponse.success(summary));
    }
}
