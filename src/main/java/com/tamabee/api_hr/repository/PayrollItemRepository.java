package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.payroll.PayrollItemEntity;
import com.tamabee.api_hr.enums.PayrollItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItemEntity, Long> {

        /**
         * Lấy tất cả payroll items của một period
         */
        List<PayrollItemEntity> findByPayrollPeriodId(Long payrollPeriodId);

        /**
         * Lấy payroll items của một period (phân trang)
         */
        Page<PayrollItemEntity> findByPayrollPeriodId(Long payrollPeriodId, Pageable pageable);

        /**
         * Lấy payroll item của nhân viên trong period cụ thể
         */
        Optional<PayrollItemEntity> findByPayrollPeriodIdAndEmployeeId(
                        Long payrollPeriodId, Long employeeId);

        /**
         * Lấy danh sách payroll items của nhân viên
         */
        List<PayrollItemEntity> findByEmployeeId(Long employeeId);

        /**
         * Lấy danh sách payroll items của nhân viên (phân trang)
         */
        Page<PayrollItemEntity> findByEmployeeId(Long employeeId, Pageable pageable);

        /**
         * Lấy payroll items theo status
         */
        List<PayrollItemEntity> findByPayrollPeriodIdAndStatus(
                        Long payrollPeriodId, PayrollItemStatus status);

        /**
         * Kiểm tra nhân viên đã có payroll item trong period chưa
         */
        boolean existsByPayrollPeriodIdAndEmployeeId(
                        Long payrollPeriodId, Long employeeId);

        /**
         * Đếm số payroll items trong period
         */
        long countByPayrollPeriodId(Long payrollPeriodId);

        /**
         * Tính tổng gross salary của period
         */
        @Query("SELECT COALESCE(SUM(pi.grossSalary), 0) FROM PayrollItemEntity pi " +
                        "WHERE pi.payrollPeriodId = :payrollPeriodId")
        BigDecimal sumGrossSalaryByPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);

        /**
         * Tính tổng net salary của period
         */
        @Query("SELECT COALESCE(SUM(pi.netSalary), 0) FROM PayrollItemEntity pi " +
                        "WHERE pi.payrollPeriodId = :payrollPeriodId")
        BigDecimal sumNetSalaryByPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);

        /**
         * Lấy payroll items có adjustment
         */
        @Query("SELECT pi FROM PayrollItemEntity pi " +
                        "WHERE pi.payrollPeriodId = :payrollPeriodId " +
                        "AND pi.adjustmentAmount IS NOT NULL " +
                        "AND pi.adjustmentAmount != 0")
        List<PayrollItemEntity> findAdjustedItemsByPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);

        /**
         * Xóa tất cả payroll items của một period (dùng khi recalculate)
         */
        @Query("DELETE FROM PayrollItemEntity pi WHERE pi.payrollPeriodId = :payrollPeriodId")
        void deleteByPayrollPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);
}
