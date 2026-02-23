package com.tamabee.api_hr.mapper.core;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.request.CreateFeedbackRequest;
import com.tamabee.api_hr.dto.response.FeedbackDetailResponse;
import com.tamabee.api_hr.dto.response.FeedbackReplyResponse;
import com.tamabee.api_hr.dto.response.FeedbackResponse;
import com.tamabee.api_hr.entity.core.FeedbackEntity;
import com.tamabee.api_hr.entity.core.FeedbackReplyEntity;
import com.tamabee.api_hr.enums.FeedbackStatus;

import lombok.RequiredArgsConstructor;

/**
 * Mapper cho Feedback và FeedbackReply entities.
 * Chuyển đổi giữa Entity, Request DTO và Response DTO.
 */
@Component
@RequiredArgsConstructor
public class FeedbackMapper {

    private final ObjectMapper objectMapper;

    /**
     * Chuyển đổi CreateFeedbackRequest sang FeedbackEntity.
     * Thông tin user được truyền từ controller (lấy từ JWT token).
     */
    public FeedbackEntity toEntity(CreateFeedbackRequest request, String tenantDomain,
                                    Long userId, String userName, String userEmail, String companyName) {
        if (request == null) {
            return null;
        }

        return FeedbackEntity.builder()
                .userId(userId)
                .tenantDomain(tenantDomain)
                .userName(userName)
                .userEmail(userEmail)
                .companyName(companyName)
                .type(request.getType())
                .title(request.getTitle())
                .description(request.getDescription())
                .attachmentUrls(serializeUrls(request.getAttachmentUrls()))
                .status(FeedbackStatus.RECEIVED)
                .build();
    }

    /**
     * Chuyển đổi FeedbackEntity sang FeedbackResponse (danh sách).
     */
    public FeedbackResponse toResponse(FeedbackEntity entity) {
        if (entity == null) {
            return null;
        }

        return FeedbackResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tenantDomain(entity.getTenantDomain())
                .userName(entity.getUserName())
                .userEmail(entity.getUserEmail())
                .companyName(entity.getCompanyName())
                .type(entity.getType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .attachmentUrls(parseAttachmentUrls(entity.getAttachmentUrls()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Chuyển đổi FeedbackEntity sang FeedbackDetailResponse (chi tiết + replies).
     */
    public FeedbackDetailResponse toDetailResponse(FeedbackEntity entity, List<FeedbackReplyResponse> replies, String userLanguage) {
        if (entity == null) {
            return null;
        }

        return FeedbackDetailResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tenantDomain(entity.getTenantDomain())
                .userName(entity.getUserName())
                .userEmail(entity.getUserEmail())
                .companyName(entity.getCompanyName())
                .type(entity.getType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .attachmentUrls(parseAttachmentUrls(entity.getAttachmentUrls()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .userLanguage(userLanguage)
                .replies(replies != null ? replies : Collections.emptyList())
                .build();
    }

    /**
     * Chuyển đổi FeedbackReplyEntity sang FeedbackReplyResponse.
     */
    public FeedbackReplyResponse toReplyResponse(FeedbackReplyEntity entity) {
        if (entity == null) {
            return null;
        }

        return FeedbackReplyResponse.builder()
                .id(entity.getId())
                .feedbackId(entity.getFeedbackId())
                .repliedByUserId(entity.getRepliedByUserId())
                .repliedByName(entity.getRepliedByName())
                .content(entity.getContent())
                .fromUser(entity.getFromUser())
                .attachmentUrls(parseAttachmentUrls(entity.getAttachmentUrls()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Parse JSON attachment URLs string thành List<String>.
     */
    public List<String> parseAttachmentUrls(String attachmentUrlsJson) {
        if (attachmentUrlsJson == null || attachmentUrlsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(attachmentUrlsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Serialize List<String> attachment URLs thành JSON string.
     */
    public String serializeUrls(List<String> attachmentUrls) {
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attachmentUrls);
        } catch (Exception e) {
            return null;
        }
    }
}
