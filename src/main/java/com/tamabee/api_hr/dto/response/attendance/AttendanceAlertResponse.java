package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

/**
 * Response cho cảnh báo chấm công của nhân viên.
 * Mỗi alert đại diện cho 1 ngày có vấn đề.
 */
@Data
@Builder
public class AttendanceAlertResponse {

    /** Ngày có vấn đề */
    private LocalDate date;

    /**
     * Loại cảnh báo:
     * - MISSING_CHECKOUT: Đã check-in nhưng chưa check-out
     * - INSUFFICIENT_BREAK: Thời gian giải lao chưa đủ yêu cầu
     * - NO_ATTENDANCE: Ngày làm việc nhưng chưa chấm công
     * - ON_LEAVE: Ngày nghỉ phép (thông tin, không phải lỗi)
     */
    private String alertType;

    /** Mô tả chi tiết (ví dụ: tên loại nghỉ phép) */
    private String detail;
}
