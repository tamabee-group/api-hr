package com.tamabee.api_hr.controller.admin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.CreateFeedbackReplyRequest;
import com.tamabee.api_hr.dto.request.UpdateFeedbackStatusRequest;
import com.tamabee.api_hr.dto.response.FeedbackDetailResponse;
import com.tamabee.api_hr.dto.response.FeedbackReplyResponse;
import com.tamabee.api_hr.dto.response.FeedbackResponse;
import com.tamabee.api_hr.enums.FeedbackStatus;
import com.tamabee.api_hr.enums.FeedbackType;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.admin.interfaces.IFeedbackService;
import com.tamabee.api_hr.service.core.interfaces.IUploadService;
import com.tamabee.api_hr.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý feedback cho Tamabee Staff.
 * Hỗ trợ xem danh sách, chi tiết, phản hồi và cập nhật trạng thái feedback.
 */
@RestController
@RequestMapping("/api/admin/feedbacks")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_TAMABEE_ACCESS)
public class FeedbackController {

    private final IFeedbackService feedbackService;
    private final IUploadService uploadService;
    private final SecurityUtil securityUtil;

    /**
     * Lấy danh sách feedbacks (phân trang, lọc theo status/type).
     * GET /api/admin/feedbacks
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<FeedbackResponse>>> getAll(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) FeedbackType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FeedbackResponse> feedbacks = feedbackService.getAll(status, type, pageable);
        return ResponseEntity.ok(BaseResponse.success(feedbacks));
    }

    /**
     * Lấy chi tiết feedback + replies.
     * GET /api/admin/feedbacks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<FeedbackDetailResponse>> getById(@PathVariable Long id) {
        FeedbackDetailResponse feedback = feedbackService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(feedback));
    }

    /**
     * Gửi phản hồi cho feedback (có thể kèm ảnh, tối đa 3).
     * POST /api/admin/feedbacks/{id}/replies
     */
    @PostMapping(value = "/{id}/replies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<FeedbackReplyResponse>> reply(
            @PathVariable Long id,
            @Valid @RequestPart("reply") CreateFeedbackReplyRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        Long userId = securityUtil.getCurrentUserId();
        String userName = securityUtil.getCurrentUserName();

        // Upload ảnh đính kèm nếu có (tối đa 3)
        List<String> attachmentUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files.stream().limit(3).toList()) {
                String url = uploadService.uploadFile(file, "feedback", "replies");
                attachmentUrls.add(url);
            }
        }

        FeedbackReplyResponse response = feedbackService.reply(id, request, userId, userName, attachmentUrls);
        return ResponseEntity.ok(BaseResponse.created(response, "Gửi phản hồi thành công"));
    }

    /**
     * Cập nhật trạng thái feedback.
     * PUT /api/admin/feedbacks/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<BaseResponse<FeedbackResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeedbackStatusRequest request) {
        FeedbackResponse response = feedbackService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật trạng thái thành công"));
    }

    /**
     * Xóa feedback và tất cả replies liên quan.
     * DELETE /api/admin/feedbacks/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa feedback thành công"));
    }
}
