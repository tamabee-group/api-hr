package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.BreakType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response cho bản ghi chấm công.
 * Bao gồm cả thông tin chấm công, break records, shift info và applied
 * settings.
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
    private Integer netWorkingMinutes;

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

    // Break records (danh sách các lần giải lao)
    private List<BreakRecordResponse> breakRecords;

    // Thông tin ca làm việc (nếu có)
    private ShiftInfoResponse shiftInfo;

    // Các cấu hình đã áp dụng tại thời điểm chấm công
    private AppliedSettingsSnapshot appliedSettings;

    // Audit info
    private String adjustmentReason;
    private Long adjustedBy;
    private LocalDateTime adjustedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
