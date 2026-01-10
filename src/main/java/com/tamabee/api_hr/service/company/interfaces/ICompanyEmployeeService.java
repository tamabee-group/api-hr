package com.tamabee.api_hr.service.company.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.request.user.CreateCompanyEmployeeRequest;
import com.tamabee.api_hr.dto.request.user.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.user.ApproverResponse;
import com.tamabee.api_hr.dto.response.user.UserResponse;

/**
 * Service quản lý nhân viên công ty
 * Dành cho ADMIN_COMPANY và MANAGER_COMPANY
 */
public interface ICompanyEmployeeService {

    /**
     * Lấy danh sách nhân viên của công ty (phân trang)
     * 
     * @param pageable thông tin phân trang
     * @return danh sách nhân viên
     */
    Page<UserResponse> getCompanyEmployees(Pageable pageable);

    /**
     * Lấy thông tin chi tiết nhân viên
     * 
     * @param employeeId ID nhân viên
     * @return thông tin nhân viên
     */
    UserResponse getCompanyEmployee(Long employeeId);

    /**
     * Tạo nhân viên mới cho công ty
     * 
     * @param request thông tin nhân viên
     * @return nhân viên đã tạo
     */
    UserResponse createCompanyEmployee(CreateCompanyEmployeeRequest request);

    /**
     * Cập nhật thông tin nhân viên
     * 
     * @param employeeId ID nhân viên
     * @param request    thông tin cập nhật
     * @return nhân viên đã cập nhật
     */
    UserResponse updateCompanyEmployee(Long employeeId, UpdateUserProfileRequest request);

    /**
     * Upload avatar cho nhân viên
     * 
     * @param employeeId ID nhân viên
     * @param file       file ảnh
     * @return URL ảnh đã upload
     */
    String uploadEmployeeAvatar(Long employeeId, MultipartFile file);

    /**
     * Lấy danh sách người có quyền duyệt (admin và manager)
     * 
     * @return danh sách người duyệt
     */
    List<ApproverResponse> getApprovers();

    /**
     * Kiểm tra email chưa tồn tại trong tenant
     * Throw ConflictException nếu email đã tồn tại
     * 
     * @param email địa chỉ email cần kiểm tra
     */
    void validateEmailNotExists(String email);
}
