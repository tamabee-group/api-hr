package com.tamabee.api_hr.service.company.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tamabee.api_hr.dto.request.attendance.CreateAttendanceLocationRequest;
import com.tamabee.api_hr.dto.request.attendance.UpdateAttendanceLocationRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceLocationResponse;

/**
 * Service quản lý vị trí chấm công.
 * Hỗ trợ CRUD, validation tọa độ, soft delete.
 */
public interface IAttendanceLocationService {

    /**
     * Lấy danh sách vị trí chấm công đang hoạt động (cho nhân viên)
     */
    List<AttendanceLocationResponse> getActiveLocations();

    /**
     * Lấy danh sách vị trí chấm công (phân trang)
     *
     * @param pageable thông tin phân trang
     * @return danh sách vị trí
     */
    Page<AttendanceLocationResponse> getLocations(Pageable pageable);

    /**
     * Lấy chi tiết vị trí chấm công theo ID
     *
     * @param id ID vị trí
     * @return thông tin vị trí
     */
    AttendanceLocationResponse getLocation(Long id);

    /**
     * Tạo vị trí chấm công mới
     *
     * @param request thông tin vị trí
     * @return vị trí đã tạo
     */
    AttendanceLocationResponse createLocation(CreateAttendanceLocationRequest request);

    /**
     * Cập nhật vị trí chấm công
     *
     * @param id      ID vị trí
     * @param request thông tin cập nhật
     * @return vị trí đã cập nhật
     */
    AttendanceLocationResponse updateLocation(Long id, UpdateAttendanceLocationRequest request);

    /**
     * Xóa vị trí chấm công (soft delete)
     *
     * @param id ID vị trí
     */
    void deleteLocation(Long id);
}
