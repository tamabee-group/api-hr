package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request cho việc query danh sách chấm công
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceQueryRequest {

    // Ngày bắt đầu (bắt buộc)
    private LocalDate startDate;

    // Ngày kết thúc (bắt buộc)
    private LocalDate endDate;

    // Lọc theo trạng thái (optional)
    private AttendanceStatus status;

    // Lọc theo nhân viên (optional, dùng cho admin)
    private Long employeeId;
}
