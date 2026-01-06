package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.wallet.WalletTransactionEntity;
import com.tamabee.api_hr.enums.TransactionType;
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
 * Repository cho quản lý lịch sử giao dịch ví
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransactionEntity, Long> {

        /**
         * Lấy danh sách transactions theo walletId (phân trang, sắp xếp theo createdAt
         * DESC)
         */
        Page<WalletTransactionEntity> findByWalletIdOrderByCreatedAtDesc(Long walletId,
                        Pageable pageable);

        /**
         * Lấy danh sách transactions theo walletId và transactionType (phân trang)
         */
        Page<WalletTransactionEntity> findByWalletIdAndTransactionTypeOrderByCreatedAtDesc(
                        Long walletId, TransactionType transactionType, Pageable pageable);

        /**
         * Lấy danh sách transactions theo walletId và khoảng thời gian (phân trang)
         */
        @Query("SELECT t FROM WalletTransactionEntity t WHERE t.walletId = :walletId " +
                        "AND t.createdAt >= :startDate AND t.createdAt <= :endDate ORDER BY t.createdAt DESC")
        Page<WalletTransactionEntity> findByWalletIdAndDateRange(
                        @Param("walletId") Long walletId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Lấy danh sách transactions theo walletId, transactionType và khoảng thời gian
         * (phân trang)
         */
        @Query("SELECT t FROM WalletTransactionEntity t WHERE t.walletId = :walletId " +
                        "AND t.transactionType = :transactionType " +
                        "AND t.createdAt >= :startDate AND t.createdAt <= :endDate ORDER BY t.createdAt DESC")
        Page<WalletTransactionEntity> findByWalletIdAndTypeAndDateRange(
                        @Param("walletId") Long walletId,
                        @Param("transactionType") TransactionType transactionType,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Lấy danh sách transactions theo companyId thông qua wallet (phân trang)
         */
        @Query("SELECT t FROM WalletTransactionEntity t JOIN WalletEntity w ON t.walletId = w.id " +
                        "WHERE w.companyId = :companyId AND w.deleted = false " +
                        "ORDER BY t.createdAt DESC")
        Page<WalletTransactionEntity> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

        /**
         * Lấy danh sách transactions theo companyId và transactionType (phân trang)
         */
        @Query("SELECT t FROM WalletTransactionEntity t JOIN WalletEntity w ON t.walletId = w.id " +
                        "WHERE w.companyId = :companyId AND w.deleted = false " +
                        "AND t.transactionType = :transactionType ORDER BY t.createdAt DESC")
        Page<WalletTransactionEntity> findByCompanyIdAndType(
                        @Param("companyId") Long companyId,
                        @Param("transactionType") TransactionType transactionType,
                        Pageable pageable);

        /**
         * Lấy danh sách transactions theo companyId và khoảng thời gian (phân trang)
         */
        @Query("SELECT t FROM WalletTransactionEntity t JOIN WalletEntity w ON t.walletId = w.id " +
                        "WHERE w.companyId = :companyId AND w.deleted = false " +
                        "AND t.createdAt >= :startDate AND t.createdAt <= :endDate ORDER BY t.createdAt DESC")
        Page<WalletTransactionEntity> findByCompanyIdAndDateRange(
                        @Param("companyId") Long companyId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Lấy danh sách transactions theo companyId, transactionType và khoảng thời
         * gian (phân trang)
         */
        @Query("SELECT t FROM WalletTransactionEntity t JOIN WalletEntity w ON t.walletId = w.id " +
                        "WHERE w.companyId = :companyId AND w.deleted = false " +
                        "AND t.transactionType = :transactionType " +
                        "AND t.createdAt >= :startDate AND t.createdAt <= :endDate ORDER BY t.createdAt DESC")
        Page<WalletTransactionEntity> findByCompanyIdAndTypeAndDateRange(
                        @Param("companyId") Long companyId,
                        @Param("transactionType") TransactionType transactionType,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        // ==================== Statistics Queries ====================

        /**
         * Tính tổng số tiền đã nạp (DEPOSIT) của một wallet
         */
        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransactionEntity t " +
                        "WHERE t.walletId = :walletId AND t.transactionType = 'DEPOSIT'")
        BigDecimal sumDepositsByWalletId(@Param("walletId") Long walletId);

        /**
         * Tính tổng số tiền đã billing của một wallet
         */
        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransactionEntity t " +
                        "WHERE t.walletId = :walletId AND t.transactionType = 'BILLING'")
        BigDecimal sumBillingsByWalletId(@Param("walletId") Long walletId);

        /**
         * Tính tổng số tiền đã nạp (DEPOSIT) của tất cả wallets
         */
        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransactionEntity t " +
                        "WHERE t.transactionType = 'DEPOSIT'")
        BigDecimal sumAllDeposits();

        /**
         * Tính tổng số tiền đã billing của tất cả wallets
         */
        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransactionEntity t " +
                        "WHERE t.transactionType = 'BILLING'")
        BigDecimal sumAllBillings();

        /**
         * Tính tổng số tiền đã nạp (DEPOSIT) của một company
         */
        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransactionEntity t " +
                        "JOIN WalletEntity w ON t.walletId = w.id " +
                        "WHERE w.companyId = :companyId AND w.deleted = false AND t.transactionType = 'DEPOSIT'")
        BigDecimal sumDepositsByCompanyId(@Param("companyId") Long companyId);
}
