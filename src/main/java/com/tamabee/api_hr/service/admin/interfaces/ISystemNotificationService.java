package com.tamabee.api_hr.service.admin.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tamabee.api_hr.dto.request.CreateSystemNotificationRequest;
import com.tamabee.api_hr.dto.response.SystemNotificationResponse;

/**
 * Service quản lý thông báo hệ thống (System Notification).
 * Hỗ trợ tạo thông báo Markdown gửi cross-tenant, xem danh sách và chi tiết.
 */
public interface ISystemNotificationService {

    /**
     * Tạo thông báo hệ thống mới và gửi đến target audience trên tất cả tenant.
     * Lưu master copy vào master DB, sau đó lặp qua tất cả tenant để:
     * - Query users theo target_audience
     * - Chọn title/content theo region của từng user
     * - Insert notification bằng JDBC
     * - Push WebSocket real-time
     *
     * @param request         Nội dung thông báo (3 ngôn ngữ) và target audience
     * @param createdByUserId ID người tạo (từ JWT token)
     * @param createdByName   Tên người tạo
     * @return Thông tin thông báo đã tạo
     */
    SystemNotificationResponse create(CreateSystemNotificationRequest request,
                                      Long createdByUserId, String createdByName);

    /**
     * Lấy danh sách thông báo hệ thống đã tạo (phân trang).
     * Sắp xếp theo thời gian tạo giảm dần (mới nhất trước).
     *
     * @param pageable Thông tin phân trang
     * @return Danh sách thông báo hệ thống phân trang
     */
    Page<SystemNotificationResponse> getAll(Pageable pageable);

    /**
     * Lấy chi tiết thông báo hệ thống theo ID.
     * Trả về nội dung 3 ngôn ngữ.
     *
     * @param id ID của thông báo hệ thống
     * @return Chi tiết thông báo hệ thống
     * @throws com.tamabee.api_hr.exception.NotFoundException nếu không tìm thấy
     */
    SystemNotificationResponse getById(Long id);
}
