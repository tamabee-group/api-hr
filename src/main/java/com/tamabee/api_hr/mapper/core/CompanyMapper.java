package com.tamabee.api_hr.mapper.core;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static com.tamabee.api_hr.constants.PlanConstants.FREE_PLAN_ID;
import com.tamabee.api_hr.dto.auth.RegisterRequest;
import com.tamabee.api_hr.dto.response.company.CompanyResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.repository.wallet.PlanRepository;
import com.tamabee.api_hr.repository.wallet.WalletRepository;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;

@Component
public class CompanyMapper {

    private final PlanRepository planRepository;
    private final WalletRepository walletRepository;

    public CompanyMapper(@Lazy PlanRepository planRepository, @Lazy WalletRepository walletRepository) {
        this.planRepository = planRepository;
        this.walletRepository = walletRepository;
    }

    /**
     * Chuyển đổi RegisterRequest sang CompanyEntity
     * Company mới đăng ký sẽ tự động được gán Free Plan (planId = 0)
     */
    public CompanyEntity toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }

        CompanyEntity entity = new CompanyEntity();
        entity.setName(request.getCompanyName());
        entity.setOwnerName(request.getOwnerName());
        entity.setEmail(request.getEmail());
        entity.setPhone(request.getPhone());
        entity.setAddress(request.getAddress());
        entity.setIndustry(request.getIndustry());
        entity.setZipcode(request.getZipcode());
        entity.setRegion(request.getRegion());
        entity.setLanguage(request.getLanguage());
        entity.setTenantDomain(request.getTenantDomain());

        // Tự động gán Free Plan khi đăng ký
        entity.setPlanId(FREE_PLAN_ID);

        return entity;
    }

    /**
     * Chuyển đổi CompanyEntity sang CompanyResponse
     */
    public CompanyResponse toResponse(CompanyEntity entity) {
        if (entity == null) {
            return null;
        }

        CompanyResponse response = new CompanyResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setOwnerName(entity.getOwnerName());
        response.setEmail(entity.getEmail());
        response.setPhone(entity.getPhone());
        response.setAddress(entity.getAddress());
        response.setIndustry(entity.getIndustry());
        response.setZipcode(entity.getZipcode());
        response.setRegion(entity.getRegion());
        response.setLanguage(entity.getLanguage());
        response.setTenantDomain(entity.getTenantDomain());
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);

        // referredByEmployeeId - thông tin chi tiết được lấy riêng qua EmployeeReferralService
        response.setReferredByEmployeeId(entity.getReferredByEmployeeId());

        response.setLogo(entity.getLogo());
        response.setOwnerId(entity.getOwner() != null ? entity.getOwner().getId() : null);
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        // Map plan info
        response.setPlanId(entity.getPlanId());
        if (entity.getPlanId() != null) {
            planRepository.findByIdAndDeletedFalse(entity.getPlanId()).ifPresent(plan -> {
                response.setPlanNameVi(plan.getNameVi());
                response.setPlanNameEn(plan.getNameEn());
                response.setPlanNameJa(plan.getNameJa());
                response.setPlanMonthlyPrice(plan.getMonthlyPrice());
                response.setPlanMaxEmployees(plan.getMaxEmployees());
            });
        }

        // Map wallet info
        walletRepository.findByCompanyIdAndDeletedFalse(entity.getId()).ifPresent(wallet -> {
            response.setWalletBalance(wallet.getBalance());
            response.setLastBillingDate(wallet.getLastBillingDate());
            response.setNextBillingDate(wallet.getNextBillingDate());
            response.setFreeTrialEndDate(wallet.getFreeTrialEndDate());
            // Tính toán free trial active
            boolean isFreeTrialActive = wallet.getFreeTrialEndDate() != null 
                && wallet.getFreeTrialEndDate().isAfter(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));
            response.setIsFreeTrialActive(isFreeTrialActive);
        });

        return response;
    }
}
