package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.payroll.EmployeeSalaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository quản lý thông tin lương của nhân viên.
 */
@Repository
public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalaryEntity, Long> {

    /**
     * Tìm thông tin lương hiện tại của nhân viên (có hiệu lực tại ngày chỉ định)
     */
    @Query("SELECT s FROM EmployeeSalaryEntity s WHERE s.deleted = false " +
            "AND s.employeeId = :employeeId " +
            "AND s.effectiveFrom <= :date " +
            "AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date) " +
            "ORDER BY s.effectiveFrom DESC")
    Optional<EmployeeSalaryEntity> findEffectiveSalary(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date);

    /**
     * Tìm thông tin lương mới nhất của nhân viên
     */
    @Query("SELECT s FROM EmployeeSalaryEntity s WHERE s.deleted = false " +
            "AND s.employeeId = :employeeId " +
            "ORDER BY s.effectiveFrom DESC " +
            "LIMIT 1")
    Optional<EmployeeSalaryEntity> findLatestSalary(@Param("employeeId") Long employeeId);

    /**
     * Kiểm tra nhân viên có thông tin lương không
     */
    boolean existsByEmployeeIdAndDeletedFalse(Long employeeId);
}
