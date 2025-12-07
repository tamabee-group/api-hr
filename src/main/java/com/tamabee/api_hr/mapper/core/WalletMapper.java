package com.tamabee.api_hr.mapper.core;

import com.tamabee.api_hr.entity.wallet.WalletEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class WalletMapper {
    
    /**
     * Tạo Wallet mới cho company
     */
    public WalletEntity createForCompany(Long companyId) {
        WalletEntity wallet = new WalletEntity();
        wallet.setCompanyId(companyId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setLastBillingDate(LocalDateTime.now());
        wallet.setNextBillingDate(LocalDateTime.now().plusMonths(1));
        
        return wallet;
    }
}
