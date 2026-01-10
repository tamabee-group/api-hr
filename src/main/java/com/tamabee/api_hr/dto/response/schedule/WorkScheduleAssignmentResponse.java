package com.tamabee.api_hr.dto.response.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response trả về thông tin gán lịch làm việc cho nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkScheduleAssignmentResponse {

    private Long id;

    private Long employeeId;

    private String employeeName;

    private Long scheduleId;

    private String scheduleName;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private LocalDateTime createdAt;
}
