package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để thay đổi plan của company
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePlanRequest {
    @NotNull(message = "Plan ID không được để trống")
    private Long planId;
}
