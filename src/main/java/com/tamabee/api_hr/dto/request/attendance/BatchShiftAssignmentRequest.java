package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO cho phân ca hàng loạt
 */
@Data
public class BatchShiftAssignmentRequest {

    @NotEmpty(message = "Danh sách nhân viên không được trống")
    private List<Long> employeeIds;

    @NotNull(message = "Mẫu ca không được trống")
    private Long shiftTemplateId;

    @NotNull(message = "Ngày làm việc không được trống")
    private LocalDate workDate;
}
