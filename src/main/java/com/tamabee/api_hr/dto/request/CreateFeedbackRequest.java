package com.tamabee.api_hr.dto.request;

import java.util.List;

import com.tamabee.api_hr.enums.FeedbackType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO để gửi feedback / yêu cầu hỗ trợ.
 * Thông tin user (userId, email, tên, company) được lấy từ JWT token.
 */
@Data
public class CreateFeedbackRequest {

    @NotNull(message = "Loại feedback không được để trống")
    private FeedbackType type;

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Nội dung mô tả không được để trống")
    private String description;

    @Size(max = 3, message = "Tối đa 3 ảnh đính kèm")
    private List<String> attachmentUrls;
}
