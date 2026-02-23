package com.tamabee.api_hr.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.tamabee.api_hr.enums.FeedbackStatus;
import com.tamabee.api_hr.enums.FeedbackType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho feedback (danh sách).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private Long id;

    private Long userId;

    private String tenantDomain;

    private String userName;

    private String userEmail;

    private String companyName;

    private FeedbackType type;

    private String title;

    private String description;

    private List<String> attachmentUrls;

    private FeedbackStatus status;

    private LocalDateTime createdAt;
}
