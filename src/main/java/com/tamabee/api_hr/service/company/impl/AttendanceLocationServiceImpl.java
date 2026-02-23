package com.tamabee.api_hr.service.company.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.request.attendance.CreateAttendanceLocationRequest;
import com.tamabee.api_hr.dto.request.attendance.UpdateAttendanceLocationRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceLocationResponse;
import com.tamabee.api_hr.entity.company.AttendanceLocationEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.AttendanceLocationMapper;
import com.tamabee.api_hr.repository.attendance.AttendanceLocationRepository;
import com.tamabee.api_hr.service.company.interfaces.IAttendanceLocationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation quản lý vị trí chấm công.
 * Hỗ trợ CRUD, validation tọa độ GPS, soft delete.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceLocationServiceImpl implements IAttendanceLocationService {

    private final AttendanceLocationRepository attendanceLocationRepository;
    private final AttendanceLocationMapper attendanceLocationMapper;

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceLocationResponse> getActiveLocations() {
        return attendanceLocationRepository.findByDeletedFalseAndIsActiveTrue()
                .stream()
                .map(attendanceLocationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceLocationResponse> getLocations(Pageable pageable) {
        return attendanceLocationRepository.findByDeletedFalse(pageable)
                .map(attendanceLocationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceLocationResponse getLocation(Long id) {
        AttendanceLocationEntity entity = findLocation(id);
        return attendanceLocationMapper.toResponse(entity);
    }

    // ==================== CRUD Operations ====================

    @Override
    @Transactional
    public AttendanceLocationResponse createLocation(CreateAttendanceLocationRequest request) {
        // Validate tọa độ và bán kính
        validateCoordinates(request.getLatitude(), request.getLongitude(), request.getRadiusMeters());

        AttendanceLocationEntity entity = attendanceLocationMapper.toEntity(request);
        entity = attendanceLocationRepository.save(entity);

        log.info("Đã tạo vị trí chấm công: {} (id={})", entity.getName(), entity.getId());
        return attendanceLocationMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public AttendanceLocationResponse updateLocation(Long id, UpdateAttendanceLocationRequest request) {
        AttendanceLocationEntity entity = findLocation(id);

        // Validate tọa độ và bán kính nếu có thay đổi
        Double lat = request.getLatitude() != null ? request.getLatitude() : entity.getLatitude();
        Double lng = request.getLongitude() != null ? request.getLongitude() : entity.getLongitude();
        Integer radius = request.getRadiusMeters() != null ? request.getRadiusMeters() : entity.getRadiusMeters();
        validateCoordinates(lat, lng, radius);

        // Validate name không được blank nếu có truyền
        if (request.getName() != null && request.getName().isBlank()) {
            throw new BadRequestException("Tên vị trí không được để trống", ErrorCode.INVALID_LOCATION);
        }

        attendanceLocationMapper.updateEntity(entity, request);
        entity = attendanceLocationRepository.save(entity);

        log.info("Đã cập nhật vị trí chấm công: {} (id={})", entity.getName(), id);
        return attendanceLocationMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteLocation(Long id) {
        AttendanceLocationEntity entity = findLocation(id);
        entity.setDeleted(true);
        attendanceLocationRepository.save(entity);

        log.info("Đã xóa vị trí chấm công: {} (id={})", entity.getName(), id);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Tìm vị trí chấm công theo ID (chưa bị xóa)
     */
    private AttendanceLocationEntity findLocation(Long id) {
        return attendanceLocationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> NotFoundException.location(id));
    }

    /**
     * Validate tọa độ GPS và bán kính
     * - latitude: -90 đến 90
     * - longitude: -180 đến 180
     * - radius: lớn hơn 0
     */
    private void validateCoordinates(Double latitude, Double longitude, Integer radiusMeters) {
        if (latitude < -90 || latitude > 90) {
            throw new BadRequestException(
                    "Vĩ độ phải nằm trong khoảng -90 đến 90",
                    ErrorCode.INVALID_LOCATION);
        }

        if (longitude < -180 || longitude > 180) {
            throw new BadRequestException(
                    "Kinh độ phải nằm trong khoảng -180 đến 180",
                    ErrorCode.INVALID_LOCATION);
        }

        if (radiusMeters <= 0) {
            throw new BadRequestException(
                    "Bán kính phải lớn hơn 0",
                    ErrorCode.INVALID_LOCATION);
        }
    }
}
