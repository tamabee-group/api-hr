package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.payroll.PayrollPeriodEntity;
import com.tamabee.api_hr.enums.PayrollPeriodStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriodEntity, Long> {

    /**
     * Tìm payroll period theo ID và chưa bị xóa
     */
    Optional<PayrollPeriodEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy payroll period theo công ty, năm và tháng
     */
    Optional<PayrollPeriodEntity> findByCompanyIdAndYearAndMonthAndDeletedFalse(
            Long companyId, Integer year, Integer month);

    /**
     * Lấy danh sách payroll periods của công ty (phân trang)
     */
    Page<PayrollPeriodEntity> findByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

    /**
     * Lấy danh sách payroll periods của công ty theo status
     */
    Page<PayrollPeriodEntity> findByCompanyIdAndStatusAndDeletedFalse(
            Long companyId, PayrollPeriodStatus status, Pageable pageable);

    /**
     * Lấy danh sách payroll periods của công ty theo năm
     */
    List<PayrollPeriodEntity> findByCompanyIdAndYearAndDeletedFalse(Long companyId, Integer year);

    /**
     * Lấy danh sách payroll periods trong khoảng thời gian
     */
    @Query("SELECT pp FROM PayrollPeriodEntity pp " +
            "WHERE pp.deleted = false " +
            "AND pp.companyId = :companyId " +
            "AND pp.periodStart <= :endDate " +
            "AND pp.periodEnd >= :startDate " +
            "ORDER BY pp.periodStart DESC")
    List<PayrollPeriodEntity> findByCompanyIdAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Kiểm tra có payroll period nào overlap với khoảng thời gian không
     */
    @Query("SELECT COUNT(pp) > 0 FROM PayrollPeriodEntity pp " +
            "WHERE pp.deleted = false " +
            "AND pp.companyId = :companyId " +
            "AND pp.id != :excludeId " +
            "AND pp.periodStart <= :endDate " +
            "AND pp.periodEnd >= :startDate")
    boolean existsOverlappingPeriod(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);

    /**
     * Kiểm tra công ty đã có payroll period cho năm/tháng chưa
     */
    boolean existsByCompanyIdAndYearAndMonthAndDeletedFalse(
            Long companyId, Integer year, Integer month);

    /**
     * Lấy payroll period gần nhất của công ty
     */
    @Query("SELECT pp FROM PayrollPeriodEntity pp " +
            "WHERE pp.deleted = false " +
            "AND pp.companyId = :companyId " +
            "ORDER BY pp.periodEnd DESC " +
            "LIMIT 1")
    Optional<PayrollPeriodEntity> findLatestByCompanyId(@Param("companyId") Long companyId);

    /**
     * Đếm số payroll periods theo status
     */
    long countByCompanyIdAndStatusAndDeletedFalse(Long companyId, PayrollPeriodStatus status);

    /**
     * Lấy danh sách payroll periods đang DRAFT hoặc REVIEWING
     */
    @Query("SELECT pp FROM PayrollPeriodEntity pp " +
            "WHERE pp.deleted = false " +
            "AND pp.companyId = :companyId " +
            "AND pp.status IN ('DRAFT', 'REVIEWING') " +
            "ORDER BY pp.periodStart DESC")
    List<PayrollPeriodEntity> findPendingPeriodsByCompanyId(@Param("companyId") Long companyId);
}
