package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho lịch sử giao dịch ví
 */
@Data
public class WalletTransactionResponse {

    private Long id;

    private Long walletId;

    private TransactionType transactionType;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String description;

    private Long referenceId;

    private LocalDateTime createdAt;
}
