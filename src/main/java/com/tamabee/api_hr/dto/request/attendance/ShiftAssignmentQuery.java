package com.tamabee.api_hr.dto.request.attendance;

import com.tamabee.api_hr.enums.ShiftAssignmentStatus;
import lombok.Data;

import java.time.LocalDate;

/**
 * Query DTO để lọc danh sách phân ca.
 */
@Data
public class ShiftAssignmentQuery {

    private Long employeeId;
    private Long shiftTemplateId;
    private LocalDate workDateFrom;
    private LocalDate workDateTo;
    private ShiftAssignmentStatus status;
}
