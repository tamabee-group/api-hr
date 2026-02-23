package com.tamabee.api_hr.repository.payroll;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.payroll.SalaryItemTemplateEntity;
import com.tamabee.api_hr.enums.SalaryItemType;

/**
 * Repository quản lý template phụ cấp/khấu trừ.
 */
@Repository
public interface SalaryItemTemplateRepository extends JpaRepository<SalaryItemTemplateEntity, Long> {

    /**
     * Lấy tất cả templates chưa bị xóa
     */
    List<SalaryItemTemplateEntity> findByDeletedFalse();

    /**
     * Lấy templates theo loại (ALLOWANCE hoặc DEDUCTION)
     */
    List<SalaryItemTemplateEntity> findByTypeAndDeletedFalse(SalaryItemType type);

    /**
     * Tìm template theo ID và chưa bị xóa
     */
    Optional<SalaryItemTemplateEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Kiểm tra template tồn tại và chưa bị xóa
     */
    boolean existsByIdAndDeletedFalse(Long id);
}
