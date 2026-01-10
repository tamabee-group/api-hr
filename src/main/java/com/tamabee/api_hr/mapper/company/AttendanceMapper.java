package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.config.RoundingConfig;
import com.tamabee.api_hr.dto.request.attendance.CheckInRequest;
import com.tamabee.api_hr.dto.response.attendance.*;
import com.tamabee.api_hr.dto.response.payroll.AppliedSettingsSnapshot;
import com.tamabee.api_hr.dto.response.payroll.RoundingConfigSnapshot;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper chuyển đổi giữa AttendanceRecordEntity và DTOs
 */
@Component
public class AttendanceMapper {

    /**
     * Tạo entity mới cho check-in
     */
    public AttendanceRecordEntity toEntity(
            Long employeeId,
            Long companyId,
            LocalDate workDate,
            LocalDateTime checkInTime,
            CheckInRequest request) {

        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setEmployeeId(employeeId);
        entity.setWorkDate(workDate);
        entity.setOriginalCheckIn(checkInTime);
        entity.setStatus(AttendanceStatus.PRESENT);

        if (request != null) {
            entity.setCheckInDeviceId(request.getDeviceId());
            entity.setCheckInLatitude(request.getLatitude());
            entity.setCheckInLongitude(request.getLongitude());
        }

        return entity;
    }

    /**
     * Chuyển entity sang response (basic - không có break records)
     */
    public AttendanceRecordResponse toResponse(AttendanceRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        return AttendanceRecordResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .workDate(entity.getWorkDate())
                .originalCheckIn(entity.getOriginalCheckIn())
                .originalCheckOut(entity.getOriginalCheckOut())
                .roundedCheckIn(entity.getRoundedCheckIn())
                .roundedCheckOut(entity.getRoundedCheckOut())
                .workingMinutes(entity.getWorkingMinutes())
                .overtimeMinutes(entity.getOvertimeMinutes())
                .lateMinutes(entity.getLateMinutes())
                .earlyLeaveMinutes(entity.getEarlyLeaveMinutes())
                .netWorkingMinutes(calculateNetWorkingMinutes(entity))
                .status(entity.getStatus())
                .checkInDeviceId(entity.getCheckInDeviceId())
                .checkOutDeviceId(entity.getCheckOutDeviceId())
                .checkInLatitude(entity.getCheckInLatitude())
                .checkInLongitude(entity.getCheckInLongitude())
                .checkOutLatitude(entity.getCheckOutLatitude())
                .checkOutLongitude(entity.getCheckOutLongitude())
                // Break time fields
                .totalBreakMinutes(entity.getTotalBreakMinutes())
                .effectiveBreakMinutes(entity.getEffectiveBreakMinutes())
                .breakType(entity.getBreakType())
                .breakCompliant(entity.getBreakCompliant())
                .breakRecords(new ArrayList<>())
                // Audit info
                .adjustmentReason(entity.getAdjustmentReason())
                .adjustedBy(entity.getAdjustedBy())
                .adjustedAt(entity.getAdjustedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển entity sang response với tên nhân viên
     */
    public AttendanceRecordResponse toResponse(AttendanceRecordEntity entity, String employeeName) {
        AttendanceRecordResponse response = toResponse(entity);
        if (response != null) {
            response.setEmployeeName(employeeName);
        }
        return response;
    }

    /**
     * Chuyển entity sang response đầy đủ (bao gồm break records, shift info,
     * applied settings)
     */
    public AttendanceRecordResponse toFullResponse(
            AttendanceRecordEntity entity,
            String employeeName,
            List<BreakRecordEntity> breakRecords,
            ShiftInfoResponse shiftInfo,
            AttendanceConfig attendanceConfig,
            BreakConfig breakConfig) {

        if (entity == null) {
            return null;
        }

        AttendanceRecordResponse response = toResponse(entity, employeeName);

        // Map break records
        if (breakRecords != null && !breakRecords.isEmpty()) {
            List<BreakRecordResponse> breakResponses = breakRecords.stream()
                    .map(this::toBreakRecordResponse)
                    .collect(Collectors.toList());
            response.setBreakRecords(breakResponses);
        }

        // Set shift info
        response.setShiftInfo(shiftInfo);

        // Build applied settings snapshot
        if (attendanceConfig != null || breakConfig != null) {
            response.setAppliedSettings(buildAppliedSettingsSnapshot(attendanceConfig, breakConfig));
        }

        return response;
    }

    /**
     * Chuyển BreakRecordEntity sang BreakRecordResponse
     */
    public BreakRecordResponse toBreakRecordResponse(BreakRecordEntity breakRecord) {
        if (breakRecord == null) {
            return null;
        }

        return BreakRecordResponse.builder()
                .id(breakRecord.getId())
                .breakNumber(breakRecord.getBreakNumber())
                .breakStart(breakRecord.getBreakStart())
                .breakEnd(breakRecord.getBreakEnd())
                .actualBreakMinutes(breakRecord.getActualBreakMinutes())
                .effectiveBreakMinutes(breakRecord.getEffectiveBreakMinutes())
                .notes(breakRecord.getNotes())
                .isActive(breakRecord.getBreakEnd() == null)
                .build();
    }

    /**
     * Build applied settings snapshot từ configs
     */
    private AppliedSettingsSnapshot buildAppliedSettingsSnapshot(
            AttendanceConfig attendanceConfig,
            BreakConfig breakConfig) {

        AppliedSettingsSnapshot.AppliedSettingsSnapshotBuilder builder = AppliedSettingsSnapshot.builder();

        if (attendanceConfig != null) {
            builder.checkInRounding(toRoundingSnapshot(attendanceConfig.getCheckInRounding()))
                    .checkOutRounding(toRoundingSnapshot(attendanceConfig.getCheckOutRounding()))
                    .lateGraceMinutes(attendanceConfig.getLateGraceMinutes())
                    .earlyLeaveGraceMinutes(attendanceConfig.getEarlyLeaveGraceMinutes());
        }

        if (breakConfig != null) {
            builder.breakConfig(toBreakConfigSnapshot(breakConfig));
        }

        return builder.build();
    }

    /**
     * Convert RoundingConfig to RoundingConfigSnapshot
     */
    private RoundingConfigSnapshot toRoundingSnapshot(RoundingConfig config) {
        if (config == null) {
            return null;
        }
        return RoundingConfigSnapshot.builder()
                .interval(config.getInterval())
                .direction(config.getDirection())
                .build();
    }

    /**
     * Convert BreakConfig to BreakConfigSnapshot
     */
    private BreakConfigSnapshot toBreakConfigSnapshot(BreakConfig config) {
        if (config == null) {
            return null;
        }
        return BreakConfigSnapshot.builder()
                .breakType(config.getBreakType())
                .minimumBreakMinutes(config.getMinimumBreakMinutes())
                .maximumBreakMinutes(config.getMaximumBreakMinutes())
                .maxBreaksPerDay(config.getMaxBreaksPerDay())
                .legalMinimumBreakMinutes(config.getMinimumBreakMinutes())
                .build();
    }

    /**
     * Calculate net working minutes
     */
    private Integer calculateNetWorkingMinutes(AttendanceRecordEntity entity) {
        if (entity.getWorkingMinutes() == null) {
            return null;
        }
        Integer effectiveBreak = entity.getEffectiveBreakMinutes() != null
                ? entity.getEffectiveBreakMinutes()
                : 0;
        return entity.getWorkingMinutes() - effectiveBreak;
    }

    /**
     * Tạo summary response từ danh sách records
     */
    public AttendanceSummaryResponse toSummaryResponse(
            Long employeeId,
            String employeeName,
            YearMonth period,
            List<AttendanceRecordEntity> records) {

        int presentDays = 0;
        int absentDays = 0;
        int leaveDays = 0;
        int holidayDays = 0;
        int totalWorkingMinutes = 0;
        int totalOvertimeMinutes = 0;
        int totalLateMinutes = 0;
        int totalEarlyLeaveMinutes = 0;

        for (AttendanceRecordEntity record : records) {
            switch (record.getStatus()) {
                case PRESENT -> presentDays++;
                case ABSENT -> absentDays++;
                case LEAVE -> leaveDays++;
                case HOLIDAY -> holidayDays++;
                default -> {
                }
            }

            if (record.getWorkingMinutes() != null) {
                totalWorkingMinutes += record.getWorkingMinutes();
            }
            if (record.getOvertimeMinutes() != null) {
                totalOvertimeMinutes += record.getOvertimeMinutes();
            }
            if (record.getLateMinutes() != null) {
                totalLateMinutes += record.getLateMinutes();
            }
            if (record.getEarlyLeaveMinutes() != null) {
                totalEarlyLeaveMinutes += record.getEarlyLeaveMinutes();
            }
        }

        return AttendanceSummaryResponse.builder()
                .employeeId(employeeId)
                .employeeName(employeeName)
                .period(period)
                .totalWorkingDays(records.size())
                .presentDays(presentDays)
                .absentDays(absentDays)
                .leaveDays(leaveDays)
                .holidayDays(holidayDays)
                .totalWorkingMinutes(totalWorkingMinutes)
                .totalOvertimeMinutes(totalOvertimeMinutes)
                .totalLateMinutes(totalLateMinutes)
                .totalEarlyLeaveMinutes(totalEarlyLeaveMinutes)
                .totalWorkingHours(totalWorkingMinutes / 60.0)
                .totalOvertimeHours(totalOvertimeMinutes / 60.0)
                .build();
    }
}
