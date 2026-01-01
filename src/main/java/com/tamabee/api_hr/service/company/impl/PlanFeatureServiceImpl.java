package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.response.PlanFeaturesResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.entity.wallet.PlanFeatureCodeEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.FeatureCode;
import com.tamabee.api_hr.exception.ForbiddenException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.PlanFeatureCodeRepository;
import com.tamabee.api_hr.repository.PlanRepository;
import com.tamabee.api_hr.service.company.IPlanFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation kiểm tra quyền truy cập tính năng theo gói
 * subscription.
 * Mỗi plan có một tập hợp các feature codes được phép sử dụng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanFeatureServiceImpl implements IPlanFeatureService {

    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    private final PlanFeatureCodeRepository planFeatureCodeRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasFeatureAccess(Long companyId, FeatureCode feature) {
        // Lấy planId của công ty
        Long planId = getCompanyPlanId(companyId);
        if (planId == null) {
            log.warn("Công ty {} chưa đăng ký gói dịch vụ, từ chối truy cập feature: {}", companyId, feature);
            return false;
        }

        // Kiểm tra feature có trong plan không
        boolean hasAccess = planFeatureCodeRepository.existsByPlanIdAndFeatureCodeAndDeletedFalse(planId, feature);

        if (!hasAccess) {
            log.info("Công ty {} (plan: {}) không có quyền truy cập feature: {}", companyId, planId, feature);
        }

        return hasAccess;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateFeatureAccess(Long companyId, FeatureCode feature) {
        // Lấy planId của công ty
        Long planId = getCompanyPlanId(companyId);
        if (planId == null) {
            log.warn("Công ty {} chưa đăng ký gói dịch vụ, từ chối truy cập feature: {}", companyId, feature);
            throw new ForbiddenException(ErrorCode.COMPANY_NO_PLAN);
        }

        // Kiểm tra feature có trong plan không
        boolean hasAccess = planFeatureCodeRepository.existsByPlanIdAndFeatureCodeAndDeletedFalse(planId, feature);

        if (!hasAccess) {
            log.warn("Công ty {} (plan: {}) bị từ chối truy cập feature: {}", companyId, planId, feature);
            throw new ForbiddenException(ErrorCode.FEATURE_NOT_AVAILABLE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureCode> getCompanyFeatures(Long companyId) {
        // Lấy planId của công ty
        Long planId = getCompanyPlanId(companyId);
        if (planId == null) {
            log.info("Công ty {} chưa đăng ký gói dịch vụ, trả về danh sách features rỗng", companyId);
            return List.of();
        }

        // Lấy danh sách feature codes của plan
        List<PlanFeatureCodeEntity> featureEntities = planFeatureCodeRepository.findByPlanIdAndDeletedFalse(planId);

        return featureEntities.stream()
                .map(PlanFeatureCodeEntity::getFeatureCode)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PlanFeaturesResponse getPlanFeatures(Long planId) {
        // Kiểm tra plan tồn tại
        PlanEntity plan = planRepository.findByIdAndDeletedFalse(planId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PLAN_NOT_FOUND));

        // Lấy danh sách feature codes
        List<PlanFeatureCodeEntity> featureEntities = planFeatureCodeRepository.findByPlanIdAndDeletedFalse(planId);

        return PlanFeaturesResponse.builder()
                .planId(planId)
                .planName(plan.getNameEn())
                .features(featureEntities.stream()
                        .map(PlanFeatureCodeEntity::getFeatureCode)
                        .collect(Collectors.toCollection(HashSet::new)))
                .build();
    }

    /**
     * Lấy planId của công ty
     */
    private Long getCompanyPlanId(Long companyId) {
        return companyRepository.findById(companyId)
                .map(CompanyEntity::getPlanId)
                .orElse(null);
    }
}
