package com.tamabee.api_hr.service.core.impl;

import com.tamabee.api_hr.dto.response.PlanFeaturesResponse;
import com.tamabee.api_hr.dto.response.PlanFeaturesResponse.FeatureItem;
import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.entity.wallet.PlanFeatureCodeEntity;
import com.tamabee.api_hr.enums.FeatureCode;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.PlanFeatureCodeRepository;
import com.tamabee.api_hr.repository.PlanRepository;
import com.tamabee.api_hr.service.core.IPlanFeaturesService;
import com.tamabee.api_hr.util.LocaleUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation của IPlanFeaturesService.
 * Lấy danh sách features của plan để frontend render sidebar động.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanFeaturesServiceImpl implements IPlanFeaturesService {

    private final PlanRepository planRepository;
    private final PlanFeatureCodeRepository planFeatureCodeRepository;

    @Override
    public PlanFeaturesResponse getFeaturesByPlanId(Long planId) {
        // Lấy plan
        PlanEntity plan = planRepository.findByIdAndDeletedFalse(planId)
                .orElseThrow(() -> NotFoundException.plan(planId));

        // Lấy danh sách feature codes của plan
        List<PlanFeatureCodeEntity> planFeatureCodes = planFeatureCodeRepository
                .findByPlanIdAndDeletedFalse(planId);

        Set<FeatureCode> enabledCodes = planFeatureCodes.stream()
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
                .planId(plan.getId())
                .planName(getPlanName(plan))
                .features(features)
                .build();
    }

    @Override
    public PlanFeaturesResponse getAllFeaturesEnabled() {
        // Tất cả features đều enabled cho Tamabee users
        List<FeatureItem> features = Arrays.stream(FeatureCode.values())
                .map(code -> FeatureItem.builder()
                        .code(code)
                        .name(getFeatureName(code))
                        .enabled(true)
                        .build())
                .collect(Collectors.toList());

        return PlanFeaturesResponse.builder()
                .planId(null)
                .planName("Tamabee")
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
}
