package com.tamabee.api_hr.repository.payroll;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.payroll.EmployeeSalaryItemEntity;
import com.tamabee.api_hr.enums.SalaryItemType;

/**
 * Repository quản lý phụ cấp/khấu trừ của nhân viên.
 */
@Repository
public interface EmployeeSalaryItemRepository extends JpaRepository<EmployeeSalaryItemEntity, Long> {

    /**
     * Lấy tất cả salary items của nhân viên
     */
    List<EmployeeSalaryItemEntity> findByEmployeeIdAndDeletedFalse(Long employeeId);

    /**
     * Lấy salary items của nhân viên theo loại template
     */
    @Query("SELECT e FROM EmployeeSalaryItemEntity e " +
           "JOIN e.template t " +
           "WHERE e.deleted = false AND e.employeeId = :employeeId AND t.type = :type")
    List<EmployeeSalaryItemEntity> findByEmployeeIdAndTemplateTypeAndDeletedFalse(
            @Param("employeeId") Long employeeId,
            @Param("type") SalaryItemType type);

    /**
     * Kiểm tra template có đang được sử dụng không
     */
    boolean existsByTemplateIdAndDeletedFalse(Long templateId);

    /**
     * Đếm số nhân viên đang sử dụng template
     */
    @Query("SELECT COUNT(DISTINCT e.employeeId) FROM EmployeeSalaryItemEntity e " +
           "WHERE e.deleted = false AND e.templateId = :templateId")
    long countEmployeesByTemplateId(@Param("templateId") Long templateId);

    /**
     * Lấy tất cả salary items theo template ID
     */
    List<EmployeeSalaryItemEntity> findByTemplateIdAndDeletedFalse(Long templateId);

    /**
     * Tìm salary item theo ID và chưa bị xóa
     */
    Optional<EmployeeSalaryItemEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy salary items của nhiều nhân viên (cho payroll calculation)
     */
    @Query("SELECT e FROM EmployeeSalaryItemEntity e " +
           "WHERE e.deleted = false AND e.employeeId IN :employeeIds")
    List<EmployeeSalaryItemEntity> findByEmployeeIdInAndDeletedFalse(
            @Param("employeeIds") List<Long> employeeIds);
}
