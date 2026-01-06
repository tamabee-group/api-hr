package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.payroll.PayrollRecordEntity;
import com.tamabee.api_hr.enums.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý bản ghi lương của nhân viên.
 */
@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecordEntity, Long> {

        /**
         * Lấy danh sách bản ghi lương của công ty theo kỳ (phân trang)
         */
        @Query("SELECT p FROM PayrollRecordEntity p " +
                        "WHERE p.companyId = :companyId " +
                        "AND p.year = :year AND p.month = :month " +
                        "ORDER BY p.employeeId ASC")
        Page<PayrollRecordEntity> findByCompanyIdAndYearAndMonth(
                        @Param("companyId") Long companyId,
                        @Param("year") Integer year,
                        @Param("month") Integer month,
                        Pageable pageable);

        /**
         * Lấy tất cả bản ghi lương của công ty theo kỳ
         */
        @Query("SELECT p FROM PayrollRecordEntity p " +
                        "WHERE p.companyId = :companyId " +
                        "AND p.year = :year AND p.month = :month " +
                        "ORDER BY p.employeeId ASC")
        List<PayrollRecordEntity> findAllByCompanyIdAndYearAndMonth(
                        @Param("companyId") Long companyId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Tìm bản ghi lương của nhân viên theo kỳ
         */
        Optional<PayrollRecordEntity> findByEmployeeIdAndYearAndMonth(
                        Long employeeId, Integer year, Integer month);

        /**
         * Lấy lịch sử lương của nhân viên (phân trang, sắp xếp theo năm/tháng giảm dần)
         */
        @Query("SELECT p FROM PayrollRecordEntity p " +
                        "WHERE p.employeeId = :employeeId " +
                        "ORDER BY p.year DESC, p.month DESC")
        Page<PayrollRecordEntity> findByEmployeeIdOrderByYearDescMonthDesc(
                        @Param("employeeId") Long employeeId,
                        Pageable pageable);

        /**
         * Lấy danh sách bản ghi lương theo trạng thái
         */
        @Query("SELECT p FROM PayrollRecordEntity p " +
                        "WHERE p.companyId = :companyId " +
                        "AND p.year = :year AND p.month = :month " +
                        "AND p.status = :status")
        List<PayrollRecordEntity> findByCompanyIdAndYearAndMonthAndStatus(
                        @Param("companyId") Long companyId,
                        @Param("year") Integer year,
                        @Param("month") Integer month,
                        @Param("status") PayrollStatus status);

        /**
         * Đếm số bản ghi lương của công ty theo kỳ
         */
        @Query("SELECT COUNT(p) FROM PayrollRecordEntity p " +
                        "WHERE p.companyId = :companyId " +
                        "AND p.year = :year AND p.month = :month")
        long countByCompanyIdAndYearAndMonth(
                        @Param("companyId") Long companyId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Kiểm tra bản ghi lương đã tồn tại cho nhân viên trong kỳ chưa
         */
        boolean existsByEmployeeIdAndYearAndMonth(
                        Long employeeId, Integer year, Integer month);

        /**
         * Lấy danh sách bản ghi lương chưa gửi thông báo
         */
        @Query("SELECT p FROM PayrollRecordEntity p " +
                        "WHERE p.companyId = :companyId " +
                        "AND p.year = :year AND p.month = :month " +
                        "AND p.status = 'FINALIZED' " +
                        "AND p.notificationSent = false")
        List<PayrollRecordEntity> findPendingNotifications(
                        @Param("companyId") Long companyId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);
}
