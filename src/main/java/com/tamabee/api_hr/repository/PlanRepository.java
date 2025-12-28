package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.wallet.PlanEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho quản lý gói dịch vụ (Plan)
 */
@Repository
public interface PlanRepository extends JpaRepository<PlanEntity, Long> {

    /**
     * Tìm plan theo id và chưa bị xóa
     */
    Optional<PlanEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy danh sách tất cả plans chưa bị xóa (phân trang)
     */
    Page<PlanEntity> findByDeletedFalse(Pageable pageable);

    /**
     * Lấy danh sách plans đang active và chưa bị xóa
     */
    List<PlanEntity> findByDeletedFalseAndIsActiveTrueOrderByMonthlyPriceAsc();

    /**
     * Kiểm tra plan có tồn tại và chưa bị xóa
     */
    boolean existsByIdAndDeletedFalse(Long id);

    /**
     * Đếm số company đang sử dụng plan (để kiểm tra trước khi xóa)
     */
    @Query("SELECT COUNT(c) FROM CompanyEntity c WHERE c.planId = :planId AND c.deleted = false")
    long countCompaniesUsingPlan(@Param("planId") Long planId);
}
