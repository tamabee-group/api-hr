package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.wallet.PlanFeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho quản lý tính năng của gói dịch vụ (Plan Feature)
 */
@Repository
public interface PlanFeatureRepository extends JpaRepository<PlanFeatureEntity, Long> {

    /**
     * Tìm feature theo id và chưa bị xóa
     */
    Optional<PlanFeatureEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy danh sách features của plan, sắp xếp theo sortOrder
     */
    List<PlanFeatureEntity> findByPlanIdAndDeletedFalseOrderBySortOrderAsc(Long planId);

    /**
     * Kiểm tra feature có tồn tại và chưa bị xóa
     */
    boolean existsByIdAndDeletedFalse(Long id);

    /**
     * Xóa tất cả features của plan (soft delete sẽ được xử lý ở service layer)
     */
    List<PlanFeatureEntity> findByPlanIdAndDeletedFalse(Long planId);
}
