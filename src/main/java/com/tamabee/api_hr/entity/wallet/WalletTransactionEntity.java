package com.tamabee.api_hr.entity.wallet;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Entity cho lịch sử giao dịch ví
 * Ghi nhận các giao dịch: DEPOSIT, BILLING, REFUND, COMMISSION
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wallet_transactions_wallet_id", columnList = "walletId"),
        @Index(name = "idx_wallet_transactions_transaction_type", columnList = "transactionType"),
        @Index(name = "idx_wallet_transactions_created_at", columnList = "createdAt"),
        @Index(name = "idx_wallet_transactions_deleted", columnList = "deleted"),
        @Index(name = "idx_wallet_transactions_wallet_id_deleted", columnList = "walletId, deleted")
})
public class WalletTransactionEntity extends BaseEntity {

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 500)
    private String description;

    // ID tham chiếu đến deposit_request hoặc commission
    @Column(name = "reference_id")
    private Long referenceId;
}
