package com.tamabee.api_hr.service.company.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.dto.request.user.CreateCompanyEmployeeRequest;
import com.tamabee.api_hr.dto.request.user.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.employee.EmployeePersonalInfoResponse;
import com.tamabee.api_hr.dto.response.employee.UserSummaryResponse;
import com.tamabee.api_hr.dto.response.user.ApproverResponse;
import com.tamabee.api_hr.dto.response.user.UserResponse;
import com.tamabee.api_hr.entity.company.DepartmentEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.core.UserMapper;
import com.tamabee.api_hr.repository.company.DepartmentRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.ICompanyEmployeeService;
import com.tamabee.api_hr.service.core.interfaces.IEmailService;
import com.tamabee.api_hr.service.core.interfaces.IUploadService;
import com.tamabee.api_hr.util.EmployeeCodeGenerator;
import com.tamabee.api_hr.util.ReferralCodeGenerator;

import lombok.RequiredArgsConstructor;

/**
 * Service implementation quản lý nhân viên công ty
 */
@Service
@RequiredArgsConstructor
public class CompanyEmployeeServiceImpl implements ICompanyEmployeeService {

    // Các role được phép tạo cho nhân viên công ty
    private static final Set<UserRole> ALLOWED_COMPANY_ROLES = Set.of(
            UserRole.ADMIN_COMPANY,
            UserRole.MANAGER_COMPANY,
            UserRole.EMPLOYEE_COMPANY);

    // Các role được phép tạo cho nhân viên Tamabee
    private static final Set<UserRole> ALLOWED_TAMABEE_ROLES = Set.of(
            UserRole.ADMIN_TAMABEE,
            UserRole.MANAGER_TAMABEE,
            UserRole.EMPLOYEE_TAMABEE);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;
    private final IUploadService uploadService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getCompanyEmployees(Pageable pageable) {
        Page<UserEntity> employees = userRepository.findByDeletedFalse(pageable);
        return employees.map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCompanyEmployee(Long employeeId) {
        UserEntity employee = findEmployeeById(employeeId);
        return userMapper.toResponse(employee);
    }

    @Override
    @Transactional
    public UserResponse createCompanyEmployee(CreateCompanyEmployeeRequest request) {
        // Kiểm tra email đã tồn tại trong tenant hiện tại
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw ConflictException.emailExists(request.getEmail());
        }

        // Lấy tenant hiện tại
        String currentTenant = TenantContext.getCurrentTenant();
        boolean isTamabeeTenant = "tamabee".equals(currentTenant);

        // Xác định role phù hợp dựa trên tenant
        UserRole assignedRole = determineRole(request.getRole(), isTamabeeTenant);

        // Tạo mật khẩu tạm thời
        String temporaryPassword = UUID.randomUUID().toString().substring(0, 8);

        // Tạo user entity
        UserEntity employee = new UserEntity();
        employee.setEmail(request.getEmail());
        employee.setPassword(passwordEncoder.encode(temporaryPassword));
        employee.setRole(assignedRole);
        employee.setStatus(UserStatus.INACTIVE); // Mặc định INACTIVE, chỉ ACTIVE khi có hợp đồng
        employee.setLanguage(request.getLanguage());
        employee.setLocale(request.getLanguage()); // Dùng language làm locale
        employee.setTenantDomain(currentTenant);

        // Tạo mã giới thiệu duy nhất
        String referralCode = generateUniqueReferralCode();

        // Tạo user profile
        UserProfileEntity profile = new UserProfileEntity();
        profile.setName(request.getName());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setZipCode(request.getZipCode());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setReferralCode(referralCode);
        profile.setUser(employee);

        employee.setProfile(profile);

        // Tạo mã nhân viên duy nhất từ ngày sinh
        String employeeCode = EmployeeCodeGenerator.generateForUser(request.getDateOfBirth(), userRepository);
        employee.setEmployeeCode(employeeCode);

        // Tính toán % hoàn thiện profile
        employee.calculateProfileCompleteness();

        // Lưu vào database
        UserEntity savedEmployee = userRepository.save(employee);

        // Gửi email mật khẩu tạm thời
        emailService.sendTemporaryPassword(
                savedEmployee.getEmail(),
                savedEmployee.getEmployeeCode(),
                temporaryPassword,
                savedEmployee.getLanguage());

        return userMapper.toResponse(savedEmployee);
    }

    @Override
    @Transactional
    public UserResponse updateCompanyEmployee(Long employeeId, UpdateUserProfileRequest request) {
        UserEntity employee = findEmployeeById(employeeId);

        // Cập nhật thông tin user
        if (request.getEmail() != null) {
            // Kiểm tra email mới không trùng với user khác
            if (!employee.getEmail().equals(request.getEmail()) &&
                    userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
                throw ConflictException.emailExists(request.getEmail());
            }
            employee.setEmail(request.getEmail());
        }
        if (request.getLanguage() != null) {
            employee.setLanguage(request.getLanguage());
        }
        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }

        // Cập nhật profile
        updateEmployeeProfile(employee, request);

        // Tính toán lại % hoàn thiện profile
        employee.calculateProfileCompleteness();

        UserEntity savedEmployee = userRepository.save(employee);
        return userMapper.toResponse(savedEmployee);
    }

    @Override
    @Transactional
    public String uploadEmployeeAvatar(Long employeeId, MultipartFile file) {
        UserEntity employee = findEmployeeById(employeeId);

        // Xóa ảnh cũ nếu có
        if (employee.getProfile() != null && employee.getProfile().getAvatar() != null) {
            uploadService.deleteFile(employee.getProfile().getAvatar());
        }

        // Upload file mới
        String avatarUrl = uploadService.uploadFile(file, "avatar", employee.getEmployeeCode());

        // Cập nhật vào database
        if (employee.getProfile() == null) {
            employee.setProfile(new UserProfileEntity());
            employee.getProfile().setUser(employee);
        }
        employee.getProfile().setAvatar(avatarUrl);
        userRepository.save(employee);

        return avatarUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApproverResponse> getApprovers() {
        // Lấy danh sách admin và manager trong tenant hiện tại
        List<UserRole> approverRoles = Arrays.asList(UserRole.ADMIN_COMPANY, UserRole.MANAGER_COMPANY);
        List<UserEntity> approvers = userRepository.findByRoleInAndDeletedFalse(approverRoles);

        return approvers.stream()
                .map(user -> ApproverResponse.builder()
                        .id(user.getId())
                        .name(user.getProfile() != null ? user.getProfile().getName() : user.getEmail())
                        .role(user.getRole().name())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public void validateEmailNotExists(String email) {
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            throw ConflictException.emailExists(email);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeePersonalInfoResponse getEmployeePersonalInfo(Long employeeId) {
        UserEntity employee = findEmployeeById(employeeId);
        UserProfileEntity profile = employee.getProfile();

        // Build basic info section
        EmployeePersonalInfoResponse.BasicInfoSection basicInfo = EmployeePersonalInfoResponse.BasicInfoSection.builder()
                .avatar(profile != null ? profile.getAvatar() : null)
                .name(profile != null ? profile.getName() : null)
                .dateOfBirth(profile != null && profile.getDateOfBirth() != null 
                        ? profile.getDateOfBirth().toString() : null)
                .gender(profile != null ? profile.getGender() : null)
                .nationality(profile != null ? profile.getNationality() : null)
                .maritalStatus(profile != null ? profile.getMaritalStatus() : null)
                .nationalId(profile != null ? profile.getNationalId() : null)
                .build();

        // Build work info section
        EmployeePersonalInfoResponse.WorkInfoSection workInfo = EmployeePersonalInfoResponse.WorkInfoSection.builder()
                .jobTitle(profile != null ? profile.getJobTitle() : null)
                .department(profile != null && profile.getDepartmentEntity() != null 
                        ? profile.getDepartmentEntity().getName() : null)
                .departmentId(profile != null && profile.getDepartmentEntity() != null 
                        ? profile.getDepartmentEntity().getId() : null)
                .directManager(buildDirectManager(profile))
                .employmentType(profile != null ? profile.getEmploymentType() : null)
                .joiningDate(profile != null && profile.getJoiningDate() != null 
                        ? profile.getJoiningDate().toString() : null)
                .workLocation(profile != null ? profile.getWorkLocation() : null)
                .build();

        // Build contact info section
        EmployeePersonalInfoResponse.ContactInfoSection contactInfo = EmployeePersonalInfoResponse.ContactInfoSection
                .builder()
                .phone(profile != null ? profile.getPhone() : null)
                .email(employee.getEmail())
                .address(profile != null ? profile.getAddress() : null)
                .zipCode(profile != null ? profile.getZipCode() : null)
                .build();

        // Build bank details section
        EmployeePersonalInfoResponse.BankDetailsSection bankDetails = EmployeePersonalInfoResponse.BankDetailsSection
                .builder()
                .bankAccountType(profile != null ? profile.getBankAccountType() : null)
                .japanBankType(profile != null ? profile.getJapanBankType() : null)
                .bankName(profile != null ? profile.getBankName() : null)
                .bankAccount(profile != null ? profile.getBankAccount() : null)
                .bankAccountName(profile != null ? profile.getBankAccountName() : null)
                .bankCode(profile != null ? profile.getBankCode() : null)
                .bankBranchCode(profile != null ? profile.getBankBranchCode() : null)
                .bankBranchName(profile != null ? profile.getBankBranchName() : null)
                .bankAccountCategory(profile != null ? profile.getBankAccountCategory() : null)
                .bankSymbol(profile != null ? profile.getBankSymbol() : null)
                .bankNumber(profile != null ? profile.getBankNumber() : null)
                .build();

        // Build emergency contact section
        EmployeePersonalInfoResponse.EmergencyContactSection emergencyContact = EmployeePersonalInfoResponse.EmergencyContactSection
                .builder()
                .name(profile != null ? profile.getEmergencyContactName() : null)
                .phone(profile != null ? profile.getEmergencyContactPhone() : null)
                .relation(profile != null ? profile.getEmergencyContactRelation() : null)
                .address(profile != null ? profile.getEmergencyContactAddress() : null)
                .build();

        return EmployeePersonalInfoResponse.builder()
                .basicInfo(basicInfo)
                .workInfo(workInfo)
                .contactInfo(contactInfo)
                .bankDetails(bankDetails)
                .emergencyContact(emergencyContact)
                .build();
    }

    // ==================== Private helper methods ====================

    /**
     * Kiểm tra role hợp lệ cho nhân viên công ty
     */
    private void validateCompanyRole(UserRole role) {
        if (!ALLOWED_COMPANY_ROLES.contains(role)) {
            throw BadRequestException.invalidRole(role.name());
        }
    }

    /**
     * Xác định role phù hợp dựa trên tenant
     * - Tenant "tamabee": chuyển đổi role COMPANY sang TAMABEE tương ứng
     * - Tenant khác: giữ nguyên role COMPANY
     */
    private UserRole determineRole(UserRole requestedRole, boolean isTamabeeTenant) {
        if (isTamabeeTenant) {
            // Chuyển đổi role COMPANY sang TAMABEE tương ứng
            UserRole tamabeeRole = switch (requestedRole) {
                case ADMIN_COMPANY -> UserRole.ADMIN_TAMABEE;
                case MANAGER_COMPANY -> UserRole.MANAGER_TAMABEE;
                case EMPLOYEE_COMPANY -> UserRole.EMPLOYEE_TAMABEE;
                // Nếu đã là role TAMABEE thì giữ nguyên
                case ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE -> requestedRole;
            };
            // Validate role TAMABEE
            if (!ALLOWED_TAMABEE_ROLES.contains(tamabeeRole)) {
                throw BadRequestException.invalidRole(tamabeeRole.name());
            }
            return tamabeeRole;
        } else {
            // Validate role COMPANY
            validateCompanyRole(requestedRole);
            return requestedRole;
        }
    }

    /**
     * Tìm nhân viên theo ID trong tenant hiện tại
     */
    private UserEntity findEmployeeById(Long employeeId) {
        return userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));
    }

    /**
     * Tạo mã giới thiệu duy nhất
     */
    private String generateUniqueReferralCode() {
        String referralCode;
        do {
            referralCode = ReferralCodeGenerator.generate();
        } while (userRepository.existsByProfileReferralCodeAndDeletedFalse(referralCode));
        return referralCode;
    }

    /**
     * Cập nhật thông tin profile nhân viên
     */
    private void updateEmployeeProfile(UserEntity employee, UpdateUserProfileRequest request) {
        if (employee.getProfile() == null) {
            employee.setProfile(new UserProfileEntity());
            employee.getProfile().setUser(employee);
        }

        UserProfileEntity profile = employee.getProfile();

        if (request.getName() != null)
            profile.setName(request.getName());
        if (request.getPhone() != null)
            profile.setPhone(request.getPhone());
        if (request.getZipCode() != null)
            profile.setZipCode(request.getZipCode());
        if (request.getAddress() != null)
            profile.setAddress(request.getAddress());
        // Basic info
        if (request.getDateOfBirth() != null)
            profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)
            profile.setGender(request.getGender());
        if (request.getNationality() != null)
            profile.setNationality(request.getNationality());
        if (request.getMaritalStatus() != null)
            profile.setMaritalStatus(request.getMaritalStatus());
        if (request.getNationalId() != null)
            profile.setNationalId(request.getNationalId());
        // Work info
        if (request.getJobTitle() != null)
            profile.setJobTitle(request.getJobTitle());
        if (request.getDepartmentId() != null) {
            DepartmentEntity department = departmentRepository.findByIdAndDeletedFalse(request.getDepartmentId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng ban", ErrorCode.DEPARTMENT_NOT_FOUND));
            profile.setDepartmentEntity(department);
        }
        if (request.getEmploymentType() != null)
            profile.setEmploymentType(request.getEmploymentType());
        if (request.getJoiningDate() != null)
            profile.setJoiningDate(request.getJoiningDate());
        if (request.getWorkLocation() != null)
            profile.setWorkLocation(request.getWorkLocation());
        // Bank info - Common
        if (request.getBankAccountType() != null)
            profile.setBankAccountType(request.getBankAccountType());
        if (request.getJapanBankType() != null)
            profile.setJapanBankType(request.getJapanBankType());
        if (request.getBankName() != null)
            profile.setBankName(request.getBankName());
        if (request.getBankAccount() != null)
            profile.setBankAccount(request.getBankAccount());
        if (request.getBankAccountName() != null)
            profile.setBankAccountName(request.getBankAccountName());
        // Bank info - Japan specific
        if (request.getBankCode() != null)
            profile.setBankCode(request.getBankCode());
        if (request.getBankBranchCode() != null)
            profile.setBankBranchCode(request.getBankBranchCode());
        if (request.getBankBranchName() != null)
            profile.setBankBranchName(request.getBankBranchName());
        if (request.getBankAccountCategory() != null)
            profile.setBankAccountCategory(request.getBankAccountCategory());
        // Bank info - Japan Post Bank (ゆうちょ銀行)
        if (request.getBankSymbol() != null)
            profile.setBankSymbol(request.getBankSymbol());
        if (request.getBankNumber() != null)
            profile.setBankNumber(request.getBankNumber());
        if (request.getEmergencyContactName() != null)
            profile.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null)
            profile.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getEmergencyContactRelation() != null)
            profile.setEmergencyContactRelation(request.getEmergencyContactRelation());
        if (request.getEmergencyContactAddress() != null)
            profile.setEmergencyContactAddress(request.getEmergencyContactAddress());
    }

    /**
     * Build direct manager info từ department manager
     */
    private UserSummaryResponse buildDirectManager(UserProfileEntity profile) {
        if (profile == null || profile.getDepartmentEntity() == null) {
            return null;
        }
        
        DepartmentEntity department = profile.getDepartmentEntity();
        if (department.getManager() == null) {
            return null;
        }
        
        UserEntity manager = department.getManager();
        UserProfileEntity managerProfile = manager.getProfile();
        
        return UserSummaryResponse.builder()
                .id(manager.getId())
                .name(managerProfile != null ? managerProfile.getName() : null)
                .jobTitle(managerProfile != null ? managerProfile.getJobTitle() : null)
                .avatar(managerProfile != null ? managerProfile.getAvatar() : null)
                .build();
    }

    @Override
    @Transactional
    public void deleteEmployee(Long employeeId) {
        UserEntity employee = findEmployeeById(employeeId);

        // Xóa avatar nếu có
        if (employee.getProfile() != null && employee.getProfile().getAvatar() != null) {
            uploadService.deleteFile(employee.getProfile().getAvatar());
        }

        // Hard delete user (cascade sẽ xóa profile)
        userRepository.delete(employee);
    }
}
