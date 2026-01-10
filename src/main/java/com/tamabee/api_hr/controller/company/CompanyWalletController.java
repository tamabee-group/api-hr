package com.tamabee.api_hr.controller.company;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.request.TransactionFilterRequest;
import com.tamabee.api_hr.dto.response.WalletResponse;
import com.tamabee.api_hr.dto.response.WalletTransactionResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.company.ICompanyWalletService;

import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý ví tiền cho Company
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập
 */
@RestController
@RequestMapping("/api/company/wallet")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class CompanyWalletController {

    private final ICompanyWalletService companyWalletService;

    /**
     * Lấy thông tin ví của company hiện tại
     * GET /api/company/wallet
     */
    @GetMapping
    public ResponseEntity<BaseResponse<WalletResponse>> getMyWallet() {
        WalletResponse wallet = companyWalletService.getMyWallet();
        return ResponseEntity.ok(BaseResponse.success(wallet));
    }

    /**
     * Lấy lịch sử giao dịch của company hiện tại
     * GET /api/company/wallet/transactions
     * 
     * @param page            số trang (mặc định 0)
     * @param size            số lượng mỗi trang (mặc định 20)
     * @param transactionType loại giao dịch (filter)
     * @param fromDate        từ ngày (filter)
     * @param toDate          đến ngày (filter)
     */
    @GetMapping("/transactions")
    public ResponseEntity<BaseResponse<Page<WalletTransactionResponse>>> getMyTransactions(
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
        Page<WalletTransactionResponse> transactions = companyWalletService.getMyTransactions(filter, pageable);

        return ResponseEntity.ok(BaseResponse.success(transactions));
    }
}
