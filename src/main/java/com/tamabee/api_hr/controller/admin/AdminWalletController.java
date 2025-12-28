package com.tamabee.api_hr.controller.admin;

import com.tamabee.api_hr.dto.request.DirectWalletRequest;
import com.tamabee.api_hr.dto.request.RefundRequest;
import com.tamabee.api_hr.dto.request.TransactionFilterRequest;
import com.tamabee.api_hr.dto.response.WalletOverviewResponse;
import com.tamabee.api_hr.dto.response.WalletResponse;
import com.tamabee.api_hr.dto.response.WalletStatisticsResponse;
import com.tamabee.api_hr.dto.response.WalletTransactionResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.admin.IWalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Controller quản lý ví tiền cho Admin Tamabee
 * ADMIN_TAMABEE, MANAGER_TAMABEE có quyền đầy đủ
 * EMPLOYEE_TAMABEE có quyền đọc (read-only) để hỗ trợ company
 */
@RestController
@RequestMapping("/api/admin/wallets")
@RequiredArgsConstructor
public class AdminWalletController {

    private final IWalletService walletService;

    /**
     * Lấy danh sách tổng quan wallet của tất cả company (phân trang)
     * GET /api/admin/wallets
     * ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE có quyền đọc
     * 
     * @param page       số trang (mặc định 0)
     * @param size       số lượng mỗi trang (mặc định 20)
     * @param sortBy     sắp xếp theo field (mặc định createdAt)
     * @param sortDir    hướng sắp xếp (asc/desc, mặc định desc)
     * @param minBalance số dư tối thiểu (filter)
     * @param maxBalance số dư tối đa (filter)
     */
    @GetMapping
    @PreAuthorize(RoleConstants.HAS_ALL_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<Page<WalletOverviewResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) BigDecimal maxBalance) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WalletOverviewResponse> wallets = walletService.getOverview(minBalance, maxBalance, pageable);
        return ResponseEntity.ok(BaseResponse.success(wallets));
    }

    /**
     * Lấy thông tin wallet của company cụ thể
     * GET /api/admin/wallets/{companyId}
     * ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE có quyền đọc
     */
    @GetMapping("/{companyId}")
    @PreAuthorize(RoleConstants.HAS_ALL_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<WalletResponse>> getByCompanyId(@PathVariable Long companyId) {
        WalletResponse wallet = walletService.getByCompanyId(companyId);
        return ResponseEntity.ok(BaseResponse.success(wallet));
    }

    /**
     * Lấy lịch sử giao dịch của company cụ thể
     * GET /api/admin/wallets/{companyId}/transactions
     * ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE có quyền đọc
     * 
     * @param companyId       ID của company
     * @param page            số trang (mặc định 0)
     * @param size            số lượng mỗi trang (mặc định 20)
     * @param transactionType loại giao dịch (filter)
     * @param fromDate        từ ngày (filter)
     * @param toDate          đến ngày (filter)
     */
    @GetMapping("/{companyId}/transactions")
    @PreAuthorize(RoleConstants.HAS_ALL_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<Page<WalletTransactionResponse>>> getTransactionsByCompanyId(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate) {

        // Tạo filter request
        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setTransactionType(transactionType);
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);

        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionResponse> transactions = walletService.getTransactionsByCompanyId(companyId, filter,
                pageable);

        return ResponseEntity.ok(BaseResponse.success(transactions));
    }

    /**
     * Lấy thống kê tổng hợp wallet
     * GET /api/admin/wallets/statistics
     * ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE có quyền đọc
     */
    @GetMapping("/statistics")
    @PreAuthorize(RoleConstants.HAS_ALL_TAMABEE_ACCESS)
    public ResponseEntity<BaseResponse<WalletStatisticsResponse>> getStatistics() {
        WalletStatisticsResponse statistics = walletService.getStatistics();
        return ResponseEntity.ok(BaseResponse.success(statistics));
    }

    /**
     * Hoàn tiền vào wallet của company
     * POST /api/admin/wallets/{companyId}/refund
     * Chỉ ADMIN_TAMABEE có quyền
     */
    @PostMapping("/{companyId}/refund")
    @PreAuthorize(RoleConstants.HAS_ADMIN_TAMABEE)
    public ResponseEntity<BaseResponse<WalletTransactionResponse>> createRefund(
            @PathVariable Long companyId,
            @Valid @RequestBody RefundRequest request) {
        WalletTransactionResponse transaction = walletService.createRefund(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(transaction, "Hoàn tiền thành công"));
    }

    // ==================== Direct Wallet Operations (Admin Only)
    // ====================

    /**
     * Thêm tiền trực tiếp vào wallet của company
     * POST /api/admin/wallets/{companyId}/add
     * Chỉ ADMIN_TAMABEE có quyền - Manager không được phép
     * Requirements: 1.2, 1.3, 1.4
     */
    @PostMapping("/{companyId}/add")
    @PreAuthorize(RoleConstants.HAS_ADMIN_TAMABEE_ONLY)
    public ResponseEntity<BaseResponse<WalletTransactionResponse>> addBalanceDirect(
            @PathVariable Long companyId,
            @Valid @RequestBody DirectWalletRequest request) {
        WalletTransactionResponse transaction = walletService.addBalanceDirect(
                companyId, request.getAmount(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(transaction, "Thêm tiền trực tiếp thành công"));
    }

    /**
     * Trừ tiền trực tiếp từ wallet của company
     * POST /api/admin/wallets/{companyId}/deduct
     * Chỉ ADMIN_TAMABEE có quyền - Manager không được phép
     * Requirements: 1.2, 1.3, 1.4
     */
    @PostMapping("/{companyId}/deduct")
    @PreAuthorize(RoleConstants.HAS_ADMIN_TAMABEE_ONLY)
    public ResponseEntity<BaseResponse<WalletTransactionResponse>> deductBalanceDirect(
            @PathVariable Long companyId,
            @Valid @RequestBody DirectWalletRequest request) {
        WalletTransactionResponse transaction = walletService.deductBalanceDirect(
                companyId, request.getAmount(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(transaction, "Trừ tiền trực tiếp thành công"));
    }
}
