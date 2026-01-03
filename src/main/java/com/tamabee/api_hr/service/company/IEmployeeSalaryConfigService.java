package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.SalaryConfigRequest;
import com.tamabee.api_hr.dto.response.EmployeeSalaryConfigResponse;
import com.tamabee.api_hr.dto.response.SalaryConfigValidationResponse;

import java.util.List;

/**
 * Service interface cho quản lý cấu hình lương nhân viên
 */
public interface IEmployeeSalaryConfigService {

    /**
     * Tạo cấu hình lương mới cho nhân viên
     * 
     * @param employeeId ID nhân viên
     * @param request    Thông tin cấu hình lương
     * @return Thông tin cấu hình lương đã tạo
     */
    EmployeeSalaryConfigResponse createSalaryConfig(Long employeeId, SalaryConfigRequest request);

    /**
     * Cập nhật cấu hình lương (tạo version mới và đóng version cũ)
     * 
     * @param configId ID cấu hình lương hiện tại
     * @param request  Thông tin cấu hình lương mới
     * @return Thông tin cấu hình lương mới
     */
    EmployeeSalaryConfigResponse updateSalaryConfig(Long configId, SalaryConfigRequest request);

    /**
     * Lấy cấu hình lương hiện tại của nhân viên
     * 
     * @param employeeId ID nhân viên
     * @return Thông tin cấu hình lương hiện tại
     */
    EmployeeSalaryConfigResponse getCurrentSalaryConfig(Long employeeId);

    /**
     * Lấy lịch sử cấu hình lương của nhân viên
     * 
     * @param employeeId ID nhân viên
     * @return Danh sách lịch sử cấu hình lương
     */
    List<EmployeeSalaryConfigResponse> getSalaryConfigHistory(Long employeeId);

    /**
     * Validate cấu hình lương mới - kiểm tra xem có ảnh hưởng đến kỳ lương hiện tại
     * không
     * 
     * @param employeeId ID nhân viên
     * @param request    Thông tin cấu hình lương
     * @return Kết quả validation
     */
    SalaryConfigValidationResponse validateSalaryConfig(Long employeeId, SalaryConfigRequest request);

    /**
     * Xóa cấu hình lương
     * 
     * @param configId ID cấu hình lương
     */
    void deleteSalaryConfig(Long configId);
}
