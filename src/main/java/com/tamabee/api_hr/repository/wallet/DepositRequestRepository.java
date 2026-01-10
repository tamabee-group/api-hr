package com.tamabee.api_hr.repository.wallet;

import com.tamabee.api_hr.entity.wallet.DepositRequestEntity;
import com.tamabee.api_hr.enums.DepositStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository cho quản lý yêu cầu nạp tiền
 */
@Repository
public interface DepositRequestRepository extends JpaRepository<DepositRequestEntity, Long> {

    /**
     * Tìm deposit request theo id và chưa bị xóa
     */
    Optional<DepositRequestEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy tất cả deposit requests chưa bị xóa (phân trang)
     */
    Page<DepositRequestEntity> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Lấy deposit requests theo status (phân trang)
     */
    Page<DepositRequestEntity> findByDeletedFalseAndStatusOrderByCreatedAtDesc(DepositStatus status, Pageable pageable);

    /**
     * Lấy deposit requests theo companyId (phân trang)
     */
    Page<DepositRequestEntity> findByDeletedFalseAndCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    /**
     * Lấy deposit requests theo companyId và status (phân trang)
     */
    Page<DepositRequestEntity> findByDeletedFalseAndCompanyIdAndStatusOrderByCreatedAtDesc(
            Long companyId, DepositStatus status, Pageable pageable);

    /**
     * Lấy deposit requests theo khoảng thời gian (phân trang)
     */
    @Query("SELECT d FROM DepositRequestEntity d WHERE d.deleted = false " +
            "AND d.createdAt >= :startDate AND d.createdAt <= :endDate ORDER BY d.createdAt DESC")
    Page<DepositRequestEntity> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Lấy deposit requests theo status và khoảng thời gian (phân trang)
     */
    @Query("SELECT d FROM DepositRequestEntity d WHERE d.deleted = false AND d.status = :status " +
            "AND d.createdAt >= :startDate AND d.createdAt <= :endDate ORDER BY d.createdAt DESC")
    Page<DepositRequestEntity> findByStatusAndDateRange(
            @Param("status") DepositStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Lấy deposit requests theo companyId và khoảng thời gian (phân trang)
     */
    @Query("SELECT d FROM DepositRequestEntity d WHERE d.deleted = false AND d.companyId = :companyId " +
            "AND d.createdAt >= :startDate AND d.createdAt <= :endDate ORDER BY d.createdAt DESC")
    Page<DepositRequestEntity> findByCompanyIdAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Lấy deposit requests theo companyId, status và khoảng thời gian (phân trang)
     */
    @Query("SELECT d FROM DepositRequestEntity d WHERE d.deleted = false AND d.companyId = :companyId " +
            "AND d.status = :status AND d.createdAt >= :startDate AND d.createdAt <= :endDate " +
            "ORDER BY d.createdAt DESC")
    Page<DepositRequestEntity> findByCompanyIdAndStatusAndDateRange(
            @Param("companyId") Long companyId,
            @Param("status") DepositStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Kiểm tra deposit request có tồn tại và chưa bị xóa
     */
    boolean existsByIdAndDeletedFalse(Long id);
}
