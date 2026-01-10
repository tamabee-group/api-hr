package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.response.schedule.ScheduleSelectionResponse;
import com.tamabee.api_hr.entity.attendance.ScheduleSelectionEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa ScheduleSelectionEntity và DTO.
 */
@Component
public class ScheduleSelectionMapper {

    /**
     * Chuyển Entity sang Response với thông tin bổ sung
     *
     * @param entity       entity cần chuyển đổi
     * @param employeeName tên nhân viên
     * @param scheduleName tên lịch làm việc
     * @param approverName tên người phê duyệt (có thể null)
     * @return response object
     */
    public ScheduleSelectionResponse toResponse(
            ScheduleSelectionEntity entity,
            String employeeName,
            String scheduleName,
            String approverName) {

        if (entity == null) {
            return null;
        }

        return ScheduleSelectionResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .employeeName(employeeName)
                .scheduleId(entity.getScheduleId())
                .scheduleName(scheduleName)
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .status(entity.getStatus())
                .approvedBy(entity.getApprovedBy())
                .approverName(approverName)
                .approvedAt(entity.getApprovedAt())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
