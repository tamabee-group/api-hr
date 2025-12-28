package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.wallet.WalletEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository cho quản lý ví tiền của công ty
 */
@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

        Optional<WalletEntity> findByCompanyId(Long companyId);

        boolean existsByCompanyId(Long companyId);

        /**
         * Lấy danh sách tất cả wallets (phân trang)
         */
        @Query("SELECT w FROM WalletEntity w ORDER BY w.createdAt DESC")
        Page<WalletEntity> findAllWallets(Pageable pageable);

        /**
         * Lấy danh sách wallets với filter theo balance (phân trang)
         */
        @Query("SELECT w FROM WalletEntity w WHERE w.balance >= :minBalance AND w.balance <= :maxBalance ORDER BY w.createdAt DESC")
        Page<WalletEntity> findByBalanceBetween(
                        @Param("minBalance") BigDecimal minBalance,
                        @Param("maxBalance") BigDecimal maxBalance,
                        Pageable pageable);

        /**
         * Đếm tổng số wallets
         */
        @Query("SELECT COUNT(w) FROM WalletEntity w")
        long countAllWallets();

        /**
         * Tính tổng số dư tất cả wallets
         */
        @Query("SELECT COALESCE(SUM(w.balance), 0) FROM WalletEntity w")
        BigDecimal sumAllBalances();

        /**
         * Đếm số company có số dư thấp (< monthlyPrice của plan)
         * Company có số dư < giá plan hiện tại được coi là low balance
         */
        @Query("SELECT COUNT(w) FROM WalletEntity w JOIN CompanyEntity c ON w.companyId = c.id " +
                        "JOIN PlanEntity p ON c.planId = p.id " +
                        "WHERE c.deleted = false AND p.deleted = false AND w.balance < p.monthlyPrice")
        long countCompaniesWithLowBalance();

        /**
         * Đếm số company đang trong thời gian miễn phí
         */
        @Query("SELECT COUNT(w) FROM WalletEntity w WHERE w.freeTrialEndDate IS NOT NULL AND w.freeTrialEndDate > :now")
        long countCompaniesInFreeTrial(@Param("now") LocalDateTime now);

        /**
         * Lấy danh sách wallets cần billing
         * Điều kiện: nextBillingDate <= now VÀ (freeTrialEndDate IS NULL HOẶC
         * freeTrialEndDate <= now)
         */
        @Query("SELECT w FROM WalletEntity w WHERE w.nextBillingDate <= :now " +
                        "AND (w.freeTrialEndDate IS NULL OR w.freeTrialEndDate <= :now)")
        java.util.List<WalletEntity> findWalletsDueForBilling(@Param("now") LocalDateTime now);
}
