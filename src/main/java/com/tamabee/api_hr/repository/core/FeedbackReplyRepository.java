package com.tamabee.api_hr.repository.core;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.core.FeedbackReplyEntity;

/**
 * Repository quản lý phản hồi feedback từ nhân viên Tamabee (master DB).
 * Entity này KHÔNG có soft delete.
 */
@Repository
public interface FeedbackReplyRepository extends JpaRepository<FeedbackReplyEntity, Long> {

    /**
     * Lấy danh sách phản hồi của một feedback, sắp xếp theo thời gian tạo tăng dần
     */
    List<FeedbackReplyEntity> findByFeedbackIdOrderByCreatedAtAsc(Long feedbackId);

    /**
     * Xóa tất cả replies của một feedback
     */
    void deleteByFeedbackId(Long feedbackId);
}
