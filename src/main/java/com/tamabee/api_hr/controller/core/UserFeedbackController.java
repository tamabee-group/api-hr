package com.tamabee.api_hr.controller.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.CreateFeedbackReplyRequest;
import com.tamabee.api_hr.dto.request.CreateFeedbackRequest;
import com.tamabee.api_hr.dto.response.FeedbackDetailResponse;
import com.tamabee.api_hr.dto.response.FeedbackReplyResponse;
import com.tamabee.api_hr.dto.response.FeedbackResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.service.admin.interfaces.IFeedbackService;
import com.tamabee.api_hr.service.core.interfaces.IUploadService;
import com.tamabee.api_hr.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller cho user gửi và xem feedback cá nhân.
 * Tất cả endpoint yêu cầu authentication.
 */
@RestController
@RequestMapping("/api/users/me/feedbacks")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserFeedbackController {

    private final IFeedbackService feedbackService;
    private final IUploadService uploadService;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;

    /**
     * Gửi feedback mới (multipart form với ảnh đính kèm).
     * POST /api/users/me/feedbacks
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<FeedbackResponse>> create(
            @Valid @RequestPart("feedback") CreateFeedbackRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        // Upload ảnh đính kèm nếu có
        if (files != null && !files.isEmpty()) {
            List<String> attachmentUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = uploadService.uploadFile(file, "feedback", "attachments");
                attachmentUrls.add(url);
            }
            request.setAttachmentUrls(attachmentUrls);
        }

        // Lấy thông tin user từ JWT
        Long userId = securityUtil.getCurrentUserId();
        String userName = securityUtil.getCurrentUserName();
        String userEmail = securityUtil.getCurrentUserEmail();
        String tenantDomain = securityUtil.getCurrentUserTenantDomain();

        // Lấy companyName từ master DB
        String companyName = companyRepository.findByTenantDomainAndDeletedFalse(tenantDomain)
                .map(CompanyEntity::getName)
                .orElse(null);

        FeedbackResponse response = feedbackService.create(request, tenantDomain, userId, userName, userEmail, companyName);
        return ResponseEntity.ok(BaseResponse.created(response, "Gửi feedback thành công"));
    }

    /**
     * Lấy danh sách feedback của user hiện tại (phân trang).
     * GET /api/users/me/feedbacks
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<FeedbackResponse>>> getMyFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = securityUtil.getCurrentUserId();
        String tenantDomain = securityUtil.getCurrentUserTenantDomain();
        Pageable pageable = PageRequest.of(page, size);
        Page<FeedbackResponse> feedbacks = feedbackService.getByUser(userId, tenantDomain, pageable);
        return ResponseEntity.ok(BaseResponse.success(feedbacks));
    }

    /**
     * Lấy chi tiết feedback + replies của user.
     * GET /api/users/me/feedbacks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<FeedbackDetailResponse>> getMyFeedbackDetail(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        String tenantDomain = securityUtil.getCurrentUserTenantDomain();
        FeedbackDetailResponse feedback = feedbackService.getByIdForUser(id, userId, tenantDomain);
        return ResponseEntity.ok(BaseResponse.success(feedback));
    }

    /**
     * Gửi tin nhắn cho feedback (chat-style, có thể kèm ảnh tối đa 3).
     * POST /api/users/me/feedbacks/{id}/replies
     */
    @PostMapping(value = "/{id}/replies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<FeedbackReplyResponse>> replyMyFeedback(
            @PathVariable Long id,
            @Valid @RequestPart("reply") CreateFeedbackReplyRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        Long userId = securityUtil.getCurrentUserId();
        String userName = securityUtil.getCurrentUserName();
        String tenantDomain = securityUtil.getCurrentUserTenantDomain();

        // Upload ảnh đính kèm nếu có (tối đa 3)
        List<String> attachmentUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files.stream().limit(3).toList()) {
                String url = uploadService.uploadFile(file, "feedback", "replies");
                attachmentUrls.add(url);
            }
        }

        FeedbackReplyResponse reply = feedbackService.userReply(id, request.getContent(), attachmentUrls, userId, userName, tenantDomain);
        return ResponseEntity.ok(BaseResponse.created(reply, "Gửi tin nhắn thành công"));
    }

}
