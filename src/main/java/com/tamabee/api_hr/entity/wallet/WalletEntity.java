package com.tamabee.api_hr.entity.wallet;

import com.tamabee.api_hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_company_id", columnList = "companyId", unique = true)
})
public class WalletEntity extends BaseEntity {
    
    @Column(nullable = false, unique = true)
    private Long companyId;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private LocalDateTime lastBillingDate;
    
    @Column(nullable = false)
    private LocalDateTime nextBillingDate;
}
