package com.tamabee.api_hr.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import com.tamabee.api_hr.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chứa thông tin thông báo.
 * Sử dụng code thay vì text để hỗ trợ i18n trên frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;

    // Mã thông báo cho i18n (ví dụ: LEAVE_APPROVED, PAYROLL_PAID)
    private String code;

    // Tham số động để interpolation (ví dụ: {"employeeName": "John", "amount": 50000})
    private Map<String, Object> params;

    // URL điều hướng khi click vào thông báo
    private String targetUrl;

    // Loại thông báo
    private NotificationType type;

    // Trạng thái đã đọc
    private Boolean isRead;

    // Thời gian tạo
    private LocalDateTime createdAt;

    // Tiêu đề thông báo (lưu theo region user tại thời điểm gửi)
    private String title;

    // Nội dung Markdown chi tiết
    private String content;

    // ID tham chiếu đến system_notifications table trong master DB
    private Long systemNotificationId;
}
