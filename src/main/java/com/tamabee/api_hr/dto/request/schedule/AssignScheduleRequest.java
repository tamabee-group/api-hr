package com.tamabee.api_hr.dto.request.schedule;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request gán lịch làm việc cho nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignScheduleRequest {

    @NotEmpty(message = "Danh sách nhân viên không được để trống")
    private List<Long> employeeIds;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate effectiveFrom;

    // Ngày kết thúc (null = vô thời hạn)
    private LocalDate effectiveTo;
}
