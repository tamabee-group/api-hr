package com.tamabee.api_hr.mapper.core;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.response.user.UserProfileResponse;
import com.tamabee.api_hr.dto.response.user.UserResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.dto.auth.RegisterRequest;
import com.tamabee.api_hr.util.LocaleUtil;

@Component
public class UserMapper {

    /**
     * Convert RegisterRequest to UserEntity (partial)
     * Chuyển đổi locale code (vi, ja) sang timezone (Asia/Ho_Chi_Minh, Asia/Tokyo)
     */
    public UserEntity toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setEmail(request.getEmail());
        // Chuyển đổi locale code sang timezone
        entity.setLocale(LocaleUtil.toTimezone(request.getLocale()));
        entity.setLanguage(request.getLanguage());

        return entity;
    }

    /**
     * Convert UserEntity to UserResponse
     */
    public UserResponse toResponse(UserEntity entity) {
        return toResponse(entity, null, null, null, null);
    }

    /**
     * Convert UserEntity to UserResponse với companyName
     */
    public UserResponse toResponse(UserEntity entity, String companyName) {
        return toResponse(entity, companyName, null, null, null);
    }

    /**
     * Convert UserEntity to UserResponse với đầy đủ thông tin
     * 
     * @param entity       UserEntity
     * @param companyName  Tên công ty
     * @param companyLogo  Logo công ty
     * @param tenantDomain Tenant domain ("tamabee" cho Tamabee users)
     * @param planId       Plan ID của company (null cho Tamabee users)
     */
    public UserResponse toResponse(UserEntity entity, String companyName, String companyLogo, String tenantDomain, Long planId) {
        if (entity == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(entity.getId());
        response.setEmployeeCode(entity.getEmployeeCode());
        response.setEmail(entity.getEmail());
        response.setRole(entity.getRole());
        response.setStatus(entity.getStatus());
        response.setLocale(entity.getLocale());
        response.setLanguage(entity.getLanguage());
        // companyId được tính từ tenantDomain, không lưu trong UserEntity
        response.setCompanyId(null);
        response.setCompanyName(companyName);
        response.setCompanyLogo(companyLogo);
        response.setTenantDomain(tenantDomain);
        response.setPlanId(planId);
        response.setProfileCompleteness(entity.getProfileCompleteness());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getProfile() != null) {
            UserProfileResponse profile = new UserProfileResponse();
            profile.setName(entity.getProfile().getName());
            profile.setPhone(entity.getProfile().getPhone());
            profile.setAddress(entity.getProfile().getAddress());
            profile.setZipCode(entity.getProfile().getZipCode());
            profile.setDateOfBirth(entity.getProfile().getDateOfBirth());
            profile.setGender(entity.getProfile().getGender());
            profile.setAvatar(entity.getProfile().getAvatar());
            profile.setReferralCode(entity.getProfile().getReferralCode());
            profile.setBankAccountType(entity.getProfile().getBankAccountType());
            profile.setJapanBankType(entity.getProfile().getJapanBankType());
            profile.setBankName(entity.getProfile().getBankName());
            profile.setBankAccount(entity.getProfile().getBankAccount());
            profile.setBankAccountName(entity.getProfile().getBankAccountName());
            profile.setBankCode(entity.getProfile().getBankCode());
            profile.setBankBranchCode(entity.getProfile().getBankBranchCode());
            profile.setBankBranchName(entity.getProfile().getBankBranchName());
            profile.setBankAccountCategory(entity.getProfile().getBankAccountCategory());
            profile.setBankSymbol(entity.getProfile().getBankSymbol());
            profile.setBankNumber(entity.getProfile().getBankNumber());
            profile.setEmergencyContactName(entity.getProfile().getEmergencyContactName());
            profile.setEmergencyContactPhone(entity.getProfile().getEmergencyContactPhone());
            profile.setEmergencyContactRelation(entity.getProfile().getEmergencyContactRelation());
            profile.setEmergencyContactAddress(entity.getProfile().getEmergencyContactAddress());
            response.setProfile(profile);
        }

        return response;
    }
}
