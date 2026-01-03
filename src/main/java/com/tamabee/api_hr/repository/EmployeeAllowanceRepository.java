package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.payroll.EmployeeAllowanceEntity;
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
public interface EmployeeAllowanceRepository extends JpaRepository<EmployeeAllowanceEntity, Long> {

    /**
     * Tìm employee allowance theo ID và chưa bị xóa
     */
    Optional<EmployeeAllowanceEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy danh sách allowances đang active của nhân viên
     */
    @Query("SELECT ea FROM EmployeeAllowanceEntity ea " +
            "WHERE ea.deleted = false " +
            "AND ea.employeeId = :employeeId " +
            "AND ea.isActive = true " +
            "AND ea.effectiveFrom <= :currentDate " +
            "AND (ea.effectiveTo IS NULL OR ea.effectiveTo >= :currentDate)")
    List<EmployeeAllowanceEntity> findActiveByEmployeeId(
            @Param("employeeId") Long employeeId,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Lấy tất cả allowances của nhân viên (bao gồm cả inactive)
     */
    List<EmployeeAllowanceEntity> findByEmployeeIdAndDeletedFalse(Long employeeId);

    /**
     * Lấy allowances của nhân viên theo trạng thái active
     */
    List<EmployeeAllowanceEntity> findByEmployeeIdAndIsActiveAndDeletedFalse(
            Long employeeId, Boolean isActive);

    /**
     * Lấy allowances của công ty (phân trang)
     */
    Page<EmployeeAllowanceEntity> findByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

    /**
     * Lấy allowances trong khoảng thời gian hiệu lực
     * (dùng cho tính lương trong kỳ)
     */
    @Query("SELECT ea FROM EmployeeAllowanceEntity ea " +
            "WHERE ea.deleted = false " +
            "AND ea.employeeId = :employeeId " +
            "AND ea.isActive = true " +
            "AND ea.effectiveFrom <= :endDate " +
            "AND (ea.effectiveTo IS NULL OR ea.effectiveTo >= :startDate)")
    List<EmployeeAllowanceEntity> findByEffectiveDateRange(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Lấy allowances của nhiều nhân viên trong khoảng thời gian
     * (dùng cho tính lương hàng loạt)
     */
    @Query("SELECT ea FROM EmployeeAllowanceEntity ea " +
            "WHERE ea.deleted = false " +
            "AND ea.employeeId IN :employeeIds " +
            "AND ea.isActive = true " +
            "AND ea.effectiveFrom <= :endDate " +
            "AND (ea.effectiveTo IS NULL OR ea.effectiveTo >= :startDate)")
    List<EmployeeAllowanceEntity> findByEmployeeIdsAndEffectiveDateRange(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Kiểm tra nhân viên có allowance code cụ thể không
     */
    boolean existsByEmployeeIdAndAllowanceCodeAndIsActiveTrueAndDeletedFalse(
            Long employeeId, String allowanceCode);

    /**
     * Lấy allowance theo employee và code
     */
    Optional<EmployeeAllowanceEntity> findByEmployeeIdAndAllowanceCodeAndDeletedFalse(
            Long employeeId, String allowanceCode);
}
