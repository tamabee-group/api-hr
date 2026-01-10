package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.AdjustAttendanceRequest;
import com.tamabee.api_hr.dto.request.AttendanceQueryRequest;
import com.tamabee.api_hr.dto.request.CheckInRequest;
import com.tamabee.api_hr.dto.request.CheckOutRequest;
import com.tamabee.api_hr.dto.request.StartBreakRequest;
import com.tamabee.api_hr.dto.response.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.AttendanceSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Service quản lý chấm công của nhân viên.
 * Hỗ trợ check-in/check-out, làm tròn giờ, tính toán giờ làm việc,
 * phát hiện đi muộn/về sớm, và validation thiết bị/vị trí.
 * Bao gồm cả quản lý break records.
 */
public interface IAttendanceService {

        // ==================== Check-in/Check-out ====================

        /**
         * Ghi nhận check-in của nhân viên
         *
         * @param employeeId ID nhân viên
         * @param request    thông tin check-in (device, location)
         * @return bản ghi chấm công
         */
        AttendanceRecordResponse checkIn(Long employeeId, CheckInRequest request);

        /**
         * Ghi nhận check-out của nhân viên
         *
         * @param employeeId ID nhân viên
         * @param request    thông tin check-out (device, location)
         * @return bản ghi chấm công đã cập nhật
         */
        AttendanceRecordResponse checkOut(Long employeeId, CheckOutRequest request);

        // ==================== Break Management ====================

        /**
         * Bắt đầu giải lao cho nhân viên.
         *
         * @param employeeId ID nhân viên
         * @param request    thông tin break (notes)
         * @return AttendanceRecordResponse với break record mới được thêm vào
         */
        AttendanceRecordResponse startBreak(Long employeeId, StartBreakRequest request);

        /**
         * Kết thúc giải lao cho nhân viên.
         *
         * @param employeeId    ID nhân viên
         * @param breakRecordId ID của break record cần kết thúc
         * @return AttendanceRecordResponse với break record đã được cập nhật
         */
        AttendanceRecordResponse endBreak(Long employeeId, Long breakRecordId);

        // ==================== Adjustment ====================

        /**
         * Điều chỉnh bản ghi chấm công (bởi admin)
         *
         * @param recordId   ID bản ghi chấm công
         * @param adjustedBy ID người điều chỉnh
         * @param request    thông tin điều chỉnh
         * @return bản ghi chấm công đã điều chỉnh
         */
        AttendanceRecordResponse adjustAttendance(Long recordId, Long adjustedBy, AdjustAttendanceRequest request);

        // ==================== Query Operations ====================

        /**
         * Lấy bản ghi chấm công theo ID (bao gồm break records, shift info, applied
         * settings)
         *
         * @param recordId ID bản ghi
         * @return bản ghi chấm công đầy đủ
         */
        AttendanceRecordResponse getAttendanceRecordById(Long recordId);

        /**
         * Lấy bản ghi chấm công của nhân viên theo ngày (bao gồm break records, shift
         * info, applied settings)
         *
         * @param employeeId ID nhân viên
         * @param date       ngày làm việc
         * @return bản ghi chấm công (null nếu không có)
         */
        AttendanceRecordResponse getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date);

        /**
         * Lấy trạng thái chấm công hôm nay của nhân viên.
         *
         * @param employeeId ID nhân viên
         * @return AttendanceRecordResponse chứa đầy đủ thông tin attendance và breaks
         */
        AttendanceRecordResponse getTodayAttendance(Long employeeId);

        /**
         * Lấy danh sách chấm công của công ty (phân trang)
         *
         * @param request  điều kiện lọc
         * @param pageable thông tin phân trang
         * @return danh sách bản ghi chấm công
         */
        Page<AttendanceRecordResponse> getAttendanceRecords(AttendanceQueryRequest request,
                        Pageable pageable);

        /**
         * Lấy danh sách chấm công của nhân viên (phân trang)
         *
         * @param employeeId ID nhân viên
         * @param request    điều kiện lọc
         * @param pageable   thông tin phân trang
         * @return danh sách bản ghi chấm công
         */
        Page<AttendanceRecordResponse> getEmployeeAttendanceRecords(Long employeeId, AttendanceQueryRequest request,
                        Pageable pageable);

        /**
         * Lấy danh sách chấm công của nhân viên theo tháng (cho calendar view)
         *
         * @param employeeId ID nhân viên
         * @param year       năm
         * @param month      tháng
         * @return danh sách bản ghi chấm công
         */
        Page<AttendanceRecordResponse> getEmployeeAttendanceByMonth(Long employeeId, int year, int month);

        /**
         * Lấy tổng hợp chấm công của nhân viên trong một kỳ
         *
         * @param employeeId ID nhân viên
         * @param period     kỳ tính (tháng)
         * @return tổng hợp chấm công
         */
        AttendanceSummaryResponse getAttendanceSummary(Long employeeId, YearMonth period);

        // ==================== Validation ====================

        /**
         * Kiểm tra thiết bị có được đăng ký không
         *
         * @param deviceId ID thiết bị
         * @return true nếu thiết bị hợp lệ hoặc công ty không yêu cầu đăng ký thiết bị
         */
        boolean validateDevice(String deviceId);

        /**
         * Kiểm tra vị trí có nằm trong geo-fence không
         *
         * @param latitude  vĩ độ
         * @param longitude kinh độ
         * @return true nếu vị trí hợp lệ hoặc công ty không yêu cầu geo-location
         */
        boolean validateLocation(Double latitude, Double longitude);
}
