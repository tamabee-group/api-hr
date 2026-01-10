package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.CreateAdjustmentRequest;
import com.tamabee.api_hr.dto.response.AdjustmentRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service quản lý yêu cầu điều chỉnh chấm công.
 * Nhân viên có thể yêu cầu thay đổi giờ check-in/check-out và cần manager phê
 * duyệt.
 */
public interface IAttendanceAdjustmentService {

    // ==================== Employee Operations ====================

    /**
     * Tạo yêu cầu điều chỉnh chấm công
     *
     * @param employeeId ID nhân viên
     * @param request    thông tin yêu cầu điều chỉnh
     * @return yêu cầu điều chỉnh đã tạo
     */
    AdjustmentRequestResponse createAdjustmentRequest(Long employeeId, CreateAdjustmentRequest request);

    // ==================== Manager Operations ====================

    /**
     * Phê duyệt yêu cầu điều chỉnh
     *
     * @param requestId ID yêu cầu điều chỉnh
     * @param managerId ID người phê duyệt
     * @param comment   ghi chú của người phê duyệt (optional)
     * @return yêu cầu điều chỉnh đã được phê duyệt
     */
    AdjustmentRequestResponse approveAdjustment(Long requestId, Long managerId, String comment);

    /**
     * Từ chối yêu cầu điều chỉnh
     *
     * @param requestId ID yêu cầu điều chỉnh
     * @param managerId ID người từ chối
     * @param reason    lý do từ chối
     * @return yêu cầu điều chỉnh đã bị từ chối
     */
    AdjustmentRequestResponse rejectAdjustment(Long requestId, Long managerId, String reason);

    // ==================== Query Operations ====================

    /**
     * Lấy yêu cầu điều chỉnh theo ID
     *
     * @param requestId ID yêu cầu
     * @return yêu cầu điều chỉnh
     */
    AdjustmentRequestResponse getRequestById(Long requestId);

    /**
     * Lấy danh sách yêu cầu đang chờ duyệt của công ty (phân trang)
     * Admin xem tất cả, Manager chỉ xem yêu cầu được gán cho mình
     *
     * @param userId   ID người dùng hiện tại
     * @param isAdmin  true nếu là admin
     * @param pageable thông tin phân trang
     * @return danh sách yêu cầu đang chờ duyệt
     */
    Page<AdjustmentRequestResponse> getPendingRequests(Long userId, boolean isAdmin, Pageable pageable);

    /**
     * Lấy tất cả yêu cầu điều chỉnh của công ty (phân trang)
     * Admin xem tất cả, Manager chỉ xem yêu cầu được gán cho mình
     *
     * @param userId   ID người dùng hiện tại
     * @param isAdmin  true nếu là admin
     * @param pageable thông tin phân trang
     * @return danh sách tất cả yêu cầu
     */
    Page<AdjustmentRequestResponse> getAllRequests(Long userId, boolean isAdmin, Pageable pageable);

    /**
     * Lấy danh sách yêu cầu của nhân viên (phân trang)
     *
     * @param employeeId ID nhân viên
     * @param pageable   thông tin phân trang
     * @return danh sách yêu cầu của nhân viên
     */
    Page<AdjustmentRequestResponse> getEmployeeRequests(Long employeeId, Pageable pageable);

    /**
     * Lấy lịch sử điều chỉnh của một bản ghi chấm công
     *
     * @param attendanceRecordId ID bản ghi chấm công
     * @return danh sách yêu cầu điều chỉnh
     */
    List<AdjustmentRequestResponse> getAdjustmentHistoryByAttendanceRecord(Long attendanceRecordId);

    /**
     * Lấy yêu cầu điều chỉnh của nhân viên theo ngày làm việc
     *
     * @param employeeId ID nhân viên
     * @param workDate   ngày làm việc (yyyy-MM-dd)
     * @return danh sách yêu cầu điều chỉnh cho ngày đó
     */
    List<AdjustmentRequestResponse> getEmployeeRequestsByWorkDate(Long employeeId, java.time.LocalDate workDate);

    /**
     * Thu hồi yêu cầu điều chỉnh (chỉ khi đang PENDING)
     *
     * @param requestId  ID yêu cầu điều chỉnh
     * @param employeeId ID nhân viên (để kiểm tra quyền)
     */
    void cancelAdjustmentRequest(Long requestId, Long employeeId);
}
