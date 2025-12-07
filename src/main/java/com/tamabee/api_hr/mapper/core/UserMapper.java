package com.tamabee.api_hr.mapper.core;

import com.tamabee.api_hr.dto.response.UserProfileResponse;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.model.request.RegisterRequest;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    /**
     * Convert RegisterRequest to UserEntity (partial)
     */
    public UserEntity toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        
        UserEntity entity = new UserEntity();
        entity.setEmail(request.getEmail());
        entity.setLocale(request.getLocale());
        entity.setLanguage(request.getLanguage());
        
        return entity;
    }
    
    /**
     * Convert UserEntity to UserResponse
     */
    public UserResponse toResponse(UserEntity entity) {
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
            profile.setBankName(entity.getProfile().getBankName());
            profile.setBankAccount(entity.getProfile().getBankAccount());
            profile.setBankAccountName(entity.getProfile().getBankAccountName());
            profile.setEmergencyContactName(entity.getProfile().getEmergencyContactName());
            profile.setEmergencyContactPhone(entity.getProfile().getEmergencyContactPhone());
            profile.setEmergencyContactRelation(entity.getProfile().getEmergencyContactRelation());
            profile.setEmergencyContactAddress(entity.getProfile().getEmergencyContactAddress());
            response.setProfile(profile);
        }
        
        return response;
    }
}
