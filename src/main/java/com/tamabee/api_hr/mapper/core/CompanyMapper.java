package com.tamabee.api_hr.mapper.core;

import com.tamabee.api_hr.dto.response.CompanyResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.model.request.RegisterRequest;
import com.tamabee.api_hr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyMapper {

    private final UserRepository userRepository;

    /**
     * Chuyển đổi RegisterRequest sang CompanyEntity
     * Lưu ý: referredByEmployee được set từ referralCode
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

        // Tìm nhân viên tư vấn từ mã giới thiệu
        if (request.getReferralCode() != null && !request.getReferralCode().isEmpty()) {
            userRepository.findByEmployeeCodeAndDeletedFalse(request.getReferralCode())
                    .ifPresent(entity::setReferredByEmployee);
        }

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
