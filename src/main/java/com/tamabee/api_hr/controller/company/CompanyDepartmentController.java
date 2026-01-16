package com.tamabee.api_hr.controller.company;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.department.CreateDepartmentRequest;
import com.tamabee.api_hr.dto.request.department.UpdateDepartmentRequest;
import com.tamabee.api_hr.dto.response.department.DepartmentResponse;
import com.tamabee.api_hr.dto.response.department.DepartmentSummary;
import com.tamabee.api_hr.dto.response.department.DepartmentTreeNode;
import com.tamabee.api_hr.dto.response.user.UserResponse;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.service.company.interfaces.IDepartmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý phòng ban của công ty.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền CRUD phòng ban.
 */
@RestController
@RequestMapping("/api/company/departments")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class CompanyDepartmentController {

    private final IDepartmentService departmentService;

    /**
     * Lấy danh sách phòng ban (phân trang)
     * GET /api/company/departments?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<DepartmentResponse>>> getDepartments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<DepartmentResponse> departments = departmentService.getDepartments(pageable);
        return ResponseEntity.ok(BaseResponse.success(departments, "Lấy danh sách phòng ban thành công"));
    }


    /**
     * Lấy cây phòng ban (hierarchical)
     * GET /api/company/departments/tree
     */
    @GetMapping("/tree")
    public ResponseEntity<BaseResponse<List<DepartmentTreeNode>>> getDepartmentTree() {
        List<DepartmentTreeNode> tree = departmentService.getDepartmentTree();
        return ResponseEntity.ok(BaseResponse.success(tree, "Lấy cây phòng ban thành công"));
    }

    /**
     * Lấy danh sách phòng ban cho dropdown
     * GET /api/company/departments/dropdown
     */
    @GetMapping("/dropdown")
    @PreAuthorize(RoleConstants.HAS_ALL_COMPANY_ACCESS)
    public ResponseEntity<BaseResponse<List<DepartmentSummary>>> getDepartmentsForDropdown() {
        List<DepartmentSummary> departments = departmentService.getDepartmentsForDropdown();
        return ResponseEntity.ok(BaseResponse.success(departments, "Lấy danh sách phòng ban thành công"));
    }

    /**
     * Tìm kiếm phòng ban theo tên hoặc mã
     * GET /api/company/departments/search?keyword=IT
     */
    @GetMapping("/search")
    public ResponseEntity<BaseResponse<Page<DepartmentResponse>>> searchDepartments(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<DepartmentResponse> departments = departmentService.searchDepartments(keyword, pageable);
        return ResponseEntity.ok(BaseResponse.success(departments, "Tìm kiếm phòng ban thành công"));
    }

    /**
     * Lấy chi tiết phòng ban
     * GET /api/company/departments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<DepartmentResponse>> getDepartment(@PathVariable Long id) {
        DepartmentResponse department = departmentService.getDepartment(id);
        return ResponseEntity.ok(BaseResponse.success(department, "Lấy thông tin phòng ban thành công"));
    }

    /**
     * Tạo phòng ban mới
     * POST /api/company/departments
     */
    @PostMapping
    public ResponseEntity<BaseResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentResponse department = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(department, "Tạo phòng ban thành công"));
    }

    /**
     * Cập nhật phòng ban
     * PUT /api/company/departments/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<DepartmentResponse>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        DepartmentResponse department = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(BaseResponse.success(department, "Cập nhật phòng ban thành công"));
    }

    /**
     * Xóa phòng ban
     * DELETE /api/company/departments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa phòng ban thành công"));
    }

    /**
     * Lấy danh sách nhân viên trong phòng ban
     * GET /api/company/departments/{id}/employees
     */
    @GetMapping("/{id}/employees")
    public ResponseEntity<BaseResponse<List<UserResponse>>> getDepartmentEmployees(@PathVariable Long id) {
        List<UserResponse> employees = departmentService.getDepartmentEmployees(id);
        return ResponseEntity.ok(BaseResponse.success(employees, "Lấy danh sách nhân viên thành công"));
    }
}
