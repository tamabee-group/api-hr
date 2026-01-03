package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.*;
import com.tamabee.api_hr.dto.response.BatchAssignmentResult;
import com.tamabee.api_hr.dto.response.ShiftAssignmentResponse;
import com.tamabee.api_hr.dto.response.ShiftSwapRequestResponse;
import com.tamabee.api_hr.dto.response.ShiftTemplateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface cho quản lý ca làm việc.
 * Bao gồm CRUD shift template, phân ca, và đổi ca.
 */
public interface IShiftService {

    // ==================== Shift Template CRUD ====================

    /**
     * Tạo mẫu ca làm việc mới.
     */
    ShiftTemplateResponse createShiftTemplate(Long companyId, ShiftTemplateRequest request);

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
    Page<ShiftTemplateResponse> getShiftTemplates(Long companyId, Pageable pageable);

    // ==================== Shift Assignment ====================

    /**
     * Phân ca cho nhân viên.
     */
    ShiftAssignmentResponse assignShift(Long companyId, ShiftAssignmentRequest request);

    /**
     * Phân ca cho nhiều nhân viên cùng lúc.
     */
    BatchAssignmentResult batchAssignShift(Long companyId, BatchShiftAssignmentRequest request);

    /**
     * Hủy phân ca.
     */
    void unassignShift(Long assignmentId);

    /**
     * Lấy danh sách phân ca với filter.
     */
    Page<ShiftAssignmentResponse> getShiftAssignments(Long companyId, ShiftAssignmentQuery query, Pageable pageable);

    // ==================== Shift Swap ====================

    /**
     * Tạo yêu cầu đổi ca.
     */
    ShiftSwapRequestResponse requestSwap(Long companyId, Long employeeId, ShiftSwapRequest request);

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
    Page<ShiftSwapRequestResponse> getSwapRequests(Long companyId, SwapRequestQuery query, Pageable pageable);
}
