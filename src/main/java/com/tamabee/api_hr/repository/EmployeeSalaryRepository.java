package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.payroll.EmployeeSalaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thông tin lương của nhân viên.
 */
@Repository
public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalaryEntity, Long> {

        /**
         * Tìm thông tin lương hiện tại của nhân viên (có hiệu lực tại ngày chỉ định)
         * Trả về danh sách để xử lý ở service layer (lấy phần tử đầu tiên)
         */
        @Query("SELECT s FROM EmployeeSalaryEntity s WHERE s.deleted = false " +
                        "AND s.employeeId = :employeeId " +
                        "AND s.effectiveFrom <= :date " +
                        "AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date) " +
                        "ORDER BY s.effectiveFrom DESC")
        List<EmployeeSalaryEntity> findEffectiveSalaries(
                        @Param("employeeId") Long employeeId,
                        @Param("date") LocalDate date);

        /**
         * Tìm thông tin lương mới nhất của nhân viên
         */
        @Query("SELECT s FROM EmployeeSalaryEntity s WHERE s.deleted = false " +
                        "AND s.employeeId = :employeeId " +
                        "ORDER BY s.effectiveFrom DESC")
        List<EmployeeSalaryEntity> findLatestSalaries(@Param("employeeId") Long employeeId);

        /**
         * Kiểm tra nhân viên có thông tin lương không
         */
        boolean existsByEmployeeIdAndDeletedFalse(Long employeeId);

        /**
         * Helper method để lấy salary hiệu lực (lấy phần tử đầu tiên từ list)
         */
        default Optional<EmployeeSalaryEntity> findEffectiveSalary(Long employeeId, LocalDate date) {
                List<EmployeeSalaryEntity> salaries = findEffectiveSalaries(employeeId, date);
                return salaries.isEmpty() ? Optional.empty() : Optional.of(salaries.get(0));
        }

        /**
         * Helper method để lấy salary mới nhất (lấy phần tử đầu tiên từ list)
         */
        default Optional<EmployeeSalaryEntity> findLatestSalary(Long employeeId) {
                List<EmployeeSalaryEntity> salaries = findLatestSalaries(employeeId);
                return salaries.isEmpty() ? Optional.empty() : Optional.of(salaries.get(0));
        }
}
