package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.response.PlanFeaturesResponse;
import com.tamabee.api_hr.dto.response.PlanFeaturesResponse.FeatureItem;
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
import com.tamabee.api_hr.util.LocaleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
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

        // Lấy danh sách feature codes của plan
        List<PlanFeatureCodeEntity> featureEntities = planFeatureCodeRepository.findByPlanIdAndDeletedFalse(planId);

        Set<FeatureCode> enabledCodes = featureEntities.stream()
                .map(PlanFeatureCodeEntity::getFeatureCode)
                .collect(Collectors.toSet());

        // Tạo danh sách tất cả features với enabled status
        List<FeatureItem> features = Arrays.stream(FeatureCode.values())
                .map(code -> FeatureItem.builder()
                        .code(code)
                        .name(getFeatureName(code))
                        .enabled(enabledCodes.contains(code))
                        .build())
                .collect(Collectors.toList());

        return PlanFeaturesResponse.builder()
                .planId(planId)
                .planName(getPlanName(plan))
                .features(features)
                .build();
    }

    /**
     * Lấy tên plan theo ngôn ngữ hiện tại
     */
    private String getPlanName(PlanEntity plan) {
        String locale = LocaleUtil.getCurrentLocale();
        return switch (locale) {
            case "en" -> plan.getNameEn();
            case "ja" -> plan.getNameJa();
            default -> plan.getNameVi();
        };
    }

    /**
     * Lấy tên feature theo ngôn ngữ hiện tại
     */
    private String getFeatureName(FeatureCode code) {
        String locale = LocaleUtil.getCurrentLocale();
        return switch (code) {
            case ATTENDANCE -> switch (locale) {
                case "en" -> "Attendance";
                case "ja" -> "勤怠管理";
                default -> "Chấm công";
            };
            case PAYROLL -> switch (locale) {
                case "en" -> "Payroll";
                case "ja" -> "給与計算";
                default -> "Tính lương";
            };
            case OVERTIME -> switch (locale) {
                case "en" -> "Overtime";
                case "ja" -> "残業管理";
                default -> "Tăng ca";
            };
            case LEAVE_MANAGEMENT -> switch (locale) {
                case "en" -> "Leave Management";
                case "ja" -> "休暇管理";
                default -> "Quản lý nghỉ phép";
            };
            case GEO_LOCATION -> switch (locale) {
                case "en" -> "Geo Location";
                case "ja" -> "位置情報";
                default -> "Chấm công theo vị trí";
            };
            case DEVICE_REGISTRATION -> switch (locale) {
                case "en" -> "Device Registration";
                case "ja" -> "デバイス登録";
                default -> "Đăng ký thiết bị";
            };
            case REPORTS -> switch (locale) {
                case "en" -> "Reports";
                case "ja" -> "レポート";
                default -> "Báo cáo";
            };
            case FLEXIBLE_SCHEDULE -> switch (locale) {
                case "en" -> "Flexible Schedule";
                case "ja" -> "フレックス勤務";
                default -> "Lịch làm việc linh hoạt";
            };
        };
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
