package com.tamabee.api_hr.mapper.admin;

import com.tamabee.api_hr.dto.response.WalletTransactionResponse;
import com.tamabee.api_hr.entity.wallet.WalletTransactionEntity;
import com.tamabee.api_hr.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper cho WalletTransaction entity
 * Chuyển đổi giữa Entity và Response DTO
 */
@Component
public class WalletTransactionMapper {

    /**
     * Tạo WalletTransactionEntity mới
     * Dùng khi tạo giao dịch mới (deposit, billing, refund, commission)
     */
    public WalletTransactionEntity createEntity(
            Long walletId,
            TransactionType transactionType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String description,
            Long referenceId) {

        WalletTransactionEntity entity = new WalletTransactionEntity();
        entity.setWalletId(walletId);
        entity.setTransactionType(transactionType);
        entity.setAmount(amount);
        entity.setBalanceBefore(balanceBefore);
        entity.setBalanceAfter(balanceAfter);
        entity.setDescription(description);
        entity.setReferenceId(referenceId);

        return entity;
    }

    /**
     * Chuyển đổi WalletTransactionEntity sang WalletTransactionResponse
     */
    public WalletTransactionResponse toResponse(WalletTransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        WalletTransactionResponse response = new WalletTransactionResponse();
        response.setId(entity.getId());
        response.setWalletId(entity.getWalletId());
        response.setTransactionType(entity.getTransactionType());
        response.setAmount(entity.getAmount());
        response.setBalanceBefore(entity.getBalanceBefore());
        response.setBalanceAfter(entity.getBalanceAfter());
        response.setDescription(entity.getDescription());
        response.setReferenceId(entity.getReferenceId());
        response.setCreatedAt(entity.getCreatedAt());

        return response;
    }
}
