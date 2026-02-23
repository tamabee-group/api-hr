package com.tamabee.api_hr.service.core.interfaces;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tamabee.api_hr.dto.response.NotificationResponse;
import com.tamabee.api_hr.enums.NotificationType;

/**
 * Service interface cho Notification - quản lý thông báo real-time trong hệ thống.
 * Hỗ trợ tạo, đọc, đánh dấu đã đọc và push thông báo qua WebSocket.
 */
public interface INotificationService {

    // ==================== Query Operations ====================

    /**
     * Lấy danh sách thông báo của user với phân trang.
     * Kết quả được sắp xếp theo thời gian tạo giảm dần (mới nhất trước).
     * 
     * @param userId ID của user
     * @param pageable Thông tin phân trang
     * @return Danh sách thông báo phân trang
     */
    Page<NotificationResponse> getNotifications(Long userId, Pageable pageable);

    /**
     * Lấy chi tiết một thông báo theo ID, kiểm tra quyền sở hữu.
     * 
     * @param id ID của thông báo
     * @param userId ID của user (để xác thực quyền sở hữu)
     * @return Chi tiết thông báo
     * @throws NotFoundException nếu không tìm thấy hoặc không thuộc user
     */
    NotificationResponse getById(Long id, Long userId);

    /**
     * Lấy số lượng thông báo chưa đọc của user.
     * 
     * @param userId ID của user
     * @return Số lượng thông báo chưa đọc
     */
    Long getUnreadCount(Long userId);

    // ==================== Mutation Operations ====================

    /**
     * Đánh dấu một thông báo là đã đọc.
     * 
     * @param userId ID của user (để xác thực quyền sở hữu)
     * @param notificationId ID của thông báo
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc.
     * 
     * @param userId ID của user
     */
    void markAllAsRead(Long userId);

    // ==================== Creation Operations ====================

    /**
     * Tạo thông báo cho một user.
     * Được gọi bởi các service khác khi có sự kiện cần thông báo.
     * 
     * @param userId ID của user nhận thông báo
     * @param code Mã thông báo cho i18n (ví dụ: LEAVE_APPROVED)
     * @param params Tham số động để interpolation (ví dụ: {"employeeName": "John"})
     * @param targetUrl URL điều hướng khi click vào thông báo
     * @param type Loại thông báo
     */
    void createNotification(Long userId, String code, Map<String, Object> params,
                           String targetUrl, NotificationType type);

    /**
     * Tạo thông báo cho nhiều user cùng lúc.
     * Sử dụng khi cần gửi cùng một thông báo cho nhiều người (ví dụ: thông báo lương).
     * 
     * @param userIds Danh sách ID của các user nhận thông báo
     * @param code Mã thông báo cho i18n
     * @param params Tham số động để interpolation
     * @param targetUrl URL điều hướng khi click vào thông báo
     * @param type Loại thông báo
     */
    void createBulkNotifications(List<Long> userIds, String code, Map<String, Object> params,
                                 String targetUrl, NotificationType type);

    /**
     * Tạo thông báo cho tất cả Tamabee admin và manager.
     * Sử dụng khi cần gửi thông báo cho staff Tamabee (ví dụ: có deposit mới).
     * Method này sẽ tự động switch tenant sang "tamabee" để query users.
     * 
     * @param code Mã thông báo cho i18n
     * @param params Tham số động để interpolation
     * @param targetUrl URL điều hướng khi click vào thông báo
     * @param type Loại thông báo
     */
    void notifyTamabeeStaff(String code, Map<String, Object> params,
                           String targetUrl, NotificationType type);

    /**
     * Tạo thông báo cho một user trong tenant cụ thể.
     * Sử dụng khi cần gửi notification từ context master DB sang tenant DB.
     * 
     * @param tenantDomain Domain của tenant
     * @param userId ID của user nhận thông báo
     * @param code Mã thông báo cho i18n
     * @param params Tham số động để interpolation
     * @param targetUrl URL điều hướng khi click vào thông báo
     * @param type Loại thông báo
     */
    void createNotificationForTenant(String tenantDomain, Long userId, String code, 
                                     Map<String, Object> params, String targetUrl, 
                                     NotificationType type);

    // ==================== WebSocket Operations ====================

    /**
     * Push thông báo real-time qua WebSocket đến user.
     * Chỉ gửi nếu user có kết nối WebSocket đang active.
     * 
     * @param userId ID của user nhận thông báo
     * @param notification Thông báo cần push
     */
    void pushNotification(Long userId, NotificationResponse notification);
}
