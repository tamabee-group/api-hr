package com.tamabee.api_hr.service.company.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.dto.request.department.CreateDepartmentRequest;
import com.tamabee.api_hr.dto.request.department.UpdateDepartmentRequest;
import com.tamabee.api_hr.dto.response.department.DefaultApproverResponse;
import com.tamabee.api_hr.dto.response.department.DepartmentResponse;
import com.tamabee.api_hr.dto.response.department.DepartmentSummary;
import com.tamabee.api_hr.dto.response.department.DepartmentTreeNode;
import com.tamabee.api_hr.dto.response.user.UserResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.company.DepartmentEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.DepartmentMapper;
import com.tamabee.api_hr.mapper.core.UserMapper;
import com.tamabee.api_hr.repository.company.DepartmentRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IDepartmentService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DepartmentServiceImpl implements IDepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate masterJdbcTemplate;

    public DepartmentServiceImpl(
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            DepartmentMapper departmentMapper,
            UserMapper userMapper,
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
        this.masterJdbcTemplate = masterJdbcTemplate;
    }


    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getDepartments(Pageable pageable) {
        return departmentRepository.findByDeletedFalse(pageable)
                .map(entity -> {
                    int employeeCount = (int) userRepository.countByProfileDepartmentEntityIdAndDeletedFalse(entity.getId());
                    return departmentMapper.toResponse(entity, employeeCount);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentTreeNode> getDepartmentTree() {
        // Lấy tất cả phòng ban gốc (không có parent)
        List<DepartmentEntity> rootDepartments = departmentRepository.findByParentIsNullAndDeletedFalse();
        return rootDepartments.stream()
                .map(this::buildTreeNode)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentSummary> getDepartmentsForDropdown() {
        return departmentRepository.findByDeletedFalse().stream()
                .map(departmentMapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(Long id) {
        DepartmentEntity entity = findDepartment(id);
        int employeeCount = (int) userRepository.countByProfileDepartmentEntityIdAndDeletedFalse(id);
        return departmentMapper.toResponse(entity, employeeCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentResponse> searchDepartments(String keyword, Pageable pageable) {
        return departmentRepository.searchByNameOrCode(keyword, pageable)
                .map(entity -> {
                    int employeeCount = (int) userRepository.countByProfileDepartmentEntityIdAndDeletedFalse(entity.getId());
                    return departmentMapper.toResponse(entity, employeeCount);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getDepartmentEmployees(Long departmentId) {
        findDepartment(departmentId); // Validate department exists
        CompanyEntity company = getCurrentCompanyEntity();
        return userRepository.findByProfileDepartmentEntityIdAndDeletedFalse(departmentId).stream()
                .map(user -> userMapper.toResponse(user, company))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DefaultApproverResponse getDefaultApprover(Long employeeId) {
        UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên", ErrorCode.USER_NOT_FOUND));

        // Lấy department của nhân viên
        if (employee.getProfile() == null || employee.getProfile().getDepartmentEntity() == null) {
            return null; // Không có department
        }

        DepartmentEntity department = employee.getProfile().getDepartmentEntity();
        if (department.getManager() == null) {
            return null; // Department không có manager
        }

        return departmentMapper.toDefaultApproverResponse(department.getManager(), department.getName());
    }


    // ==================== CRUD Operations ====================

    @Override
    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        // Generate code tự động
        String generatedCode = generateDepartmentCode();

        DepartmentEntity entity = departmentMapper.toEntity(request, generatedCode);

        // Set parent nếu có
        if (request.getParentId() != null) {
            DepartmentEntity parent = findDepartment(request.getParentId());
            entity.setParent(parent);
        }

        // Set manager nếu có
        if (request.getManagerId() != null) {
            UserEntity manager = userRepository.findByIdAndDeletedFalse(request.getManagerId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy người quản lý", ErrorCode.DEPARTMENT_MANAGER_NOT_FOUND));
            entity.setManager(manager);
        }

        entity = departmentRepository.save(entity);
        log.info("Đã tạo phòng ban {} - {}", entity.getId(), entity.getName());

        return departmentMapper.toResponse(entity, 0);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        DepartmentEntity entity = findDepartment(id);

        // Update parent nếu có thay đổi
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Phòng ban không thể là parent của chính nó", ErrorCode.DEPARTMENT_CIRCULAR_REFERENCE);
            }
            DepartmentEntity newParent = findDepartment(request.getParentId());
            // Kiểm tra circular reference
            if (isDescendant(newParent, id)) {
                throw new BadRequestException("Không thể tạo vòng lặp trong cấu trúc phòng ban", ErrorCode.DEPARTMENT_CIRCULAR_REFERENCE);
            }
            entity.setParent(newParent);
        }

        // Update manager nếu có thay đổi
        if (request.getManagerId() != null) {
            UserEntity manager = userRepository.findByIdAndDeletedFalse(request.getManagerId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy người quản lý", ErrorCode.DEPARTMENT_MANAGER_NOT_FOUND));
            entity.setManager(manager);
        }

        departmentMapper.updateEntity(entity, request);
        entity = departmentRepository.save(entity);

        log.info("Đã cập nhật phòng ban {}", id);
        int employeeCount = (int) userRepository.countByProfileDepartmentEntityIdAndDeletedFalse(id);
        return departmentMapper.toResponse(entity, employeeCount);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        DepartmentEntity entity = findDepartment(id);

        // Kiểm tra có nhân viên không
        long employeeCount = userRepository.countByProfileDepartmentEntityIdAndDeletedFalse(id);
        if (employeeCount > 0) {
            throw new BadRequestException("Không thể xóa phòng ban có nhân viên", ErrorCode.DEPARTMENT_HAS_EMPLOYEES);
        }

        // Kiểm tra có phòng ban con không
        long childCount = departmentRepository.countByParentIdAndDeletedFalse(id);
        if (childCount > 0) {
            throw new BadRequestException("Không thể xóa phòng ban có phòng ban con", ErrorCode.DEPARTMENT_HAS_CHILDREN);
        }

        entity.setDeleted(true);
        departmentRepository.save(entity);
        log.info("Đã xóa phòng ban {}", id);
    }


    // ==================== Private Helper Methods ====================

    private DepartmentEntity findDepartment(Long id) {
        return departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng ban", ErrorCode.DEPARTMENT_NOT_FOUND));
    }

    /**
     * Generate mã phòng ban tự động theo format: DEPT-XXXX
     */
    private String generateDepartmentCode() {
        long count = departmentRepository.countByDeletedFalse() + 1;
        String code;
        do {
            code = String.format("DEPT-%04d", count);
            count++;
        } while (departmentRepository.existsByCodeAndDeletedFalse(code));
        return code;
    }

    /**
     * Xây dựng tree node đệ quy
     */
    private DepartmentTreeNode buildTreeNode(DepartmentEntity entity) {
        int employeeCount = (int) userRepository.countByProfileDepartmentEntityIdAndDeletedFalse(entity.getId());
        List<DepartmentEntity> children = departmentRepository.findByParentIdAndDeletedFalse(entity.getId());
        
        List<DepartmentTreeNode> childNodes = children.stream()
                .map(this::buildTreeNode)
                .collect(Collectors.toList());

        return departmentMapper.toTreeNode(entity, employeeCount, childNodes);
    }

    /**
     * Kiểm tra xem department có phải là con cháu của targetId không
     * Dùng để phát hiện circular reference
     */
    private boolean isDescendant(DepartmentEntity department, Long targetId) {
        DepartmentEntity current = department;
        while (current != null) {
            if (current.getId().equals(targetId)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Lấy CompanyEntity từ tenant domain hiện tại.
     * Region là thuộc tính của company, dùng để set vào UserResponse.
     */
    private CompanyEntity getCurrentCompanyEntity() {
        String tenantDomain = TenantContext.getCurrentTenant();
        if (tenantDomain == null) {
            return null;
        }
        try {
            String sql = "SELECT id, name, region, logo, status, tenant_domain FROM companies WHERE tenant_domain = ? AND deleted = false";
            return masterJdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                CompanyEntity company = new CompanyEntity();
                company.setId(rs.getLong("id"));
                company.setName(rs.getString("name"));
                company.setRegion(rs.getString("region"));
                company.setLogo(rs.getString("logo"));
                company.setTenantDomain(rs.getString("tenant_domain"));
                String status = rs.getString("status");
                if (status != null) {
                    company.setStatus(com.tamabee.api_hr.enums.CompanyStatus.valueOf(status));
                }
                return company;
            }, tenantDomain);
        } catch (Exception e) {
            log.error("Lỗi khi lấy company entity từ master DB: {}", e.getMessage());
            return null;
        }
    }
}
