package com.tamabee.api_hr.entity.core;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity lưu trữ thông báo cho người dùng.
 * Không có soft delete vì đây là dữ liệu có khối lượng lớn.
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationEntity extends BaseEntity {

    /**
     * ID của người dùng nhận thông báo
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * Mã thông báo để hỗ trợ i18n (ví dụ: "LEAVE_APPROVED", "PAYROLL_PAID")
     */
    @Column(nullable = false, length = 50)
    private String code;

    /**
     * Tham số động dạng JSON để nội suy vào message (ví dụ: {"employeeName": "John", "amount": 50000})
     */
    @Column(columnDefinition = "TEXT")
    private String params;

    /**
     * URL điều hướng khi click vào thông báo (ví dụ: "/me/leave", "/me/payroll")
     */
    @Column(length = 255)
    private String targetUrl;

    /**
     * Loại thông báo để phân loại và lọc
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * Trạng thái đã đọc của thông báo
     */
    @Column(nullable = false)
    private Boolean isRead = false;

    /**
     * Tiêu đề thông báo (lưu theo region user tại thời điểm gửi)
     */
    @Column(length = 255)
    private String title;

    /**
     * Nội dung Markdown chi tiết. NULL cho notifications thông thường
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * ID tham chiếu đến system_notifications table trong master DB
     */
    @Column(name = "system_notification_id")
    private Long systemNotificationId;
}
