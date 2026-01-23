package com.tamabee.api_hr.service.company.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tamabee.api_hr.dto.request.attendance.BatchDeleteShiftAssignmentRequest;
import com.tamabee.api_hr.dto.request.attendance.BatchShiftAssignmentRequest;
import com.tamabee.api_hr.dto.request.attendance.ShiftAssignmentQuery;
import com.tamabee.api_hr.dto.request.attendance.ShiftAssignmentRequest;
import com.tamabee.api_hr.dto.request.attendance.ShiftSwapRequest;
import com.tamabee.api_hr.dto.request.attendance.ShiftTemplateRequest;
import com.tamabee.api_hr.dto.request.attendance.SwapRequestQuery;
import com.tamabee.api_hr.dto.response.attendance.BatchAssignmentResult;
import com.tamabee.api_hr.dto.response.attendance.BatchDeleteResult;
import com.tamabee.api_hr.dto.response.attendance.ShiftAssignmentResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftSwapRequestResponse;
import com.tamabee.api_hr.dto.response.attendance.ShiftTemplateResponse;

/**
 * Service interface cho quản lý ca làm việc.
 * Bao gồm CRUD shift template, phân ca, và đổi ca.
 */
public interface IShiftService {

    // ==================== Shift Template CRUD ====================

    /**
     * Tạo mẫu ca làm việc mới.
     */
    ShiftTemplateResponse createShiftTemplate(ShiftTemplateRequest request);

    /**
     * Cập nhật mẫu ca làm việc.
     */
    ShiftTemplateResponse updateShiftTemplate(Long id, ShiftTemplateRequest request);

    /**
     * Xóa mẫu ca làm việc (soft delete).
     */
    void deleteShiftTemplate(Long id);

    /**
     * Lấy danh sách mẫu ca làm việc của công ty.
     */
    Page<ShiftTemplateResponse> getShiftTemplates(Pageable pageable);

    // ==================== Shift Assignment ====================

    /**
     * Phân ca cho nhân viên.
     */
    ShiftAssignmentResponse assignShift(ShiftAssignmentRequest request);

    /**
     * Phân ca cho nhiều nhân viên cùng lúc.
     */
    BatchAssignmentResult batchAssignShift(BatchShiftAssignmentRequest request);

    /**
     * Xóa phân ca hàng loạt theo nhân viên và khoảng thời gian.
     */
    BatchDeleteResult batchDeleteShiftAssignments(BatchDeleteShiftAssignmentRequest request);

    /**
     * Hủy phân ca.
     */
    void unassignShift(Long assignmentId);

    /**
     * Lấy danh sách phân ca với filter.
     */
    Page<ShiftAssignmentResponse> getShiftAssignments(ShiftAssignmentQuery query, Pageable pageable);

    // ==================== Shift Swap ====================

    /**
     * Tạo yêu cầu đổi ca.
     */
    ShiftSwapRequestResponse requestSwap(Long employeeId, ShiftSwapRequest request);

    /**
     * Duyệt yêu cầu đổi ca.
     */
    ShiftSwapRequestResponse approveSwap(Long requestId, Long approverId);

    /**
     * Từ chối yêu cầu đổi ca.
     */
    ShiftSwapRequestResponse rejectSwap(Long requestId, Long approverId, String reason);

    /**
     * Lấy danh sách yêu cầu đổi ca với filter.
     */
    Page<ShiftSwapRequestResponse> getSwapRequests(SwapRequestQuery query, Pageable pageable);
}
