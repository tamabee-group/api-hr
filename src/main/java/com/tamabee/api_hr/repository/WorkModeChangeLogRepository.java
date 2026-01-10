package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.audit.WorkModeChangeLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý audit log thay đổi work mode.
 */
@Repository
public interface WorkModeChangeLogRepository extends JpaRepository<WorkModeChangeLogEntity, Long> {

    /**
     * Tìm tất cả log thay đổi work mode (sắp xếp theo thời gian mới nhất)
     */
    List<WorkModeChangeLogEntity> findAllByOrderByChangedAtDesc();

    /**
     * Tìm tất cả log thay đổi work mode (phân trang)
     */
    Page<WorkModeChangeLogEntity> findAll(Pageable pageable);

    /**
     * Đếm số lần thay đổi work mode
     */
    long count();
}
