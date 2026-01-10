package com.tamabee.api_hr.service.company.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tamabee.api_hr.dto.request.wallet.TransactionFilterRequest;
import com.tamabee.api_hr.dto.response.wallet.WalletResponse;
import com.tamabee.api_hr.dto.response.wallet.WalletTransactionResponse;

/**
 * Service interface cho company wallet
 * Sử dụng masterJdbcTemplate vì wallets và wallet_transactions nằm trong master DB
 */
public interface ICompanyWalletService {

    /**
     * Lấy thông tin ví của company hiện tại
     */
    WalletResponse getMyWallet();

    /**
     * Lấy lịch sử giao dịch của company hiện tại
     */
    Page<WalletTransactionResponse> getMyTransactions(TransactionFilterRequest filter, Pageable pageable);
}
