package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.request.leave.CreateLeaveRequest;
import com.tamabee.api_hr.dto.response.leave.LeaveBalanceResponse;
import com.tamabee.api_hr.dto.response.leave.LeaveRequestResponse;
import com.tamabee.api_hr.entity.leave.LeaveBalanceEntity;
import com.tamabee.api_hr.entity.leave.LeaveRequestEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa Leave entities và DTOs.
 */
@Component
public class LeaveMapper {

    /**
     * Chuyển đổi LeaveRequestEntity sang response
     *
     * @param entity       entity cần chuyển đổi
     * @param employeeName tên nhân viên
     * @param approverName tên người phê duyệt (null nếu chưa xử lý)
     * @return response
     */
    public LeaveRequestResponse toResponse(
            LeaveRequestEntity entity,
            String employeeName,
            String approverName) {
        if (entity == null) {
            return null;
        }

        return LeaveRequestResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .employeeName(employeeName)
                .leaveType(entity.getLeaveType())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .totalDays(entity.getTotalDays())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .approvedBy(entity.getApprovedBy())
                .approverName(approverName)
                .approvedAt(entity.getApprovedAt())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển đổi request sang entity
     *
     * @param employeeId ID nhân viên
     * @param request    request tạo mới
     * @return entity
     */
    public LeaveRequestEntity toEntity(Long employeeId, CreateLeaveRequest request) {
        if (request == null) {
            return null;
        }

        LeaveRequestEntity entity = new LeaveRequestEntity();
        entity.setEmployeeId(employeeId);
        entity.setLeaveType(request.getLeaveType());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setReason(request.getReason());
        return entity;
    }

    /**
     * Chuyển đổi LeaveBalanceEntity sang response
     *
     * @param entity entity cần chuyển đổi
     * @return response
     */
    public LeaveBalanceResponse toBalanceResponse(LeaveBalanceEntity entity) {
        if (entity == null) {
            return null;
        }

        return LeaveBalanceResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .year(entity.getYear())
                .leaveType(entity.getLeaveType())
                .totalDays(entity.getTotalDays())
                .usedDays(entity.getUsedDays())
                .remainingDays(entity.getRemainingDays())
                .build();
    }
}
