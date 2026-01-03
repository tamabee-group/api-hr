package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO để phân ca cho nhân viên.
 */
@Data
public class ShiftAssignmentRequest {

    @NotNull(message = "ID nhân viên không được để trống")
    private Long employeeId;

    @NotNull(message = "ID mẫu ca làm việc không được để trống")
    private Long shiftTemplateId;

    @NotNull(message = "Ngày làm việc không được để trống")
    private LocalDate workDate;
}
