package com.tamabee.api_hr.controller.core;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.response.NotificationResponse;
import com.tamabee.api_hr.service.core.interfaces.INotificationService;
import com.tamabee.api_hr.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * Controller cho Notification - quản lý thông báo của user hiện tại.
 * Tất cả endpoint yêu cầu authentication.
 */
@RestController
@RequestMapping("/api/users/me/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final INotificationService notificationService;
    private final SecurityUtil securityUtil;

    /**
     * Lấy ID của user hiện tại từ JWT token
     */
    private Long getCurrentUserId() {
        return securityUtil.getCurrentUserId();
    }

    /**
     * Lấy chi tiết một thông báo theo ID.
     * Trả về thông tin notification bao gồm title + content Markdown.
     * GET /api/users/me/notifications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<NotificationResponse>> getById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        NotificationResponse notification = notificationService.getById(id, userId);
        return ResponseEntity.ok(BaseResponse.success(notification, "Lấy chi tiết thông báo thành công"));
    }

    /**
     * Lấy danh sách thông báo của user với phân trang.
     * Kết quả được sắp xếp theo thời gian tạo giảm dần (mới nhất trước).
     * GET /api/users/me/notifications
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(BaseResponse.success(notifications, "Lấy danh sách thông báo thành công"));
    }

    /**
     * Lấy số lượng thông báo chưa đọc của user.
     * GET /api/users/me/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<BaseResponse<Long>> getUnreadCount() {
        Long userId = getCurrentUserId();
        Long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(BaseResponse.success(unreadCount, "Lấy số lượng thông báo chưa đọc thành công"));
    }

    /**
     * Đánh dấu một thông báo là đã đọc.
     * PUT /api/users/me/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<BaseResponse<Void>> markAsRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(BaseResponse.success(null, "Đánh dấu thông báo đã đọc thành công"));
    }

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc.
     * PUT /api/users/me/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<BaseResponse<Void>> markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(BaseResponse.success(null, "Đánh dấu tất cả thông báo đã đọc thành công"));
    }
}
