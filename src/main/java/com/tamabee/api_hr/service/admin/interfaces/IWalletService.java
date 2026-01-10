package com.tamabee.api_hr.service.admin.interfaces;

import com.tamabee.api_hr.dto.request.wallet.RefundRequest;
import com.tamabee.api_hr.dto.request.wallet.TransactionFilterRequest;
import com.tamabee.api_hr.dto.response.wallet.WalletOverviewResponse;
import com.tamabee.api_hr.dto.response.wallet.WalletResponse;
import com.tamabee.api_hr.dto.response.wallet.WalletStatisticsResponse;
import com.tamabee.api_hr.dto.response.wallet.WalletTransactionResponse;
import com.tamabee.api_hr.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Service quản lý ví tiền của công ty
 * Hỗ trợ xem thông tin ví, thêm/trừ số dư, hoàn tiền và thống kê
 */
public interface IWalletService {

        // ==================== View Operations ====================

        /**
         * Lấy thông tin wallet theo companyId
         * Dùng cho Admin Tamabee xem wallet của company cụ thể
         *
         * @param companyId ID của company
         * @return thông tin wallet
         */
        WalletResponse getByCompanyId(Long companyId);

        /**
         * Lấy thông tin wallet của company hiện tại (từ JWT token)
         * Dùng cho ADMIN_COMPANY, MANAGER_COMPANY xem wallet của mình
         *
         * @return thông tin wallet
         */
        WalletResponse getMyWallet();

        // ==================== Balance Operations ====================

        /**
         * Cộng tiền vào wallet
         * Tự động tạo transaction record
         *
         * @param companyId   ID của company
         * @param amount      số tiền cộng (phải > 0)
         * @param description mô tả giao dịch
         * @param type        loại giao dịch (DEPOSIT, REFUND)
         * @param referenceId ID tham chiếu (deposit_request_id hoặc null)
         * @return transaction đã tạo
         */
        WalletTransactionResponse addBalance(Long companyId, BigDecimal amount, String description,
                        TransactionType type, Long referenceId);

        /**
         * Trừ tiền từ wallet
         * Tự động tạo transaction record
         *
         * @param companyId   ID của company
         * @param amount      số tiền trừ (phải > 0)
         * @param description mô tả giao dịch
         * @param type        loại giao dịch (BILLING)
         * @param referenceId ID tham chiếu (null cho billing)
         * @return transaction đã tạo
         */
        WalletTransactionResponse deductBalance(Long companyId, BigDecimal amount, String description,
                        TransactionType type, Long referenceId);

        // ==================== Admin Operations ====================

        /**
         * Lấy danh sách tổng quan wallet của tất cả company (phân trang)
         * Dùng cho Admin Dashboard
         *
         * @param minBalance số dư tối thiểu (filter)
         * @param maxBalance số dư tối đa (filter)
         * @param pageable   thông tin phân trang
         * @return danh sách wallet overview
         */
        Page<WalletOverviewResponse> getOverview(BigDecimal minBalance, BigDecimal maxBalance, Pageable pageable);

        /**
         * Lấy thống kê tổng hợp wallet
         * Dùng cho Admin Dashboard
         *
         * @return thống kê wallet
         */
        WalletStatisticsResponse getStatistics();

        /**
         * Hoàn tiền vào wallet của company
         * Chỉ ADMIN_TAMABEE có quyền
         *
         * @param companyId ID của company
         * @param request   thông tin hoàn tiền (amount, reason)
         * @return transaction đã tạo
         */
        WalletTransactionResponse createRefund(Long companyId, RefundRequest request);

        // ==================== Direct Wallet Operations (Admin Only)
        // ====================

        /**
         * Thêm tiền trực tiếp vào wallet - CHỈ ADMIN TAMABEE
         * Tạo transaction log với đầy đủ thông tin (timestamp, operator, amount,
         * balance before/after)
         * Requirements: 1.3, 1.4, 1.5
         *
         * @param companyId   ID của company
         * @param amount      số tiền cần thêm (phải > 0)
         * @param description mô tả giao dịch
         * @return transaction đã tạo với đầy đủ thông tin
         */
        WalletTransactionResponse addBalanceDirect(Long companyId, BigDecimal amount, String description);

        /**
         * Trừ tiền trực tiếp từ wallet - CHỈ ADMIN TAMABEE
         * Tạo transaction log với đầy đủ thông tin (timestamp, operator, amount,
         * balance before/after)
         * Requirements: 1.3, 1.4, 1.5
         *
         * @param companyId   ID của company
         * @param amount      số tiền cần trừ (phải > 0)
         * @param description mô tả giao dịch
         * @return transaction đã tạo với đầy đủ thông tin
         */
        WalletTransactionResponse deductBalanceDirect(Long companyId, BigDecimal amount, String description);

        // ==================== Transaction Operations ====================

        /**
         * Lấy lịch sử giao dịch theo walletId (phân trang)
         *
         * @param walletId ID của wallet
         * @param filter   filter theo transactionType và khoảng thời gian
         * @param pageable thông tin phân trang
         * @return danh sách transactions
         */
        Page<WalletTransactionResponse> getTransactionsByWalletId(Long walletId, TransactionFilterRequest filter,
                        Pageable pageable);

        /**
         * Lấy lịch sử giao dịch theo companyId (phân trang)
         *
         * @param companyId ID của company
         * @param filter    filter theo transactionType và khoảng thời gian
         * @param pageable  thông tin phân trang
         * @return danh sách transactions
         */
        Page<WalletTransactionResponse> getTransactionsByCompanyId(Long companyId, TransactionFilterRequest filter,
                        Pageable pageable);

        /**
         * Lấy lịch sử giao dịch của company hiện tại (từ JWT token)
         *
         * @param filter   filter theo transactionType và khoảng thời gian
         * @param pageable thông tin phân trang
         * @return danh sách transactions
         */
        Page<WalletTransactionResponse> getMyTransactions(TransactionFilterRequest filter, Pageable pageable);
}
