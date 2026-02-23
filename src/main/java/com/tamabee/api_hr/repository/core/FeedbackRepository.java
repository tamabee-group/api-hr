package com.tamabee.api_hr.repository.core;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.core.FeedbackEntity;
import com.tamabee.api_hr.enums.FeedbackStatus;
import com.tamabee.api_hr.enums.FeedbackType;

/**
 * Repository quản lý feedback từ khách hàng (master DB).
 * Entity này KHÔNG có soft delete.
 */
@Repository
public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

    /**
     * Lấy danh sách feedback của một user theo tenantDomain và userId
     */
    Page<FeedbackEntity> findByTenantDomainAndUserId(String tenantDomain, Long userId, Pageable pageable);

    /**
     * Lọc feedback theo trạng thái
     */
    Page<FeedbackEntity> findByStatus(FeedbackStatus status, Pageable pageable);

    /**
     * Lọc feedback theo loại
     */
    Page<FeedbackEntity> findByType(FeedbackType type, Pageable pageable);

    /**
     * Lọc feedback theo trạng thái và loại
     */
    Page<FeedbackEntity> findByStatusAndType(FeedbackStatus status, FeedbackType type, Pageable pageable);

    /**
     * Đếm số feedback theo trạng thái
     */
    long countByStatus(FeedbackStatus status);
}
