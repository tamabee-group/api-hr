package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO để yêu cầu đổi ca.
 */
@Data
public class ShiftSwapRequest {

    @NotNull(message = "ID nhân viên được yêu cầu đổi ca không được để trống")
    private Long targetEmployeeId;

    @NotNull(message = "ID phân ca của người yêu cầu không được để trống")
    private Long requesterAssignmentId;

    @NotNull(message = "ID phân ca của người được yêu cầu không được để trống")
    private Long targetAssignmentId;
}
