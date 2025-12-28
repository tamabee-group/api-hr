package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.request.TransactionFilterRequest;
import com.tamabee.api_hr.dto.response.WalletTransactionResponse;
import com.tamabee.api_hr.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Service quản lý lịch sử giao dịch ví
 * Hỗ trợ tạo transaction, lấy danh sách theo walletId/companyId với filter và
 * pagination
 * 
 * Requirements: 4.1-4.6
 */
public interface IWalletTransactionService {

    /**
     * Tạo transaction mới
     * Dùng nội bộ khi thực hiện các thao tác balance
     *
     * @param walletId      ID của wallet
     * @param type          loại giao dịch (DEPOSIT, BILLING, REFUND, COMMISSION)
     * @param amount        số tiền giao dịch
     * @param balanceBefore số dư trước giao dịch
     * @param balanceAfter  số dư sau giao dịch
     * @param description   mô tả giao dịch
     * @param referenceId   ID tham chiếu (deposit_request_id, commission_id, hoặc
     *                      null)
     * @return transaction đã tạo
     */
    WalletTransactionResponse create(
            Long walletId,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String description,
            Long referenceId);

    /**
     * Lấy lịch sử giao dịch theo walletId (phân trang)
     * Kết quả được sắp xếp theo createdAt giảm dần (mới nhất trước)
     *
     * @param walletId ID của wallet
     * @param filter   filter theo transactionType và khoảng thời gian
     * @param pageable thông tin phân trang (mặc định page size = 20)
     * @return danh sách transactions
     * 
     *         Requirements: 4.4, 4.5, 4.6
     */
    Page<WalletTransactionResponse> getByWalletId(
            Long walletId,
            TransactionFilterRequest filter,
            Pageable pageable);

    /**
     * Lấy lịch sử giao dịch theo companyId (phân trang)
     * Kết quả được sắp xếp theo createdAt giảm dần (mới nhất trước)
     *
     * @param companyId ID của company
     * @param filter    filter theo transactionType và khoảng thời gian
     * @param pageable  thông tin phân trang (mặc định page size = 20)
     * @return danh sách transactions
     * 
     *         Requirements: 4.1, 4.2, 4.4, 4.5, 4.6
     */
    Page<WalletTransactionResponse> getByCompanyId(
            Long companyId,
            TransactionFilterRequest filter,
            Pageable pageable);

    /**
     * Lấy lịch sử giao dịch của company hiện tại (từ JWT token)
     * Kết quả được sắp xếp theo createdAt giảm dần (mới nhất trước)
     *
     * @param filter   filter theo transactionType và khoảng thời gian
     * @param pageable thông tin phân trang (mặc định page size = 20)
     * @return danh sách transactions
     * 
     *         Requirements: 4.1, 4.4, 4.5, 4.6
     */
    Page<WalletTransactionResponse> getMyTransactions(
            TransactionFilterRequest filter,
            Pageable pageable);
}
