package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.AllowanceAssignmentRequest;
import com.tamabee.api_hr.dto.response.EmployeeAllowanceResponse;

import java.util.List;

/**
 * Service interface cho quản lý phụ cấp cá nhân của nhân viên
 */
public interface IEmployeeAllowanceService {

    /**
     * Gán phụ cấp cho nhân viên
     *
     * @param employeeId ID nhân viên
     * @param request    Thông tin phụ cấp
     * @return Phụ cấp đã được gán
     */
    EmployeeAllowanceResponse assignAllowance(Long employeeId, AllowanceAssignmentRequest request);

    /**
     * Cập nhật phụ cấp của nhân viên
     *
     * @param assignmentId ID phụ cấp
     * @param request      Thông tin phụ cấp mới
     * @return Phụ cấp đã được cập nhật
     */
    EmployeeAllowanceResponse updateAllowance(Long assignmentId, AllowanceAssignmentRequest request);

    /**
     * Vô hiệu hóa phụ cấp (soft deactivation)
     *
     * @param assignmentId ID phụ cấp
     */
    void deactivateAllowance(Long assignmentId);

    /**
     * Lấy danh sách phụ cấp của nhân viên
     *
     * @param employeeId      ID nhân viên
     * @param includeInactive Có bao gồm phụ cấp đã vô hiệu hóa không
     * @return Danh sách phụ cấp
     */
    List<EmployeeAllowanceResponse> getEmployeeAllowances(Long employeeId, boolean includeInactive);
}
