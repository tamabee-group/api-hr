package com.tamabee.api_hr.dto.request.attendance;

import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO để tạo hoặc cập nhật nguyện vọng ca làm việc.
 */
@Data
public class ShiftPreferenceRequest {

    @NotNull(message = "Năm không được để trống")
    private Integer year;

    @NotNull(message = "Số tuần không được để trống")
    private Integer weekNumber;

    private List<Integer> daysOfWeek; // 1=Monday..7=Sunday, empty = tất cả

    private Long shiftTemplateId; // nullable nếu custom

    private LocalTime customStartTime; // nullable nếu chọn template

    private LocalTime customEndTime; // nullable nếu chọn template

    private String reason; // nullable
}
