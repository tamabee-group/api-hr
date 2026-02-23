package com.tamabee.api_hr.service.admin.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tamabee.api_hr.dto.request.CreateFeedbackReplyRequest;
import com.tamabee.api_hr.dto.request.CreateFeedbackRequest;
import com.tamabee.api_hr.dto.response.FeedbackDetailResponse;
import com.tamabee.api_hr.dto.response.FeedbackReplyResponse;
import com.tamabee.api_hr.dto.response.FeedbackResponse;
import com.tamabee.api_hr.enums.FeedbackStatus;
import com.tamabee.api_hr.enums.FeedbackType;

/**
 * Service quản lý Feedback & Support.
 * Hỗ trợ tạo feedback từ user, xem danh sách, phản hồi từ Tamabee staff, và cập nhật trạng thái.
 * Feedback được lưu trong master DB (cross-tenant).
 */
public interface IFeedbackService {

    /**
     * Tạo feedback mới từ user.
     * Lưu vào master DB với status OPEN, gửi notification + email đến Tamabee staff.
     *
     * @param request      Nội dung feedback (type, title, description, attachmentUrls)
     * @param tenantDomain Domain của tenant
     * @param userId       ID user gửi feedback (từ JWT)
     * @param userName     Tên user gửi feedback
     * @param userEmail    Email user gửi feedback
     * @param companyName  Tên công ty của user
     * @return Feedback đã tạo
     */
    FeedbackResponse create(CreateFeedbackRequest request, String tenantDomain,
                           Long userId, String userName, String userEmail, String companyName);

    /**
     * Lấy danh sách feedback của một user (phân trang).
     * Chỉ trả về feedback thuộc userId + tenantDomain.
     *
     * @param userId       ID user
     * @param tenantDomain Domain của tenant
     * @param pageable     Thông tin phân trang
     * @return Danh sách feedback phân trang
     */
    Page<FeedbackResponse> getByUser(Long userId, String tenantDomain, Pageable pageable);

    /**
     * Lấy chi tiết feedback của user, bao gồm danh sách replies.
     * Kiểm tra quyền sở hữu: feedback phải thuộc userId + tenantDomain.
     *
     * @param id           ID feedback
     * @param userId       ID user (để xác thực quyền sở hữu)
     * @param tenantDomain Domain của tenant
     * @return Chi tiết feedback + replies
     * @throws NotFoundException  nếu không tìm thấy feedback
     * @throws ForbiddenException nếu feedback không thuộc user
     */
    FeedbackDetailResponse getByIdForUser(Long id, Long userId, String tenantDomain);

    /**
     * Lấy danh sách tất cả feedback (cho Tamabee staff), hỗ trợ lọc theo status/type.
     *
     * @param status   Lọc theo trạng thái (nullable = không lọc)
     * @param type     Lọc theo loại feedback (nullable = không lọc)
     * @param pageable Thông tin phân trang
     * @return Danh sách feedback phân trang
     */
    Page<FeedbackResponse> getAll(FeedbackStatus status, FeedbackType type, Pageable pageable);

    /**
     * Lấy chi tiết feedback theo ID (cho Tamabee staff), bao gồm danh sách replies.
     *
     * @param id ID feedback
     * @return Chi tiết feedback + replies
     * @throws NotFoundException nếu không tìm thấy feedback
     */
    FeedbackDetailResponse getById(Long id);

    /**
     * Gửi phản hồi cho feedback từ Tamabee staff.
     * Lưu reply, gửi notification đến người gửi feedback (cross-tenant).
     *
     * @param feedbackId      ID feedback cần phản hồi
     * @param request         Nội dung phản hồi
     * @param repliedByUserId ID người phản hồi (từ JWT)
     * @param repliedByName   Tên người phản hồi
     * @return Reply đã tạo
     * @throws NotFoundException nếu không tìm thấy feedback
     */
    FeedbackReplyResponse reply(Long feedbackId, CreateFeedbackReplyRequest request,
                                Long repliedByUserId, String repliedByName, List<String> attachmentUrls);

    /**
     * Cập nhật trạng thái feedback.
     * Validate transition: OPEN→IN_PROGRESS, IN_PROGRESS→RESOLVED, RESOLVED→CLOSED, any→CLOSED.
     *
     * @param id     ID feedback
     * @param status Trạng thái mới
     * @return Feedback đã cập nhật
     * @throws NotFoundException    nếu không tìm thấy feedback
     * @throws BadRequestException  nếu transition không hợp lệ
     */
    FeedbackResponse updateStatus(Long id, FeedbackStatus status);

    /**
     * Xóa feedback và tất cả replies liên quan (hard delete).
     * Chỉ dành cho admin.
     *
     * @param id ID feedback cần xóa
     * @throws NotFoundException nếu không tìm thấy feedback
     */
    void delete(Long id);

    /**
     * Gửi tin nhắn từ user cho feedback (chat-style).
     * Lưu reply với fromUser=true, gửi notification đến Tamabee staff (không gửi email).
     *
     * @param feedbackId   ID feedback
     * @param content      Nội dung tin nhắn
     * @param userId       ID user gửi (từ JWT)
     * @param userName     Tên user
     * @param tenantDomain Domain của tenant
     * @return Reply đã tạo
     */
    FeedbackReplyResponse userReply(Long feedbackId, String content, List<String> attachmentUrls,
                                    Long userId, String userName, String tenantDomain);
}
