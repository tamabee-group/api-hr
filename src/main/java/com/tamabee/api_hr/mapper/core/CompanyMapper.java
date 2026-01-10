package com.tamabee.api_hr.mapper.core;

import org.springframework.stereotype.Component;

import static com.tamabee.api_hr.constants.PlanConstants.FREE_PLAN_ID;
import com.tamabee.api_hr.dto.response.company.CompanyResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.dto.auth.RegisterRequest;

@Component
public class CompanyMapper {

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
        entity.setLocale(request.getLocale());
        entity.setLanguage(request.getLanguage());
        entity.setTenantDomain(request.getTenantDomain());

        // Tự động gán Free Plan khi đăng ký
        // Free Plan cho phép dùng full tính năng trong thời gian trial
        entity.setPlanId(FREE_PLAN_ID);

        // TODO: Xử lý referral code để tính commission sau
        // Referral code nằm trong tenant DB (tamabee_tamabee.user_profiles)
        // Cần implement cross-tenant query hoặc lưu referral code riêng

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
        response.setLocale(entity.getLocale());
        response.setLanguage(entity.getLanguage());

        // Lấy thông tin nhân viên tư vấn
        UserEntity referrer = entity.getReferredByEmployee();
        if (referrer != null) {
            response.setReferredByEmployeeCode(referrer.getEmployeeCode());
            if (referrer.getProfile() != null) {
                response.setReferredByEmployeeName(referrer.getProfile().getName());
            }
        }

        response.setLogo(entity.getLogo());
        response.setOwnerId(entity.getOwner() != null ? entity.getOwner().getId() : null);
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}
