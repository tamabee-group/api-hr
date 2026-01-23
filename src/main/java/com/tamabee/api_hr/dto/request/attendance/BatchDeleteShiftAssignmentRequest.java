package com.tamabee.api_hr.dto.request.attendance;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO cho xóa phân ca hàng loạt
 * Hỗ trợ xóa theo danh sách nhân viên và khoảng thời gian
 */
@Data
public class BatchDeleteShiftAssignmentRequest {

    @NotEmpty(message = "Danh sách nhân viên không được trống")
    private List<Long> employeeIds;

    @NotNull(message = "Ngày bắt đầu không được trống")
    private LocalDate startDate;

    // Nếu endDate = null hoặc = startDate thì chỉ xóa phân ca cho 1 ngày
    private LocalDate endDate;
}
