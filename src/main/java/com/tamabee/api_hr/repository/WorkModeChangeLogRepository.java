package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.audit.WorkModeChangeLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý audit log thay đổi work mode của công ty.
 */
@Repository
public interface WorkModeChangeLogRepository extends JpaRepository<WorkModeChangeLogEntity, Long> {

    /**
     * Tìm tất cả log thay đổi work mode của công ty
     */
    List<WorkModeChangeLogEntity> findByCompanyIdOrderByChangedAtDesc(Long companyId);

    /**
     * Tìm tất cả log thay đổi work mode của công ty (phân trang)
     */
    Page<WorkModeChangeLogEntity> findByCompanyId(Long companyId, Pageable pageable);

    /**
     * Đếm số lần thay đổi work mode của công ty
     */
    long countByCompanyId(Long companyId);
}
