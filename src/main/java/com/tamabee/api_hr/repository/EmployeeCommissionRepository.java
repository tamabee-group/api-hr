package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.wallet.EmployeeCommissionEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
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
 * Repository cho quản lý hoa hồng giới thiệu của nhân viên Tamabee
 */
@Repository
public interface EmployeeCommissionRepository extends JpaRepository<EmployeeCommissionEntity, Long> {

        /**
         * Tìm commission theo id và chưa bị xóa
         */
        Optional<EmployeeCommissionEntity> findByIdAndDeletedFalse(Long id);

        /**
         * Lấy tất cả commissions chưa bị xóa (phân trang)
         */
        Page<EmployeeCommissionEntity> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

        /**
         * Lấy commissions theo employeeCode (phân trang)
         */
        Page<EmployeeCommissionEntity> findByDeletedFalseAndEmployeeCodeOrderByCreatedAtDesc(
                        String employeeCode, Pageable pageable);

        /**
         * Lấy commissions theo status (phân trang)
         */
        Page<EmployeeCommissionEntity> findByDeletedFalseAndStatusOrderByCreatedAtDesc(
                        CommissionStatus status, Pageable pageable);

        /**
         * Lấy commissions theo employeeCode và status (phân trang)
         */
        Page<EmployeeCommissionEntity> findByDeletedFalseAndEmployeeCodeAndStatusOrderByCreatedAtDesc(
                        String employeeCode, CommissionStatus status, Pageable pageable);

        /**
         * Lấy commissions theo khoảng thời gian (phân trang)
         */
        @Query("SELECT c FROM EmployeeCommissionEntity c WHERE c.deleted = false " +
                        "AND c.createdAt >= :startDate AND c.createdAt <= :endDate ORDER BY c.createdAt DESC")
        Page<EmployeeCommissionEntity> findByDateRange(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Lấy commissions theo employeeCode và khoảng thời gian (phân trang)
         */
        @Query("SELECT c FROM EmployeeCommissionEntity c WHERE c.deleted = false AND c.employeeCode = :employeeCode " +
                        "AND c.createdAt >= :startDate AND c.createdAt <= :endDate ORDER BY c.createdAt DESC")
        Page<EmployeeCommissionEntity> findByEmployeeCodeAndDateRange(
                        @Param("employeeCode") String employeeCode,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Lấy commissions theo status và khoảng thời gian (phân trang)
         */
        @Query("SELECT c FROM EmployeeCommissionEntity c WHERE c.deleted = false AND c.status = :status " +
                        "AND c.createdAt >= :startDate AND c.createdAt <= :endDate ORDER BY c.createdAt DESC")
        Page<EmployeeCommissionEntity> findByStatusAndDateRange(
                        @Param("status") CommissionStatus status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Lấy commissions theo employeeCode, status và khoảng thời gian (phân trang)
         */
        @Query("SELECT c FROM EmployeeCommissionEntity c WHERE c.deleted = false AND c.employeeCode = :employeeCode " +
                        "AND c.status = :status AND c.createdAt >= :startDate AND c.createdAt <= :endDate " +
                        "ORDER BY c.createdAt DESC")
        Page<EmployeeCommissionEntity> findByEmployeeCodeAndStatusAndDateRange(
                        @Param("employeeCode") String employeeCode,
                        @Param("status") CommissionStatus status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Kiểm tra company đã có commission chưa (để đảm bảo chỉ tính hoa hồng lần đầu)
         */
        boolean existsByCompanyIdAndDeletedFalse(Long companyId);

        /**
         * Tính tổng hoa hồng theo employeeCode
         */
        @Query("SELECT COALESCE(SUM(c.amount), 0) FROM EmployeeCommissionEntity c " +
                        "WHERE c.deleted = false AND c.employeeCode = :employeeCode")
        BigDecimal sumAmountByEmployeeCode(@Param("employeeCode") String employeeCode);

        /**
         * Tính tổng hoa hồng theo employeeCode và status
         */
        @Query("SELECT COALESCE(SUM(c.amount), 0) FROM EmployeeCommissionEntity c " +
                        "WHERE c.deleted = false AND c.employeeCode = :employeeCode AND c.status = :status")
        BigDecimal sumAmountByEmployeeCodeAndStatus(
                        @Param("employeeCode") String employeeCode,
                        @Param("status") CommissionStatus status);

        /**
         * Tính tổng hoa hồng theo employeeCode và tháng
         */
        @Query("SELECT COALESCE(SUM(c.amount), 0) FROM EmployeeCommissionEntity c " +
                        "WHERE c.deleted = false AND c.employeeCode = :employeeCode " +
                        "AND c.createdAt >= :startOfMonth AND c.createdAt < :endOfMonth")
        BigDecimal sumAmountByEmployeeCodeAndMonth(
                        @Param("employeeCode") String employeeCode,
                        @Param("startOfMonth") LocalDateTime startOfMonth,
                        @Param("endOfMonth") LocalDateTime endOfMonth);

        /**
         * Đếm số commission theo employeeCode
         */
        long countByEmployeeCodeAndDeletedFalse(String employeeCode);

        /**
         * Đếm số commission theo employeeCode và status
         */
        long countByEmployeeCodeAndStatusAndDeletedFalse(String employeeCode, CommissionStatus status);

        // ==================== Overall Summary Methods ====================

        /**
         * Tính tổng hoa hồng toàn bộ hệ thống
         */
        @Query("SELECT COALESCE(SUM(c.amount), 0) FROM EmployeeCommissionEntity c WHERE c.deleted = false")
        BigDecimal sumTotalAmount();

        /**
         * Tính tổng hoa hồng theo status
         */
        @Query("SELECT COALESCE(SUM(c.amount), 0) FROM EmployeeCommissionEntity c " +
                        "WHERE c.deleted = false AND c.status = :status")
        BigDecimal sumAmountByStatus(@Param("status") CommissionStatus status);

        /**
         * Đếm số commission theo status
         */
        long countByStatusAndDeletedFalse(CommissionStatus status);

        /**
         * Tính tổng hoa hồng theo khoảng thời gian và status
         */
        @Query("SELECT COALESCE(SUM(c.amount), 0) FROM EmployeeCommissionEntity c " +
                        "WHERE c.deleted = false AND c.status = :status " +
                        "AND c.createdAt >= :startOfMonth AND c.createdAt < :endOfMonth")
        BigDecimal sumAmountByMonthRangeAndStatus(
                        @Param("startOfMonth") LocalDateTime startOfMonth,
                        @Param("endOfMonth") LocalDateTime endOfMonth,
                        @Param("status") CommissionStatus status);

        /**
         * Đếm số commission theo khoảng thời gian
         */
        @Query("SELECT COUNT(c) FROM EmployeeCommissionEntity c " +
                        "WHERE c.deleted = false " +
                        "AND c.createdAt >= :startOfMonth AND c.createdAt < :endOfMonth")
        long countByMonthRange(
                        @Param("startOfMonth") LocalDateTime startOfMonth,
                        @Param("endOfMonth") LocalDateTime endOfMonth);

        /**
         * Lấy danh sách các tháng có commission (distinct) - native query cho
         * PostgreSQL
         */
        @Query(value = "SELECT DISTINCT DATE_TRUNC('month', created_at) as month FROM employee_commissions " +
                        "WHERE deleted = false ORDER BY month DESC", nativeQuery = true)
        java.util.List<java.sql.Timestamp> findDistinctMonthsNative();

        /**
         * Lấy danh sách các employeeCode có commission (distinct)
         */
        @Query("SELECT DISTINCT c.employeeCode FROM EmployeeCommissionEntity c WHERE c.deleted = false ORDER BY c.employeeCode")
        java.util.List<String> findDistinctEmployeeCodes();

        // ==================== Eligibility Methods ====================

        /**
         * Lấy tất cả commissions PENDING của một company (để recalculate eligibility)
         */
        @Query("SELECT c FROM EmployeeCommissionEntity c WHERE c.deleted = false " +
                        "AND c.companyId = :companyId AND c.status = 'PENDING'")
        java.util.List<EmployeeCommissionEntity> findPendingByCompanyId(@Param("companyId") Long companyId);

        /**
         * Lấy tất cả commissions với eligibility status (phân trang)
         */
        @Query("SELECT c FROM EmployeeCommissionEntity c WHERE c.deleted = false ORDER BY c.createdAt DESC")
        Page<EmployeeCommissionEntity> findAllWithEligibility(Pageable pageable);

        // ==================== Employee Referral Methods ====================

        /**
         * Lấy commission theo companyId (để hiển thị trong referred company response)
         */
        Optional<EmployeeCommissionEntity> findByCompanyIdAndDeletedFalse(Long companyId);
}
