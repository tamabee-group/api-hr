package com.tamabee.api_hr.entity.core;

import com.tamabee.api_hr.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity lưu trữ phản hồi từ nhân viên Tamabee cho feedback.
 * Lưu trong master DB. Không soft delete.
 */
@Entity
@Table(name = "feedback_replies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FeedbackReplyEntity extends BaseEntity {

    @Column(name = "feedback_id", nullable = false)
    private Long feedbackId;

    @Column(name = "replied_by_user_id", nullable = false)
    private Long repliedByUserId;

    @Column(name = "replied_by_name", nullable = false, length = 100)
    private String repliedByName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** true = tin nhắn từ user, false = phản hồi từ Tamabee staff */
    @Column(name = "from_user", nullable = false)
    @Builder.Default
    private Boolean fromUser = false;

    /** Danh sách URL ảnh đính kèm (JSON array, tối đa 3 ảnh) */
    @Column(name = "attachment_urls", columnDefinition = "TEXT")
    private String attachmentUrls;
}
