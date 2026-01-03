package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.DeductionAssignmentRequest;
import com.tamabee.api_hr.dto.response.EmployeeDeductionResponse;

import java.util.List;

/**
 * Service interface cho quản lý khấu trừ cá nhân của nhân viên
 */
public interface IEmployeeDeductionService {

    /**
     * Gán khấu trừ cho nhân viên
     *
     * @param employeeId ID nhân viên
     * @param request    Thông tin khấu trừ
     * @return Khấu trừ đã được gán
     */
    EmployeeDeductionResponse assignDeduction(Long employeeId, DeductionAssignmentRequest request);

    /**
     * Cập nhật khấu trừ của nhân viên
     *
     * @param assignmentId ID khấu trừ
     * @param request      Thông tin khấu trừ mới
     * @return Khấu trừ đã được cập nhật
     */
    EmployeeDeductionResponse updateDeduction(Long assignmentId, DeductionAssignmentRequest request);

    /**
     * Vô hiệu hóa khấu trừ (soft deactivation)
     *
     * @param assignmentId ID khấu trừ
     */
    void deactivateDeduction(Long assignmentId);

    /**
     * Lấy danh sách khấu trừ của nhân viên
     *
     * @param employeeId      ID nhân viên
     * @param includeInactive Có bao gồm khấu trừ đã vô hiệu hóa không
     * @return Danh sách khấu trừ
     */
    List<EmployeeDeductionResponse> getEmployeeDeductions(Long employeeId, boolean includeInactive);
}
