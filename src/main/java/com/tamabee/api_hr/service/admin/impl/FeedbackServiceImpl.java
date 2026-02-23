package com.tamabee.api_hr.service.admin.impl;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.constants.NotificationCode;
import com.tamabee.api_hr.dto.request.CreateFeedbackReplyRequest;
import com.tamabee.api_hr.dto.request.CreateFeedbackRequest;
import com.tamabee.api_hr.dto.response.FeedbackDetailResponse;
import com.tamabee.api_hr.dto.response.FeedbackReplyResponse;
import com.tamabee.api_hr.dto.response.FeedbackResponse;
import com.tamabee.api_hr.entity.core.FeedbackEntity;
import com.tamabee.api_hr.entity.core.FeedbackReplyEntity;
import com.tamabee.api_hr.enums.FeedbackStatus;
import com.tamabee.api_hr.enums.FeedbackType;
import com.tamabee.api_hr.enums.NotificationType;
import com.tamabee.api_hr.exception.ForbiddenException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.core.FeedbackMapper;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.core.FeedbackReplyRepository;
import com.tamabee.api_hr.repository.core.FeedbackRepository;
import com.tamabee.api_hr.service.admin.interfaces.IFeedbackService;
import com.tamabee.api_hr.service.core.interfaces.IEmailService;
import com.tamabee.api_hr.service.core.interfaces.INotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation của IFeedbackService.
 * Quản lý feedback từ khách hàng: tạo, xem danh sách, phản hồi, cập nhật trạng thái.
 * Feedback được lưu trong master DB (cross-tenant).
 *
 * Luồng tạo feedback:
 * 1. Lưu feedback vào master DB với status OPEN
 * 2. Gửi notification đến tất cả Tamabee staff
 * 3. Gửi email thông báo đến Tamabee admin
 *
 * Luồng phản hồi:
 * 1. Lưu reply vào master DB
 * 2. Gửi notification cross-tenant đến người gửi feedback
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements IFeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackReplyRepository feedbackReplyRepository;
    private final CompanyRepository companyRepository;
    private final FeedbackMapper feedbackMapper;
    private final INotificationService notificationService;
    private final IEmailService emailService;

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getByUser(Long userId, String tenantDomain, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return feedbackRepository.findByTenantDomainAndUserId(tenantDomain, userId, sortedPageable)
                .map(feedbackMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackDetailResponse getByIdForUser(Long id, Long userId, String tenantDomain) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> NotFoundException.feedback(id));

        // Kiểm tra quyền sở hữu
        if (!feedback.getUserId().equals(userId) || !feedback.getTenantDomain().equals(tenantDomain)) {
            throw new ForbiddenException("Feedback không thuộc về người dùng này");
        }

        List<FeedbackReplyResponse> replies = feedbackReplyRepository
                .findByFeedbackIdOrderByCreatedAtAsc(id)
                .stream()
                .map(feedbackMapper::toReplyResponse)
                .toList();

        String userLanguage = getUserLanguage(feedback.getTenantDomain());
        return feedbackMapper.toDetailResponse(feedback, replies, userLanguage);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAll(FeedbackStatus status, FeedbackType type, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        if (status != null && type != null) {
            return feedbackRepository.findByStatusAndType(status, type, sortedPageable)
                    .map(feedbackMapper::toResponse);
        } else if (status != null) {
            return feedbackRepository.findByStatus(status, sortedPageable)
                    .map(feedbackMapper::toResponse);
        } else if (type != null) {
            return feedbackRepository.findByType(type, sortedPageable)
                    .map(feedbackMapper::toResponse);
        } else {
            return feedbackRepository.findAll(sortedPageable)
                    .map(feedbackMapper::toResponse);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackDetailResponse getById(Long id) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> NotFoundException.feedback(id));

        List<FeedbackReplyResponse> replies = feedbackReplyRepository
                .findByFeedbackIdOrderByCreatedAtAsc(id)
                .stream()
                .map(feedbackMapper::toReplyResponse)
                .toList();

        String userLanguage = getUserLanguage(feedback.getTenantDomain());
        return feedbackMapper.toDetailResponse(feedback, replies, userLanguage);
    }

    // ==================== Creation Operations ====================

    @Override
    @Transactional
    public FeedbackResponse create(CreateFeedbackRequest request, String tenantDomain,
                                   Long userId, String userName, String userEmail, String companyName) {
        // 1. Lưu feedback vào master DB
        FeedbackEntity entity = feedbackMapper.toEntity(request, tenantDomain, userId, userName, userEmail, companyName);
        FeedbackEntity savedEntity = feedbackRepository.save(entity);

        log.info("Đã tạo feedback mới: id={}, type={}, userId={}, tenant={}",
                savedEntity.getId(), savedEntity.getType(), userId, tenantDomain);

        // 2. Gửi notification đến tất cả Tamabee staff
        try {
            Map<String, Object> params = Map.of(
                    "title", savedEntity.getTitle(),
                    "userName", userName != null ? userName : "",
                    "companyName", companyName != null ? companyName : ""
            );
            String targetUrl = "/admin/feedbacks/" + savedEntity.getId();

            notificationService.notifyTamabeeStaff(
                    NotificationCode.FEEDBACK_SUBMITTED,
                    params,
                    targetUrl,
                    NotificationType.FEEDBACK
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification feedback đến Tamabee staff: {}", e.getMessage());
        }

        // 3. Gửi email thông báo đến Tamabee admin
        try {
            emailService.sendFeedbackNotification(
                    savedEntity.getType().name(),
                    savedEntity.getTitle(),
                    userName,
                    userEmail,
                    companyName
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi email feedback notification: {}", e.getMessage());
        }

        return feedbackMapper.toResponse(savedEntity);
    }

    // ==================== Mutation Operations ====================

    @Override
    @Transactional
    public FeedbackReplyResponse reply(Long feedbackId, CreateFeedbackReplyRequest request,
                                       Long repliedByUserId, String repliedByName, List<String> attachmentUrls) {
        FeedbackEntity feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> NotFoundException.feedback(feedbackId));

        // 1. Lưu reply
        FeedbackReplyEntity replyEntity = FeedbackReplyEntity.builder()
                .feedbackId(feedbackId)
                .repliedByUserId(repliedByUserId)
                .repliedByName(repliedByName)
                .content(request.getContent())
                .attachmentUrls(feedbackMapper.serializeUrls(attachmentUrls))
                .build();
        FeedbackReplyEntity savedReply = feedbackReplyRepository.save(replyEntity);

        log.info("Đã tạo reply cho feedback {}: replyId={}, repliedBy={}",
                feedbackId, savedReply.getId(), repliedByName);

        // 2. Gửi notification cross-tenant đến người gửi feedback
        try {
            Map<String, Object> params = Map.of(
                    "title", feedback.getTitle(),
                    "repliedByName", "Tamabee"
            );
            String targetUrl = "/me/help/feedbacks/" + feedbackId;

            notificationService.createNotificationForTenant(
                    feedback.getTenantDomain(),
                    feedback.getUserId(),
                    NotificationCode.FEEDBACK_REPLIED,
                    params,
                    targetUrl,
                    NotificationType.FEEDBACK
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification reply đến user {} trong tenant {}: {}",
                    feedback.getUserId(), feedback.getTenantDomain(), e.getMessage());
        }

        return feedbackMapper.toReplyResponse(savedReply);
    }

    @Override
    @Transactional
    public FeedbackResponse updateStatus(Long id, FeedbackStatus newStatus) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> NotFoundException.feedback(id));

        FeedbackStatus currentStatus = feedback.getStatus();

        // Admin Tamabee có thể chuyển sang bất kỳ trạng thái nào
        if (currentStatus == newStatus) {
            return feedbackMapper.toResponse(feedback);
        }

        feedback.setStatus(newStatus);
        FeedbackEntity updatedEntity = feedbackRepository.save(feedback);

        log.info("Đã cập nhật trạng thái feedback {}: {} → {}", id, currentStatus, newStatus);

        return feedbackMapper.toResponse(updatedEntity);
    }

    // ==================== Helper Methods ====================

    @Override
    @Transactional
    public void delete(Long id) {
        FeedbackEntity feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> NotFoundException.feedback(id));

        // Xóa replies trước (cascade cũng xử lý nhưng explicit cho rõ ràng)
        feedbackReplyRepository.deleteByFeedbackId(id);
        feedbackRepository.delete(feedback);

        log.info("Đã xóa feedback id={}, title={}", id, feedback.getTitle());
    }

    /** Lấy ngôn ngữ của user từ company theo tenantDomain */
    private String getUserLanguage(String tenantDomain) {
        return companyRepository.findByTenantDomainAndDeletedFalse(tenantDomain)
                .map(c -> c.getLanguage() != null ? c.getLanguage() : "vi")
                .orElse("vi");
    }

    @Override
    @Transactional
    public FeedbackReplyResponse userReply(Long feedbackId, String content, List<String> attachmentUrls,
                                           Long userId, String userName, String tenantDomain) {
        FeedbackEntity feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> NotFoundException.feedback(feedbackId));

        // Kiểm tra quyền sở hữu
        if (!feedback.getUserId().equals(userId) || !feedback.getTenantDomain().equals(tenantDomain)) {
            throw new ForbiddenException("Bạn không có quyền trả lời feedback này");
        }

        // Lưu reply với fromUser = true
        FeedbackReplyEntity replyEntity = FeedbackReplyEntity.builder()
                .feedbackId(feedbackId)
                .repliedByUserId(userId)
                .repliedByName(userName)
                .content(content)
                .fromUser(true)
                .attachmentUrls(feedbackMapper.serializeUrls(attachmentUrls))
                .build();
        FeedbackReplyEntity savedReply = feedbackReplyRepository.save(replyEntity);

        log.info("User {} đã gửi tin nhắn cho feedback {}", userId, feedbackId);

        // Gửi notification đến Tamabee staff (không gửi email)
        try {
            Map<String, Object> params = Map.of(
                    "title", feedback.getTitle(),
                    "userName", userName != null ? userName : ""
            );
            String targetUrl = "/admin/feedbacks/" + feedbackId;

            notificationService.notifyTamabeeStaff(
                    NotificationCode.FEEDBACK_SUBMITTED,
                    params,
                    targetUrl,
                    NotificationType.FEEDBACK
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification user reply: {}", e.getMessage());
        }

        return feedbackMapper.toReplyResponse(savedReply);
    }
}
