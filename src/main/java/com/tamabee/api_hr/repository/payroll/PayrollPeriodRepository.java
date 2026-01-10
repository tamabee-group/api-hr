package com.tamabee.api_hr.repository.payroll;

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

/**
 * Repository quản lý kỳ lương.
 */
@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriodEntity, Long> {

        /**
         * Lấy payroll period theo năm và tháng
         */
        Optional<PayrollPeriodEntity> findByYearAndMonth(Integer year, Integer month);

        /**
         * Lấy danh sách payroll periods (phân trang)
         */
        @Query("SELECT pp FROM PayrollPeriodEntity pp ORDER BY pp.periodStart DESC")
        Page<PayrollPeriodEntity> findAllPaged(Pageable pageable);

        /**
         * Lấy danh sách payroll periods theo status
         */
        Page<PayrollPeriodEntity> findByStatus(PayrollPeriodStatus status, Pageable pageable);

        /**
         * Lấy danh sách payroll periods theo năm
         */
        List<PayrollPeriodEntity> findByYear(Integer year);

        /**
         * Lấy danh sách payroll periods trong khoảng thời gian
         */
        @Query("SELECT pp FROM PayrollPeriodEntity pp " +
                        "WHERE pp.periodStart <= :endDate " +
                        "AND pp.periodEnd >= :startDate " +
                        "ORDER BY pp.periodStart DESC")
        List<PayrollPeriodEntity> findByDateRange(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Kiểm tra có payroll period nào overlap với khoảng thời gian không
         */
        @Query("SELECT COUNT(pp) > 0 FROM PayrollPeriodEntity pp " +
                        "WHERE pp.id != :excludeId " +
                        "AND pp.periodStart <= :endDate " +
                        "AND pp.periodEnd >= :startDate")
        boolean existsOverlappingPeriod(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("excludeId") Long excludeId);

        /**
         * Kiểm tra đã có payroll period cho năm/tháng chưa
         */
        boolean existsByYearAndMonth(Integer year, Integer month);

        /**
         * Lấy payroll period gần nhất
         */
        @Query("SELECT pp FROM PayrollPeriodEntity pp " +
                        "ORDER BY pp.periodEnd DESC " +
                        "LIMIT 1")
        Optional<PayrollPeriodEntity> findLatest();

        /**
         * Đếm số payroll periods theo status
         */
        long countByStatus(PayrollPeriodStatus status);

        /**
         * Lấy danh sách payroll periods đang DRAFT hoặc REVIEWING
         */
        @Query("SELECT pp FROM PayrollPeriodEntity pp " +
                        "WHERE pp.status IN ('DRAFT', 'REVIEWING') " +
                        "ORDER BY pp.periodStart DESC")
        List<PayrollPeriodEntity> findPendingPeriods();
}
