package com.tamabee.api_hr.mapper.admin;

import com.tamabee.api_hr.dto.request.wallet.PlanFeatureRequest;
import com.tamabee.api_hr.dto.response.wallet.PlanFeatureResponse;
import com.tamabee.api_hr.entity.wallet.PlanFeatureEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper cho PlanFeature entity
 * Chuyển đổi giữa Entity, Request DTO và Response DTO
 */
@Component
public class PlanFeatureMapper {

    /**
     * Chuyển đổi PlanFeatureRequest sang PlanFeatureEntity
     * Dùng khi tạo mới feature
     */
    public PlanFeatureEntity toEntity(PlanFeatureRequest request, Long planId) {
        if (request == null) {
            return null;
        }

        PlanFeatureEntity entity = new PlanFeatureEntity();
        entity.setPlanId(planId);
        entity.setFeatureVi(request.getFeatureVi());
        entity.setFeatureEn(request.getFeatureEn());
        entity.setFeatureJa(request.getFeatureJa());
        entity.setSortOrder(request.getSortOrder());
        entity.setIsHighlighted(request.getIsHighlighted() != null ? request.getIsHighlighted() : false);
        entity.setDeleted(false);

        return entity;
    }

    /**
     * Cập nhật PlanFeatureEntity từ PlanFeatureRequest
     * Chỉ cập nhật các trường được gửi
     */
    public void updateEntity(PlanFeatureEntity entity, PlanFeatureRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getFeatureVi() != null) {
            entity.setFeatureVi(request.getFeatureVi());
        }
        if (request.getFeatureEn() != null) {
            entity.setFeatureEn(request.getFeatureEn());
        }
        if (request.getFeatureJa() != null) {
            entity.setFeatureJa(request.getFeatureJa());
        }
        if (request.getSortOrder() != null) {
            entity.setSortOrder(request.getSortOrder());
        }
        if (request.getIsHighlighted() != null) {
            entity.setIsHighlighted(request.getIsHighlighted());
        }
    }

    /**
     * Chuyển đổi PlanFeatureEntity sang PlanFeatureResponse
     */
    public PlanFeatureResponse toResponse(PlanFeatureEntity entity) {
        if (entity == null) {
            return null;
        }

        PlanFeatureResponse response = new PlanFeatureResponse();
        response.setId(entity.getId());
        response.setFeatureVi(entity.getFeatureVi());
        response.setFeatureEn(entity.getFeatureEn());
        response.setFeatureJa(entity.getFeatureJa());
        response.setSortOrder(entity.getSortOrder());
        response.setIsHighlighted(entity.getIsHighlighted());

        return response;
    }
}
