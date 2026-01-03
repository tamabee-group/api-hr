package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.ShiftAssignmentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO cho phân ca nhân viên.
 */
@Data
public class ShiftAssignmentResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long companyId;
    private Long shiftTemplateId;
    private ShiftTemplateResponse shiftTemplate;
    private LocalDate workDate;
    private ShiftAssignmentStatus status;
    private Long swappedWithEmployeeId;
    private String swappedWithEmployeeName;
    private Long swappedFromAssignmentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
