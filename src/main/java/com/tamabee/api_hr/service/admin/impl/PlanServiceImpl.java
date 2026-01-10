package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.dto.request.wallet.PlanCreateRequest;
import com.tamabee.api_hr.dto.request.wallet.PlanFeatureRequest;
import com.tamabee.api_hr.dto.request.wallet.PlanUpdateRequest;
import com.tamabee.api_hr.dto.response.wallet.PlanResponse;
import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.entity.wallet.PlanFeatureEntity;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.admin.PlanFeatureMapper;
import com.tamabee.api_hr.mapper.admin.PlanMapper;
import com.tamabee.api_hr.repository.wallet.PlanFeatureRepository;
import com.tamabee.api_hr.repository.wallet.PlanRepository;
import com.tamabee.api_hr.service.admin.interfaces.IPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản lý gói dịch vụ (Plan)
 * Hỗ trợ CRUD operations và quản lý features
 */
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements IPlanService {

    private final PlanRepository planRepository;
    private final PlanFeatureRepository planFeatureRepository;
    private final PlanMapper planMapper;
    private final PlanFeatureMapper planFeatureMapper;

    @Override
    @Transactional
    public PlanResponse create(PlanCreateRequest request) {
        // Tạo plan entity
        PlanEntity planEntity = planMapper.toEntity(request);
        PlanEntity savedPlan = planRepository.save(planEntity);

        // Tạo features nếu có
        List<PlanFeatureEntity> features = null;
        if (request.getFeatures() != null && !request.getFeatures().isEmpty()) {
            features = createFeatures(savedPlan.getId(), request.getFeatures());
        }

        return planMapper.toResponse(savedPlan, features);
    }

    @Override
    @Transactional
    public PlanResponse update(Long id, PlanUpdateRequest request) {
        // Tìm plan
        PlanEntity planEntity = planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> NotFoundException.plan(id));

        // Cập nhật thông tin plan
        planMapper.updateEntity(planEntity, request);
        PlanEntity savedPlan = planRepository.save(planEntity);

        // Cập nhật features nếu có trong request
        List<PlanFeatureEntity> features;
        if (request.getFeatures() != null) {
            // Xóa features cũ (soft delete)
            deleteExistingFeatures(id);
            // Tạo features mới
            features = createFeatures(id, request.getFeatures());
        } else {
            // Lấy features hiện tại
            features = planFeatureRepository.findByPlanIdAndDeletedFalseOrderBySortOrderAsc(id);
        }

        return planMapper.toResponse(savedPlan, features);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // Tìm plan
        PlanEntity planEntity = planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> NotFoundException.plan(id));

        // Kiểm tra plan có đang được sử dụng không
        long companiesUsingPlan = planRepository.countCompaniesUsingPlan(id);
        if (companiesUsingPlan > 0) {
            throw BadRequestException.planInUse(id);
        }

        // Soft delete plan
        planEntity.setDeleted(true);
        planRepository.save(planEntity);

        // Soft delete tất cả features
        deleteExistingFeatures(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getById(Long id) {
        PlanEntity planEntity = planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> NotFoundException.plan(id));

        List<PlanFeatureEntity> features = planFeatureRepository
                .findByPlanIdAndDeletedFalseOrderBySortOrderAsc(id);

        return planMapper.toResponse(planEntity, features);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlanResponse> getAll(Pageable pageable) {
        Page<PlanEntity> plans = planRepository.findByDeletedFalse(pageable);

        return plans.map(plan -> {
            List<PlanFeatureEntity> features = planFeatureRepository
                    .findByPlanIdAndDeletedFalseOrderBySortOrderAsc(plan.getId());
            return planMapper.toResponse(plan, features);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {
        List<PlanEntity> plans = planRepository.findByDeletedFalseAndIsActiveTrueOrderByMonthlyPriceAsc();

        return plans.stream()
                .map(plan -> {
                    List<PlanFeatureEntity> features = planFeatureRepository
                            .findByPlanIdAndDeletedFalseOrderBySortOrderAsc(plan.getId());
                    return planMapper.toResponse(plan, features);
                })
                .collect(Collectors.toList());
    }

    /**
     * Tạo danh sách features cho plan
     */
    private List<PlanFeatureEntity> createFeatures(Long planId, List<PlanFeatureRequest> featureRequests) {
        return featureRequests.stream()
                .map(featureRequest -> {
                    PlanFeatureEntity featureEntity = planFeatureMapper.toEntity(featureRequest, planId);
                    return planFeatureRepository.save(featureEntity);
                })
                .collect(Collectors.toList());
    }

    /**
     * Soft delete tất cả features của plan
     */
    private void deleteExistingFeatures(Long planId) {
        List<PlanFeatureEntity> existingFeatures = planFeatureRepository.findByPlanIdAndDeletedFalse(planId);
        existingFeatures.forEach(feature -> {
            feature.setDeleted(true);
            planFeatureRepository.save(feature);
        });
    }
}
