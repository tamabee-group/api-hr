package com.tamabee.api_hr.mapper.admin;

import com.tamabee.api_hr.dto.request.wallet.PlanCreateRequest;
import com.tamabee.api_hr.dto.request.wallet.PlanUpdateRequest;
import com.tamabee.api_hr.dto.response.wallet.PlanFeatureResponse;
import com.tamabee.api_hr.dto.response.wallet.PlanResponse;
import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.entity.wallet.PlanFeatureEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper cho Plan entity
 * Chuyển đổi giữa Entity, Request DTO và Response DTO
 */
@Component
@RequiredArgsConstructor
public class PlanMapper {

    private final PlanFeatureMapper planFeatureMapper;

    /**
     * Chuyển đổi PlanCreateRequest sang PlanEntity
     * Dùng khi tạo mới plan
     */
    public PlanEntity toEntity(PlanCreateRequest request) {
        if (request == null) {
            return null;
        }

        PlanEntity entity = new PlanEntity();
        entity.setNameVi(request.getNameVi());
        entity.setNameEn(request.getNameEn());
        entity.setNameJa(request.getNameJa());
        entity.setDescriptionVi(request.getDescriptionVi());
        entity.setDescriptionEn(request.getDescriptionEn());
        entity.setDescriptionJa(request.getDescriptionJa());
        entity.setMonthlyPrice(request.getMonthlyPrice());
        entity.setMaxEmployees(request.getMaxEmployees());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        entity.setDeleted(false);

        return entity;
    }

    /**
     * Cập nhật PlanEntity từ PlanUpdateRequest
     * Chỉ cập nhật các trường được gửi (không null)
     */
    public void updateEntity(PlanEntity entity, PlanUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getNameVi() != null) {
            entity.setNameVi(request.getNameVi());
        }
        if (request.getNameEn() != null) {
            entity.setNameEn(request.getNameEn());
        }
        if (request.getNameJa() != null) {
            entity.setNameJa(request.getNameJa());
        }
        if (request.getDescriptionVi() != null) {
            entity.setDescriptionVi(request.getDescriptionVi());
        }
        if (request.getDescriptionEn() != null) {
            entity.setDescriptionEn(request.getDescriptionEn());
        }
        if (request.getDescriptionJa() != null) {
            entity.setDescriptionJa(request.getDescriptionJa());
        }
        if (request.getMonthlyPrice() != null) {
            entity.setMonthlyPrice(request.getMonthlyPrice());
        }
        if (request.getMaxEmployees() != null) {
            entity.setMaxEmployees(request.getMaxEmployees());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }

    /**
     * Chuyển đổi PlanEntity sang PlanResponse
     * Không bao gồm features (cần gọi riêng)
     */
    public PlanResponse toResponse(PlanEntity entity) {
        if (entity == null) {
            return null;
        }

        PlanResponse response = new PlanResponse();
        response.setId(entity.getId());
        response.setNameVi(entity.getNameVi());
        response.setNameEn(entity.getNameEn());
        response.setNameJa(entity.getNameJa());
        response.setDescriptionVi(entity.getDescriptionVi());
        response.setDescriptionEn(entity.getDescriptionEn());
        response.setDescriptionJa(entity.getDescriptionJa());
        response.setMonthlyPrice(entity.getMonthlyPrice());
        response.setMaxEmployees(entity.getMaxEmployees());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }

    /**
     * Chuyển đổi PlanEntity sang PlanResponse với danh sách features
     */
    public PlanResponse toResponse(PlanEntity entity, List<PlanFeatureEntity> features) {
        if (entity == null) {
            return null;
        }

        PlanResponse response = toResponse(entity);

        if (features != null && !features.isEmpty()) {
            List<PlanFeatureResponse> featureResponses = features.stream()
                    .map(planFeatureMapper::toResponse)
                    .collect(Collectors.toList());
            response.setFeatures(featureResponses);
        }

        return response;
    }
}
