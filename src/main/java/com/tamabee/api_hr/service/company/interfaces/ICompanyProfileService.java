package com.tamabee.api_hr.service.company.interfaces;

import com.tamabee.api_hr.dto.request.company.UpdateCompanyProfileRequest;
import com.tamabee.api_hr.dto.response.company.CompanyProfileResponse;

/**
 * Service interface cho company profile
 */
public interface ICompanyProfileService {

    /**
     * Lấy thông tin công ty hiện tại
     */
    CompanyProfileResponse getMyCompanyProfile();

    /**
     * Cập nhật thông tin công ty
     */
    CompanyProfileResponse updateCompanyProfile(UpdateCompanyProfileRequest request);

    /**
     * Cập nhật logo công ty
     */
    CompanyProfileResponse updateLogo(String logoUrl);
}
