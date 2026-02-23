package com.tamabee.api_hr.service.company.interfaces;

import java.util.List;

import com.tamabee.api_hr.dto.request.payroll.AssignSalaryItemRequest;
import com.tamabee.api_hr.dto.request.payroll.UpdateSalaryItemRequest;
import com.tamabee.api_hr.dto.response.payroll.EmployeeSalaryItemResponse;

/**
 * Service interface cho quản lý phụ cấp/khấu trừ của nhân viên
 */
public interface IEmployeeSalaryItemService {

    /**
     * Lấy tất cả salary items của nhân viên
     *
     * @param employeeId ID nhân viên
     * @return Danh sách salary items
     */
    List<EmployeeSalaryItemResponse> getEmployeeSalaryItems(Long employeeId);

    /**
     * Lấy danh sách phụ cấp của nhân viên
     *
     * @param employeeId ID nhân viên
     * @return Danh sách phụ cấp
     */
    List<EmployeeSalaryItemResponse> getEmployeeAllowances(Long employeeId);

    /**
     * Lấy danh sách khấu trừ của nhân viên
     *
     * @param employeeId ID nhân viên
     * @return Danh sách khấu trừ
     */
    List<EmployeeSalaryItemResponse> getEmployeeDeductions(Long employeeId);

    /**
     * Gán salary item cho nhân viên
     *
     * @param employeeId ID nhân viên
     * @param request    Thông tin salary item
     * @return Salary item đã gán
     */
    EmployeeSalaryItemResponse assignSalaryItem(Long employeeId, AssignSalaryItemRequest request);

    /**
     * Cập nhật salary item
     *
     * @param itemId  ID salary item
     * @param request Thông tin cập nhật
     * @return Salary item đã cập nhật
     */
    EmployeeSalaryItemResponse updateSalaryItem(Long itemId, UpdateSalaryItemRequest request);

    /**
     * Xóa salary item (soft delete)
     *
     * @param itemId ID salary item
     */
    void deleteSalaryItem(Long itemId);
}
