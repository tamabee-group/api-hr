package com.tamabee.api_hr.mapper.core;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.model.request.RegisterRequest;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {
    
    /**
     * Convert RegisterRequest to CompanyEntity
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
        entity.setReferredByEmployeeCode(request.getReferralCode());
        
        return entity;
    }
}
