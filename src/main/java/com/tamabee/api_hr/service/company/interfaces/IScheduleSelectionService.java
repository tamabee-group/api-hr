package com.tamabee.api_hr.service.company.interfaces;

import com.tamabee.api_hr.dto.request.schedule.SelectScheduleRequest;
import com.tamabee.api_hr.dto.response.schedule.ScheduleSelectionResponse;
import com.tamabee.api_hr.dto.response.schedule.WorkScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service quản lý yêu cầu chọn lịch làm việc của nhân viên.
 * Nhân viên có thể chọn lịch làm việc phù hợp và cần manager phê duyệt.
 */
public interface IScheduleSelectionService {

    // ==================== Employee Operations ====================

    /**
     * Nhân viên chọn lịch làm việc
     *
     * @param employeeId ID nhân viên
     * @param request    thông tin lịch muốn chọn
     * @return yêu cầu chọn lịch đã tạo
     */
    ScheduleSelectionResponse selectSchedule(Long employeeId, SelectScheduleRequest request);

    // ==================== Manager Operations ====================

    /**
     * Phê duyệt yêu cầu chọn lịch
     *
     * @param selectionId ID yêu cầu chọn lịch
     * @param managerId   ID người phê duyệt
     * @return yêu cầu chọn lịch đã được phê duyệt
     */
    ScheduleSelectionResponse approveSelection(Long selectionId, Long managerId);

    /**
     * Từ chối yêu cầu chọn lịch
     *
     * @param selectionId ID yêu cầu chọn lịch
     * @param managerId   ID người từ chối
     * @param reason      lý do từ chối
     * @return yêu cầu chọn lịch đã bị từ chối
     */
    ScheduleSelectionResponse rejectSelection(Long selectionId, Long managerId, String reason);

    // ==================== Query Operations ====================

    /**
     * Lấy yêu cầu chọn lịch theo ID
     *
     * @param selectionId ID yêu cầu
     * @return yêu cầu chọn lịch
     */
    ScheduleSelectionResponse getSelectionById(Long selectionId);

    /**
     * Lấy danh sách lịch gợi ý cho nhân viên
     * Dựa trên lịch sử chọn lịch và lịch được công ty khuyến nghị
     *
     * @param employeeId ID nhân viên
     * @return danh sách lịch gợi ý
     */
    List<WorkScheduleResponse> getSuggestedSchedules(Long employeeId);

    /**
     * Lấy danh sách lịch có sẵn của công ty cho một ngày cụ thể
     *
     * @param date ngày cần kiểm tra
     * @return danh sách lịch có sẵn
     */
    List<WorkScheduleResponse> getAvailableSchedules(LocalDate date);

    /**
     * Lấy danh sách yêu cầu đang chờ duyệt của công ty (phân trang)
     *
     * @param pageable thông tin phân trang
     * @return danh sách yêu cầu đang chờ duyệt
     */
    Page<ScheduleSelectionResponse> getPendingSelections(Pageable pageable);

    /**
     * Lấy lịch sử chọn lịch của nhân viên
     *
     * @param employeeId ID nhân viên
     * @return danh sách yêu cầu chọn lịch
     */
    List<ScheduleSelectionResponse> getEmployeeSelectionHistory(Long employeeId);
}
