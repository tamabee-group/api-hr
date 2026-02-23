package com.tamabee.api_hr.dto.request.attendance;

import java.util.List;

import com.tamabee.api_hr.enums.ApplyMode;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO để áp dụng nguyện vọng thành phân ca.
 */
@Data
public class ApplyPreferenceRequest {

    @NotNull(message = "Chế độ áp dụng không được để trống")
    private ApplyMode mode; // EXISTING_TEMPLATES, CREATE_NEW, HYBRID

    private Long newTemplateId; // nếu mode = CREATE_NEW

    private String newTemplateName; // tên ca mới (frontend gửi theo locale)

    private List<Long> templateIds; // nếu mode = EXISTING_TEMPLATES hoặc HYBRID
}
