package com.tamabee.api_hr.service.company.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tamabee.api_hr.dto.request.attendance.CreateAttendanceKioskRequest;
import com.tamabee.api_hr.dto.request.attendance.KioskCheckInRequest;
import com.tamabee.api_hr.dto.request.attendance.KioskLoginRequest;
import com.tamabee.api_hr.dto.request.attendance.UpdateAttendanceKioskRequest;
import com.tamabee.api_hr.dto.response.attendance.AttendanceKioskResponse;
import com.tamabee.api_hr.dto.response.attendance.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.attendance.KioskActivityResponse;
import com.tamabee.api_hr.dto.response.attendance.KioskEmployeeStatusResponse;
import com.tamabee.api_hr.dto.response.attendance.KioskLoginResponse;

/**
 * Service quản lý máy chấm công cố định (kiosk).
 */
public interface IAttendanceKioskService {

    // ==================== CRUD (Admin) ====================

    Page<AttendanceKioskResponse> getKiosks(Pageable pageable);

    AttendanceKioskResponse getKiosk(Long id);

    AttendanceKioskResponse createKiosk(CreateAttendanceKioskRequest request);

    AttendanceKioskResponse updateKiosk(Long id, UpdateAttendanceKioskRequest request);

    void deleteKiosk(Long id);

    // ==================== Kiosk Operations ====================

    /**
     * Đăng nhập kiosk bằng PIN
     */
    KioskLoginResponse login(KioskLoginRequest request);

    /**
     * Chấm công vào qua kiosk
     */
    AttendanceRecordResponse kioskCheckIn(Long kioskId, KioskCheckInRequest request);

    /**
     * Chấm công ra qua kiosk
     */
    AttendanceRecordResponse kioskCheckOut(Long kioskId, KioskCheckInRequest request);

    /**
     * Bắt đầu giải lao qua kiosk
     */
    AttendanceRecordResponse kioskStartBreak(Long kioskId, KioskCheckInRequest request);

    /**
     * Kết thúc giải lao qua kiosk
     */
    AttendanceRecordResponse kioskEndBreak(Long kioskId, KioskCheckInRequest request);

    /**
     * Lấy hoạt động gần đây trên kiosk
     */
    List<KioskActivityResponse> getRecentActivities(Long kioskId, int limit);

    /**
     * Lấy danh sách nhân viên kèm trạng thái chấm công hôm nay
     */
    List<KioskEmployeeStatusResponse> getEmployeeStatuses();
}
