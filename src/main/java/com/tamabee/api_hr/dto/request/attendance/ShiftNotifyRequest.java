package com.tamabee.api_hr.dto.request.attendance;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO để gửi thông báo phân ca đến nhân viên.
 */
@Data
public class ShiftNotifyRequest {

    @NotNull(message = "Năm không được để trống")
    private Integer year;

    @NotNull(message = "Số tuần không được để trống")
    private Integer weekNumber;

    private List<Long> employeeIds; // empty = tất cả liên quan

    private String message; // tin nhắn tùy chỉnh (optional)
}
