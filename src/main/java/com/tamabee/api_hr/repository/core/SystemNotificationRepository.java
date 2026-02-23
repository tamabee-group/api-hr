package com.tamabee.api_hr.repository.core;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.core.SystemNotificationEntity;

/**
 * Repository quản lý thông báo hệ thống (master DB).
 * Entity này KHÔNG có soft delete.
 */
@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotificationEntity, Long> {

    /**
     * Lấy danh sách thông báo hệ thống, sắp xếp theo thời gian tạo giảm dần (mới nhất trước)
     */
    Page<SystemNotificationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
