package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.CreateLeaveRequest;
import com.tamabee.api_hr.dto.response.LeaveBalanceResponse;
import com.tamabee.api_hr.dto.response.LeaveRequestResponse;
import com.tamabee.api_hr.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service quản lý nghỉ phép.
 * Hỗ trợ tạo yêu cầu nghỉ phép, phê duyệt/từ chối, và theo dõi số ngày phép.
 */
public interface ILeaveService {

    // ==================== Employee Operations ====================

    /**
     * Tạo yêu cầu nghỉ phép mới
     *
     * @param employeeId ID nhân viên
     * @param companyId  ID công ty
     * @param request    thông tin yêu cầu nghỉ phép
     * @return yêu cầu nghỉ phép đã tạo
     */
    LeaveRequestResponse createLeaveRequest(Long employeeId, Long companyId, CreateLeaveRequest request);

    /**
     * Hủy yêu cầu nghỉ phép (chỉ khi đang PENDING)
     *
     * @param requestId  ID yêu cầu
     * @param employeeId ID nhân viên (để xác thực quyền)
     * @return yêu cầu nghỉ phép đã hủy
     */
    LeaveRequestResponse cancelLeaveRequest(Long requestId, Long employeeId);

    // ==================== Manager Operations ====================

    /**
     * Phê duyệt yêu cầu nghỉ phép
     *
     * @param requestId ID yêu cầu
     * @param managerId ID người phê duyệt
     * @return yêu cầu nghỉ phép đã được phê duyệt
     */
    LeaveRequestResponse approveLeave(Long requestId, Long managerId);

    /**
     * Từ chối yêu cầu nghỉ phép
     *
     * @param requestId ID yêu cầu
     * @param managerId ID người từ chối
     * @param reason    lý do từ chối
     * @return yêu cầu nghỉ phép đã bị từ chối
     */
    LeaveRequestResponse rejectLeave(Long requestId, Long managerId, String reason);

    // ==================== Query Operations ====================

    /**
     * Lấy yêu cầu nghỉ phép theo ID
     *
     * @param requestId ID yêu cầu
     * @return yêu cầu nghỉ phép
     */
    LeaveRequestResponse getLeaveRequestById(Long requestId);

    /**
     * Lấy danh sách yêu cầu đang chờ duyệt của công ty (phân trang)
     *
     * @param companyId ID công ty
     * @param pageable  thông tin phân trang
     * @return danh sách yêu cầu đang chờ duyệt
     */
    Page<LeaveRequestResponse> getPendingLeaveRequests(Long companyId, Pageable pageable);

    /**
     * Lấy tất cả yêu cầu nghỉ phép của công ty (phân trang)
     *
     * @param companyId ID công ty
     * @param pageable  thông tin phân trang
     * @return danh sách tất cả yêu cầu
     */
    Page<LeaveRequestResponse> getAllLeaveRequests(Long companyId, Pageable pageable);

    /**
     * Lấy danh sách yêu cầu của nhân viên (phân trang)
     *
     * @param employeeId ID nhân viên
     * @param pageable   thông tin phân trang
     * @return danh sách yêu cầu của nhân viên
     */
    Page<LeaveRequestResponse> getEmployeeLeaveRequests(Long employeeId, Pageable pageable);

    // ==================== Balance Operations ====================

    /**
     * Lấy số ngày phép còn lại của nhân viên theo năm
     *
     * @param employeeId ID nhân viên
     * @param year       năm
     * @return danh sách số ngày phép theo từng loại
     */
    List<LeaveBalanceResponse> getLeaveBalance(Long employeeId, Integer year);

    /**
     * Cập nhật số ngày phép của nhân viên
     *
     * @param employeeId ID nhân viên
     * @param type       loại nghỉ phép
     * @param year       năm
     * @param adjustment số ngày điều chỉnh (dương = thêm, âm = trừ)
     */
    void updateLeaveBalance(Long employeeId, LeaveType type, Integer year, Integer adjustment);
}
