package com.tamabee.api_hr.mapper.company;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.response.attendance.AdjustmentRequestResponse;
import com.tamabee.api_hr.dto.response.attendance.BreakItemResponse;
import com.tamabee.api_hr.dto.response.attendance.BreakRecordResponse;
import com.tamabee.api_hr.entity.attendance.AdjustmentBreakItemEntity;
import com.tamabee.api_hr.entity.attendance.AttendanceAdjustmentRequestEntity;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;

/**
 * Mapper chuyển đổi giữa AttendanceAdjustmentRequestEntity và DTO.
 * Hỗ trợ nhiều break items trong 1 request.
 */
@Component
public class AttendanceAdjustmentMapper {

    /**
     * Chuyển đổi entity sang response với đầy đủ thông tin
     */
    public AdjustmentRequestResponse toResponse(
            AttendanceAdjustmentRequestEntity entity,
            String employeeName,
            String approverName,
            String assignedToName,
            LocalDate workDate,
            List<BreakRecordEntity> allBreakRecords) {
        if (entity == null) {
            return null;
        }

        // Map break items
        List<BreakItemResponse> breakItemResponses = mapBreakItems(entity.getBreakItems());

        // Map all break records
        List<BreakRecordResponse> allBreakRecordResponses = mapAllBreakRecords(allBreakRecords);

        return AdjustmentRequestResponse.builder()
                .id(entity.getId())
                .requestType(entity.getRequestType())
                .employeeId(entity.getEmployeeId())
                .employeeName(employeeName)
                .attendanceRecordId(entity.getAttendanceRecordId())
                .workDate(workDate)
                .assignedTo(entity.getAssignedTo())
                .assignedToName(assignedToName)
                .originalCheckIn(entity.getOriginalCheckIn())
                .originalCheckOut(entity.getOriginalCheckOut())
                .requestedCheckIn(entity.getRequestedCheckIn())
                .requestedCheckOut(entity.getRequestedCheckOut())
                .breakItems(breakItemResponses)
                .allBreakRecords(allBreakRecordResponses)
                .reason(entity.getReason())
                .status(entity.getStatus())
                .approvedBy(entity.getApprovedBy())
                .approverName(approverName)
                .approvedAt(entity.getApprovedAt())
                .approverComment(entity.getApproverComment())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển đổi entity sang response (không có allBreakRecords)
     */
    public AdjustmentRequestResponse toResponse(
            AttendanceAdjustmentRequestEntity entity,
            String employeeName,
            String approverName,
            String assignedToName,
            LocalDate workDate) {
        return toResponse(entity, employeeName, approverName, assignedToName, workDate, null);
    }

    /**
     * Chuyển đổi entity sang response với thông tin từ attendance record
     */
    public AdjustmentRequestResponse toResponse(
            AttendanceAdjustmentRequestEntity entity,
            String employeeName,
            String approverName,
            AttendanceRecordEntity attendanceRecord) {
        LocalDate workDate = attendanceRecord != null ? attendanceRecord.getWorkDate() : entity.getWorkDate();
        return toResponse(entity, employeeName, approverName, null, workDate, null);
    }

    /**
     * Map danh sách break items từ entity sang response
     */
    private List<BreakItemResponse> mapBreakItems(List<AdjustmentBreakItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(this::mapBreakItem)
                .collect(Collectors.toList());
    }

    /**
     * Map một break item từ entity sang response
     */
    private BreakItemResponse mapBreakItem(AdjustmentBreakItemEntity item) {
        return BreakItemResponse.builder()
                .id(item.getId())
                .breakRecordId(item.getBreakRecordId())
                .breakNumber(item.getBreakNumber())
                .actionType(item.getActionType())
                .originalBreakStart(item.getOriginalBreakStart())
                .originalBreakEnd(item.getOriginalBreakEnd())
                .requestedBreakStart(item.getRequestedBreakStart())
                .requestedBreakEnd(item.getRequestedBreakEnd())
                .createdAt(item.getCreatedAt())
                .build();
    }

    /**
     * Map tất cả break records của ngày
     */
    private List<BreakRecordResponse> mapAllBreakRecords(List<BreakRecordEntity> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .map(this::mapBreakRecord)
                .collect(Collectors.toList());
    }

    /**
     * Map một break record từ entity sang response
     */
    private BreakRecordResponse mapBreakRecord(BreakRecordEntity record) {
        return BreakRecordResponse.builder()
                .id(record.getId())
                .breakNumber(record.getBreakNumber())
                .breakStart(record.getBreakStart())
                .breakEnd(record.getBreakEnd())
                .actualBreakMinutes(record.getActualBreakMinutes())
                .effectiveBreakMinutes(record.getEffectiveBreakMinutes())
                .notes(record.getNotes())
                .breakStartLatitude(record.getBreakStartLatitude())
                .breakStartLongitude(record.getBreakStartLongitude())
                .breakEndLatitude(record.getBreakEndLatitude())
                .breakEndLongitude(record.getBreakEndLongitude())
                .build();
    }
}
