package com.tamabee.api_hr.repository.wallet;

import com.tamabee.api_hr.entity.wallet.PlanFeatureCodeEntity;
import com.tamabee.api_hr.enums.FeatureCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho quản lý mapping giữa Plan và FeatureCode
 */
@Repository
public interface PlanFeatureCodeRepository extends JpaRepository<PlanFeatureCodeEntity, Long> {

    /**
     * Lấy danh sách feature codes của plan
     */
    List<PlanFeatureCodeEntity> findByPlanIdAndDeletedFalse(Long planId);

    /**
     * Kiểm tra plan có feature code không
     */
    boolean existsByPlanIdAndFeatureCodeAndDeletedFalse(Long planId, FeatureCode featureCode);

    /**
     * Xóa tất cả feature codes của plan (dùng khi cập nhật plan)
     */
    void deleteByPlanId(Long planId);
}
