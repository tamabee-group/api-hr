package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.dto.request.wallet.TransactionFilterRequest;
import com.tamabee.api_hr.dto.response.wallet.WalletTransactionResponse;
import com.tamabee.api_hr.entity.wallet.WalletTransactionEntity;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.mapper.admin.WalletTransactionMapper;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.repository.wallet.WalletTransactionRepository;
import com.tamabee.api_hr.service.admin.interfaces.IWalletTransactionService;
import com.tamabee.api_hr.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service quản lý lịch sử giao dịch ví
 * Hỗ trợ tạo transaction, lấy danh sách theo walletId/companyId với filter và
 * pagination
 * Kết quả luôn được sắp xếp theo createdAt giảm dần (mới nhất trước)
 * 
 * Requirements: 4.1-4.6
 */
@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements IWalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletTransactionMapper walletTransactionMapper;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public WalletTransactionResponse create(
            Long walletId,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String description,
            Long referenceId) {

        WalletTransactionEntity entity = walletTransactionMapper.createEntity(
                walletId,
                type,
                amount,
                balanceBefore,
                balanceAfter,
                description,
                referenceId);

        WalletTransactionEntity savedEntity = walletTransactionRepository.save(entity);
        return walletTransactionMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getByWalletId(
            Long walletId,
            TransactionFilterRequest filter,
            Pageable pageable) {

        Page<WalletTransactionEntity> transactions = queryTransactionsByWalletId(walletId, filter, pageable);
        return transactions.map(walletTransactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getByCompanyId(
            Long companyId,
            TransactionFilterRequest filter,
            Pageable pageable) {

        Page<WalletTransactionEntity> transactions = queryTransactionsByCompanyId(companyId, filter, pageable);
        return transactions.map(walletTransactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getMyTransactions(
            TransactionFilterRequest filter,
            Pageable pageable) {

        Long companyId = getCurrentUserCompanyId();
        return getByCompanyId(companyId, filter, pageable);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Lấy companyId của user hiện tại từ JWT token
     */
    private Long getCurrentUserCompanyId() {
        return securityUtil.getCurrentUserCompanyId();
    }

    /**
     * Query transactions theo walletId với filter
     * Kết quả luôn được sắp xếp theo createdAt DESC (mới nhất trước)
     * 
     * Requirements: 4.5, 4.6
     */
    private Page<WalletTransactionEntity> queryTransactionsByWalletId(
            Long walletId,
            TransactionFilterRequest filter,
            Pageable pageable) {

        boolean hasType = filter != null && filter.getTransactionType() != null;
        boolean hasDateRange = filter != null && filter.getFromDate() != null && filter.getToDate() != null;

        if (hasType && hasDateRange) {
            return walletTransactionRepository.findByWalletIdAndTypeAndDateRange(
                    walletId, filter.getTransactionType(), filter.getFromDate(), filter.getToDate(), pageable);
        } else if (hasType) {
            return walletTransactionRepository.findByWalletIdAndTransactionTypeOrderByCreatedAtDesc(
                    walletId, filter.getTransactionType(), pageable);
        } else if (hasDateRange) {
            return walletTransactionRepository.findByWalletIdAndDateRange(
                    walletId, filter.getFromDate(), filter.getToDate(), pageable);
        } else {
            return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
        }
    }

    /**
     * Query transactions theo companyId với filter
     * Kết quả luôn được sắp xếp theo createdAt DESC (mới nhất trước)
     * 
     * Requirements: 4.5, 4.6
     */
    private Page<WalletTransactionEntity> queryTransactionsByCompanyId(
            Long companyId,
            TransactionFilterRequest filter,
            Pageable pageable) {

        boolean hasType = filter != null && filter.getTransactionType() != null;
        boolean hasDateRange = filter != null && filter.getFromDate() != null && filter.getToDate() != null;

        if (hasType && hasDateRange) {
            return walletTransactionRepository.findByCompanyIdAndTypeAndDateRange(
                    companyId, filter.getTransactionType(), filter.getFromDate(), filter.getToDate(), pageable);
        } else if (hasType) {
            return walletTransactionRepository.findByCompanyIdAndType(
                    companyId, filter.getTransactionType(), pageable);
        } else if (hasDateRange) {
            return walletTransactionRepository.findByCompanyIdAndDateRange(
                    companyId, filter.getFromDate(), filter.getToDate(), pageable);
        } else {
            return walletTransactionRepository.findByCompanyId(companyId, pageable);
        }
    }
}
