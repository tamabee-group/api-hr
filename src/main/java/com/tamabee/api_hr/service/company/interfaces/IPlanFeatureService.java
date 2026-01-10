package com.tamabee.api_hr.service.company.interfaces;

import com.tamabee.api_hr.dto.response.wallet.PlanFeaturesResponse;
import com.tamabee.api_hr.enums.FeatureCode;

import java.util.List;

/**
 * Service kiểm tra quyền truy cập tính năng theo gói subscription của công ty.
 * Mỗi plan có một tập hợp các feature codes được phép sử dụng.
 */
public interface IPlanFeatureService {

    /**
     * Kiểm tra công ty có quyền truy cập tính năng không
     *
     * @param companyId ID công ty
     * @param feature   mã tính năng cần kiểm tra
     * @return true nếu có quyền, false nếu không
     */
    boolean hasFeatureAccess(Long companyId, FeatureCode feature);

    /**
     * Validate quyền truy cập tính năng, throw exception nếu không có quyền
     *
     * @param companyId ID công ty
     * @param feature   mã tính năng cần kiểm tra
     * @throws com.tamabee.api_hr.exception.ForbiddenException nếu không có quyền
     */
    void validateFeatureAccess(Long companyId, FeatureCode feature);

    /**
     * Lấy danh sách feature codes mà công ty được phép sử dụng
     *
     * @param companyId ID công ty
     * @return danh sách feature codes
     */
    List<FeatureCode> getCompanyFeatures(Long companyId);

    /**
     * Lấy thông tin features của một plan
     *
     * @param planId ID plan
     * @return thông tin plan và danh sách features
     */
    PlanFeaturesResponse getPlanFeatures(Long planId);
}
