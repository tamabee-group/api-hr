package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO cho employee tạo yêu cầu đổi ca.
 * Sử dụng requesterShiftId và targetShiftId thay vì assignmentId.
 */
@Data
public class EmployeeSwapRequest {

    @NotNull(message = "ID ca của bạn không được để trống")
    private Long requesterShiftId;

    @NotNull(message = "ID ca muốn đổi không được để trống")
    private Long targetShiftId;

    private String reason;
}
