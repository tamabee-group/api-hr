package com.tamabee.api_hr.mapper.core;

import com.tamabee.api_hr.dto.response.UserProfileResponse;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.model.request.RegisterRequest;
import com.tamabee.api_hr.util.LocaleUtil;
import org.springframework.stereotype.Component;

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
        return toResponse(entity, null);
    }

    /**
     * Convert UserEntity to UserResponse với companyName
     */
    public UserResponse toResponse(UserEntity entity, String companyName) {
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
        response.setCompanyId(entity.getCompanyId());
        response.setCompanyName(companyName);
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
