package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.payroll.EmployeeDeductionEntity;
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
public interface EmployeeDeductionRepository extends JpaRepository<EmployeeDeductionEntity, Long> {

    /**
     * Tìm employee deduction theo ID và chưa bị xóa
     */
    Optional<EmployeeDeductionEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy danh sách deductions đang active của nhân viên
     */
    @Query("SELECT ed FROM EmployeeDeductionEntity ed " +
            "WHERE ed.deleted = false " +
            "AND ed.employeeId = :employeeId " +
            "AND ed.isActive = true " +
            "AND ed.effectiveFrom <= :currentDate " +
            "AND (ed.effectiveTo IS NULL OR ed.effectiveTo >= :currentDate)")
    List<EmployeeDeductionEntity> findActiveByEmployeeId(
            @Param("employeeId") Long employeeId,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Lấy tất cả deductions của nhân viên (bao gồm cả inactive)
     */
    List<EmployeeDeductionEntity> findByEmployeeIdAndDeletedFalse(Long employeeId);

    /**
     * Lấy deductions của nhân viên theo trạng thái active
     */
    List<EmployeeDeductionEntity> findByEmployeeIdAndIsActiveAndDeletedFalse(
            Long employeeId, Boolean isActive);

    /**
     * Lấy deductions của công ty (phân trang)
     */
    Page<EmployeeDeductionEntity> findByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

    /**
     * Lấy deductions trong khoảng thời gian hiệu lực
     * (dùng cho tính lương trong kỳ)
     */
    @Query("SELECT ed FROM EmployeeDeductionEntity ed " +
            "WHERE ed.deleted = false " +
            "AND ed.employeeId = :employeeId " +
            "AND ed.isActive = true " +
            "AND ed.effectiveFrom <= :endDate " +
            "AND (ed.effectiveTo IS NULL OR ed.effectiveTo >= :startDate)")
    List<EmployeeDeductionEntity> findByEffectiveDateRange(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Lấy deductions của nhiều nhân viên trong khoảng thời gian
     * (dùng cho tính lương hàng loạt)
     */
    @Query("SELECT ed FROM EmployeeDeductionEntity ed " +
            "WHERE ed.deleted = false " +
            "AND ed.employeeId IN :employeeIds " +
            "AND ed.isActive = true " +
            "AND ed.effectiveFrom <= :endDate " +
            "AND (ed.effectiveTo IS NULL OR ed.effectiveTo >= :startDate)")
    List<EmployeeDeductionEntity> findByEmployeeIdsAndEffectiveDateRange(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Kiểm tra nhân viên có deduction code cụ thể không
     */
    boolean existsByEmployeeIdAndDeductionCodeAndIsActiveTrueAndDeletedFalse(
            Long employeeId, String deductionCode);

    /**
     * Lấy deduction theo employee và code
     */
    Optional<EmployeeDeductionEntity> findByEmployeeIdAndDeductionCodeAndDeletedFalse(
            Long employeeId, String deductionCode);
}
