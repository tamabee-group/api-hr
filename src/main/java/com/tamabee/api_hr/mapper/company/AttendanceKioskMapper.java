package com.tamabee.api_hr.mapper.company;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.request.attendance.CreateAttendanceKioskRequest;
import com.tamabee.api_hr.dto.request.attendance.UpdateAttendanceKioskRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceKioskResponse;
import com.tamabee.api_hr.entity.company.AttendanceKioskEntity;

/**
 * Mapper chuyển đổi giữa AttendanceKioskEntity và DTO.
 */
@Component
public class AttendanceKioskMapper {

    /**
     * Chuyển entity sang response
     */
    public AttendanceKioskResponse toResponse(AttendanceKioskEntity entity, String locationName) {
        if (entity == null) {
            return null;
        }

        return AttendanceKioskResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .pinCode(entity.getPinCode())
                .locationId(entity.getLocationId())
                .locationName(locationName)
                .isActive(entity.getIsActive())
                .lastActiveAt(entity.getLastActiveAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển request tạo mới sang entity
     */
    public AttendanceKioskEntity toEntity(CreateAttendanceKioskRequest request) {
        if (request == null) {
            return null;
        }

        AttendanceKioskEntity entity = new AttendanceKioskEntity();
        entity.setName(request.getName());
        entity.setPinCode(request.getPinCode());
        entity.setLocationId(request.getLocationId());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return entity;
    }

    /**
     * Cập nhật entity từ request (chỉ cập nhật field không null)
     */
    public void updateEntity(AttendanceKioskEntity entity, UpdateAttendanceKioskRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getPinCode() != null) {
            entity.setPinCode(request.getPinCode());
        }
        if (request.getLocationId() != null) {
            entity.setLocationId(request.getLocationId());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }
}
