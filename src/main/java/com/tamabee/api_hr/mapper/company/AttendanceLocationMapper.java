package com.tamabee.api_hr.mapper.company;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.request.attendance.CreateAttendanceLocationRequest;
import com.tamabee.api_hr.dto.request.attendance.UpdateAttendanceLocationRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceLocationResponse;
import com.tamabee.api_hr.entity.company.AttendanceLocationEntity;

/**
 * Mapper chuyển đổi giữa AttendanceLocationEntity và DTO.
 */
@Component
public class AttendanceLocationMapper {

    /**
     * Chuyển đổi entity sang response
     */
    public AttendanceLocationResponse toResponse(AttendanceLocationEntity entity) {
        if (entity == null) {
            return null;
        }

        return AttendanceLocationResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .address(entity.getAddress())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .radiusMeters(entity.getRadiusMeters())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Chuyển đổi request tạo mới sang entity
     */
    public AttendanceLocationEntity toEntity(CreateAttendanceLocationRequest request) {
        if (request == null) {
            return null;
        }

        AttendanceLocationEntity entity = new AttendanceLocationEntity();
        entity.setName(request.getName());
        entity.setAddress(request.getAddress());
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setRadiusMeters(request.getRadiusMeters());
        Boolean isActive = request.getIsActive();
        entity.setIsActive(isActive != null ? isActive : true);
        return entity;
    }

    /**
     * Cập nhật entity từ request
     */
    public void updateEntity(AttendanceLocationEntity entity, UpdateAttendanceLocationRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getAddress() != null) {
            entity.setAddress(request.getAddress());
        }
        if (request.getLatitude() != null) {
            entity.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            entity.setLongitude(request.getLongitude());
        }
        if (request.getRadiusMeters() != null) {
            entity.setRadiusMeters(request.getRadiusMeters());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }
}
