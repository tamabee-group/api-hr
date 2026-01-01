package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.dto.config.AllowanceRule;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request cập nhật cấu hình phụ cấp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowanceConfigRequest {

    @Valid
    private List<AllowanceRule> allowances;
}
