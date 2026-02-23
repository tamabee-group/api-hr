package com.tamabee.api_hr.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho phản hồi feedback từ nhân viên Tamabee.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackReplyResponse {

    private Long id;

    private Long feedbackId;

    private Long repliedByUserId;

    private String repliedByName;

    private String content;

    private Boolean fromUser;

    private List<String> attachmentUrls;

    private LocalDateTime createdAt;
}
