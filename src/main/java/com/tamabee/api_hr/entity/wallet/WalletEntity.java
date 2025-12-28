package com.tamabee.api_hr.entity.wallet;

import com.tamabee.api_hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity cho ví tiền của công ty
 * Mỗi company có một wallet duy nhất để thanh toán subscription
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_company_id", columnList = "companyId", unique = true),
        @Index(name = "idx_wallets_free_trial_end_date", columnList = "freeTrialEndDate")
})
public class WalletEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long companyId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    // Tổng số tiền billing đã tính cho company (dùng để tính eligibility của
    // commission)
    @Column(name = "total_billing", nullable = false, precision = 15, scale = 0)
    private BigDecimal totalBilling = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime lastBillingDate;

    @Column(nullable = false)
    private LocalDateTime nextBillingDate;

    // Ngày hết thời gian miễn phí (free trial)
    @Column(name = "free_trial_end_date")
    private LocalDateTime freeTrialEndDate;
}
