package com.tamabee.api_hr.controller.admin;

import com.tamabee.api_hr.dto.request.wallet.CommissionFilterRequest;
import com.tamabee.api_hr.dto.response.wallet.CommissionOverallSummaryResponse;
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
 * Controller quản lý hoa hồng cho Admin Tamabee
 * ADMIN_TAMABEE và MANAGER_TAMABEE có quyền truy cập
 */
@RestController
@RequestMapping("/api/admin/commissions")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_TAMABEE_ACCESS)
public class AdminCommissionController {

    private final ICommissionService commissionService;

    /**
     * Lấy danh sách tất cả commissions (phân trang)
     * GET /api/admin/commissions
     * 
     * @param page         số trang (mặc định 0)
     * @param size         số lượng mỗi trang (mặc định 20)
     * @param sortBy       sắp xếp theo field (mặc định createdAt)
     * @param sortDir      hướng sắp xếp (asc/desc, mặc định desc)
     * @param employeeCode filter theo employee code
     * @param status       filter theo status (PENDING, PAID)
     * @param fromDate     filter từ ngày
     * @param toDate       filter đến ngày
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<CommissionResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String employeeCode,
            @RequestParam(required = false) CommissionStatus status,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate) {

        // Tạo filter request
        CommissionFilterRequest filter = new CommissionFilterRequest();
        filter.setEmployeeCode(employeeCode);
        filter.setStatus(status);
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CommissionResponse> commissions = commissionService.getAll(filter, pageable);
        return ResponseEntity.ok(BaseResponse.success(commissions));
    }

    /**
     * Lấy tổng hợp hoa hồng toàn bộ hệ thống
     * GET /api/admin/commissions/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<CommissionOverallSummaryResponse>> getOverallSummary() {
        CommissionOverallSummaryResponse summary = commissionService.getOverallSummary();
        return ResponseEntity.ok(BaseResponse.success(summary));
    }

    /**
     * Lấy tổng hợp hoa hồng theo nhân viên
     * GET /api/admin/commissions/summary/{employeeCode}
     * 
     * @param employeeCode employee code của nhân viên Tamabee
     */
    @GetMapping("/summary/{employeeCode}")
    public ResponseEntity<BaseResponse<CommissionSummaryResponse>> getSummary(
            @PathVariable String employeeCode) {
        CommissionSummaryResponse summary = commissionService.getSummary(employeeCode);
        return ResponseEntity.ok(BaseResponse.success(summary));
    }

    /**
     * Đánh dấu commission đã thanh toán
     * POST /api/admin/commissions/{id}/paid
     * 
     * @param id ID của commission
     */
    @PostMapping("/{id}/paid")
    public ResponseEntity<BaseResponse<CommissionResponse>> markAsPaid(@PathVariable Long id) {
        CommissionResponse commission = commissionService.markAsPaid(id);
        return ResponseEntity.ok(BaseResponse.success(commission, "Đã đánh dấu thanh toán thành công"));
    }
}
