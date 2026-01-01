package com.tamabee.api_hr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response chứa thông tin bản ghi giờ giải lao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakRecordResponse {

    private Long id;

    private Long attendanceRecordId;

    private Long employeeId;

    private String employeeName;

    private Long companyId;

    private LocalDate workDate;

    // Số thứ tự break trong ngày (1, 2, 3, ...)
    private Integer breakNumber;

    // Thời gian bắt đầu giải lao
    private LocalDateTime breakStart;

    // Thời gian kết thúc giải lao
    private LocalDateTime breakEnd;

    // Thời gian giải lao thực tế (phút)
    private Integer actualBreakMinutes;

    // Thời gian giải lao hiệu lực sau khi áp dụng min/max (phút)
    private Integer effectiveBreakMinutes;

    // Ghi chú
    private String notes;

    // Audit info
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
