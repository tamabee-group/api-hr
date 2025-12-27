package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.CreateCompanyEmployeeRequest;
import com.tamabee.api_hr.dto.request.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service quản lý nhân viên công ty
 * Dành cho ADMIN_COMPANY và MANAGER_COMPANY
 */
public interface ICompanyEmployeeService {

    /**
     * Lấy danh sách nhân viên của công ty (phân trang)
     * 
     * @param companyId ID công ty
     * @param pageable  thông tin phân trang
     * @return danh sách nhân viên
     */
    Page<UserResponse> getCompanyEmployees(Long companyId, Pageable pageable);

    /**
     * Lấy thông tin chi tiết nhân viên
     * 
     * @param companyId  ID công ty
     * @param employeeId ID nhân viên
     * @return thông tin nhân viên
     */
    UserResponse getCompanyEmployee(Long companyId, Long employeeId);

    /**
     * Tạo nhân viên mới cho công ty
     * 
     * @param companyId ID công ty
     * @param request   thông tin nhân viên
     * @return nhân viên đã tạo
     */
    UserResponse createCompanyEmployee(Long companyId, CreateCompanyEmployeeRequest request);

    /**
     * Cập nhật thông tin nhân viên
     * 
     * @param companyId  ID công ty
     * @param employeeId ID nhân viên
     * @param request    thông tin cập nhật
     * @return nhân viên đã cập nhật
     */
    UserResponse updateCompanyEmployee(Long companyId, Long employeeId, UpdateUserProfileRequest request);

    /**
     * Upload avatar cho nhân viên
     * 
     * @param companyId  ID công ty
     * @param employeeId ID nhân viên
     * @param file       file ảnh
     * @return URL ảnh đã upload
     */
    String uploadEmployeeAvatar(Long companyId, Long employeeId, MultipartFile file);
}
