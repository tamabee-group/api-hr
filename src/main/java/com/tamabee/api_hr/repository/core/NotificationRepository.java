package com.tamabee.api_hr.repository.core;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.core.NotificationEntity;

/**
 * Repository quản lý thông báo người dùng.
 * Entity này KHÔNG có soft delete (dữ liệu khối lượng lớn).
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * Lấy danh sách thông báo của user, sắp xếp theo thời gian tạo giảm dần (mới nhất trước)
     */
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Đếm số thông báo chưa đọc của user
     */
    Long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Tìm thông báo theo ID và userId (dùng để kiểm tra quyền truy cập)
     */
    Optional<NotificationEntity> findByIdAndUserId(Long id, Long userId);

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}
