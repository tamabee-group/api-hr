package com.tamabee.api_hr.mapper.admin;

import com.tamabee.api_hr.dto.response.WalletOverviewResponse;
import com.tamabee.api_hr.dto.response.WalletResponse;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapper cho Wallet entity
 * Chuyển đổi giữa Entity và Response DTO
 */
@Component
public class WalletMapper {

    /**
     * Chuyển đổi WalletEntity sang WalletResponse
     *
     * @param entity   wallet entity
     * @param planName tên gói dịch vụ hiện tại
     * @return wallet response
     */
    public WalletResponse toResponse(WalletEntity entity, String planName) {
        if (entity == null) {
            return null;
        }

        WalletResponse response = new WalletResponse();
        response.setId(entity.getId());
        response.setCompanyId(entity.getCompanyId());
        response.setBalance(entity.getBalance());
        response.setLastBillingDate(entity.getLastBillingDate());
        response.setNextBillingDate(entity.getNextBillingDate());
        response.setFreeTrialEndDate(entity.getFreeTrialEndDate());
        response.setPlanName(planName);
        response.setIsFreeTrialActive(isInFreeTrial(entity.getFreeTrialEndDate()));

        return response;
    }

    /**
     * Chuyển đổi WalletEntity sang WalletOverviewResponse
     *
     * @param entity        wallet entity
     * @param companyName   tên công ty
     * @param planName      tên gói dịch vụ
     * @param totalDeposits tổng số tiền đã nạp
     * @param totalBillings tổng số tiền đã billing
     * @return wallet overview response
     */
    public WalletOverviewResponse toOverviewResponse(
            WalletEntity entity,
            String companyName,
            String planName,
            BigDecimal totalDeposits,
            BigDecimal totalBillings) {

        if (entity == null) {
            return null;
        }

        WalletOverviewResponse response = new WalletOverviewResponse();
        response.setId(entity.getId());
        response.setCompanyId(entity.getCompanyId());
        response.setCompanyName(companyName);
        response.setBalance(entity.getBalance());
        response.setLastBillingDate(entity.getLastBillingDate());
        response.setNextBillingDate(entity.getNextBillingDate());
        response.setFreeTrialEndDate(entity.getFreeTrialEndDate());
        response.setPlanName(planName);
        response.setIsFreeTrialActive(isInFreeTrial(entity.getFreeTrialEndDate()));
        response.setTotalDeposits(totalDeposits != null ? totalDeposits : BigDecimal.ZERO);
        response.setTotalBillings(totalBillings != null ? totalBillings : BigDecimal.ZERO);

        return response;
    }

    /**
     * Kiểm tra company có đang trong thời gian miễn phí không
     */
    private boolean isInFreeTrial(LocalDateTime freeTrialEndDate) {
        if (freeTrialEndDate == null) {
            return false;
        }
        return freeTrialEndDate.isAfter(LocalDateTime.now());
    }
}
