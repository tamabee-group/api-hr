package com.tamabee.api_hr.repository.wallet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.wallet.PlanChangeHistoryEntity;

/**
 * Repository quản lý lịch sử thay đổi plan
 */
@Repository
public interface PlanChangeHistoryRepository extends JpaRepository<PlanChangeHistoryEntity, Long> {

    /**
     * Lấy lịch sử thay đổi plan của company
     */
    List<PlanChangeHistoryEntity> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    /**
     * Lấy giá plan cao nhất trong kỳ billing
     * Dùng để tính tiền theo plan cao nhất đã sử dụng (chống gian lận)
     */
    @Query("SELECT MAX(p.toPlanPrice) FROM PlanChangeHistoryEntity p " +
           "WHERE p.companyId = :companyId " +
           "AND p.effectiveDate >= :periodStart " +
           "AND p.effectiveDate <= :periodEnd")
    BigDecimal findMaxPlanPriceInPeriod(
            @Param("companyId") Long companyId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    /**
     * Lấy tất cả thay đổi plan trong kỳ billing
     */
    @Query("SELECT p FROM PlanChangeHistoryEntity p " +
           "WHERE p.companyId = :companyId " +
           "AND p.effectiveDate >= :periodStart " +
           "AND p.effectiveDate <= :periodEnd " +
           "ORDER BY p.effectiveDate ASC")
    List<PlanChangeHistoryEntity> findByCompanyIdAndPeriod(
            @Param("companyId") Long companyId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);
}
