package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.BreakType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response cho bản ghi chấm công
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long companyId;
    private LocalDate workDate;

    // Thời gian gốc
    private LocalDateTime originalCheckIn;
    private LocalDateTime originalCheckOut;

    // Thời gian sau làm tròn
    private LocalDateTime roundedCheckIn;
    private LocalDateTime roundedCheckOut;

    // Tính toán (phút)
    private Integer workingMinutes;
    private Integer overtimeMinutes;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;

    // Trạng thái
    private AttendanceStatus status;

    // Device info
    private String checkInDeviceId;
    private String checkOutDeviceId;

    // Location info
    private Double checkInLatitude;
    private Double checkInLongitude;
    private Double checkOutLatitude;
    private Double checkOutLongitude;

    // Break time fields
    private Integer totalBreakMinutes;
    private Integer effectiveBreakMinutes;
    private BreakType breakType;
    private Boolean breakCompliant;

    // Audit info
    private String adjustmentReason;
    private Long adjustedBy;
    private LocalDateTime adjustedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
