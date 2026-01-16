package com.tamabee.api_hr.service.company.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tamabee.api_hr.dto.request.department.CreateDepartmentRequest;
import com.tamabee.api_hr.dto.request.department.UpdateDepartmentRequest;
import com.tamabee.api_hr.dto.response.department.DefaultApproverResponse;
import com.tamabee.api_hr.dto.response.department.DepartmentResponse;
import com.tamabee.api_hr.dto.response.department.DepartmentSummary;
import com.tamabee.api_hr.dto.response.department.DepartmentTreeNode;
import com.tamabee.api_hr.dto.response.user.UserResponse;

public interface IDepartmentService {

    // Lấy danh sách phòng ban có phân trang
    Page<DepartmentResponse> getDepartments(Pageable pageable);

    // Lấy danh sách phòng ban dạng cây
    List<DepartmentTreeNode> getDepartmentTree();

    // Lấy danh sách phòng ban cho dropdown
    List<DepartmentSummary> getDepartmentsForDropdown();

    // Lấy chi tiết phòng ban
    DepartmentResponse getDepartment(Long id);

    // Tạo phòng ban mới
    DepartmentResponse createDepartment(CreateDepartmentRequest request);

    // Cập nhật phòng ban
    DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request);

    // Xóa phòng ban (soft delete)
    void deleteDepartment(Long id);

    // Lấy danh sách nhân viên trong phòng ban
    List<UserResponse> getDepartmentEmployees(Long departmentId);

    // Lấy người duyệt mặc định cho nhân viên (department manager)
    DefaultApproverResponse getDefaultApprover(Long employeeId);

    // Tìm kiếm phòng ban
    Page<DepartmentResponse> searchDepartments(String keyword, Pageable pageable);
}
