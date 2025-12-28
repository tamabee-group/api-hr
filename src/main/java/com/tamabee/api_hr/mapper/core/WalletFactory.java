package com.tamabee.api_hr.mapper.core;

import com.tamabee.api_hr.entity.wallet.WalletEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Factory để tạo WalletEntity mới
 * Đổi tên từ WalletMapper để tránh conflict với admin.WalletMapper
 */
@Component
public class WalletFactory {

    /**
     * Tạo Wallet mới cho company với freeTrialEndDate
     * 
     * @param companyId        ID của company
     * @param freeTrialEndDate ngày hết hạn free trial
     * @return WalletEntity mới
     */
    public WalletEntity createForCompany(Long companyId, LocalDateTime freeTrialEndDate) {
        WalletEntity wallet = new WalletEntity();
        wallet.setCompanyId(companyId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setLastBillingDate(LocalDateTime.now());
        // nextBillingDate được set sau khi hết free trial
        wallet.setNextBillingDate(freeTrialEndDate);
        wallet.setFreeTrialEndDate(freeTrialEndDate);

        return wallet;
    }

    /**
     * Tạo Wallet mới cho company (backward compatibility)
     * Sử dụng default 1 tháng nếu không có freeTrialEndDate
     * 
     * @param companyId ID của company
     * @return WalletEntity mới
     */
    public WalletEntity createForCompany(Long companyId) {
        return createForCompany(companyId, LocalDateTime.now().plusMonths(1));
    }
}
