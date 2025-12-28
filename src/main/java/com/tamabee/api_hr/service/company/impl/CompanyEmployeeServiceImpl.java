package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.request.CreateCompanyEmployeeRequest;
import com.tamabee.api_hr.dto.request.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.ForbiddenException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.core.UserMapper;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.ICompanyEmployeeService;
import com.tamabee.api_hr.service.core.IEmailService;
import com.tamabee.api_hr.service.core.IUploadService;
import com.tamabee.api_hr.util.EmployeeCodeGenerator;
import com.tamabee.api_hr.util.LocaleUtil;
import com.tamabee.api_hr.util.ReferralCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

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

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;
    private final IUploadService uploadService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getCompanyEmployees(Long companyId, Pageable pageable) {
        // Kiểm tra công ty tồn tại
        validateCompanyExists(companyId);

        Page<UserEntity> employees = userRepository.findByCompanyIdAndDeletedFalse(companyId, pageable);
        return employees.map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCompanyEmployee(Long companyId, Long employeeId) {
        UserEntity employee = findEmployeeByIdAndCompany(employeeId, companyId);
        return userMapper.toResponse(employee);
    }

    @Override
    @Transactional
    public UserResponse createCompanyEmployee(Long companyId, CreateCompanyEmployeeRequest request) {
        // Kiểm tra công ty tồn tại
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> NotFoundException.company(companyId));

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw ConflictException.emailExists(request.getEmail());
        }

        // Kiểm tra role hợp lệ cho nhân viên công ty
        validateCompanyRole(request.getRole());

        // Tạo mật khẩu tạm thời
        String temporaryPassword = UUID.randomUUID().toString().substring(0, 8);

        // Lấy timezone từ locale của công ty
        String timezone = LocaleUtil.toTimezone(company.getLocale());

        // Tạo user entity
        UserEntity employee = new UserEntity();
        employee.setEmail(request.getEmail());
        employee.setPassword(passwordEncoder.encode(temporaryPassword));
        employee.setRole(request.getRole());
        employee.setStatus(UserStatus.ACTIVE);
        employee.setLanguage(request.getLanguage());
        employee.setLocale(timezone);
        employee.setCompanyId(companyId);

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

        // Tạo mã nhân viên duy nhất từ companyId và ngày sinh
        String employeeCode = EmployeeCodeGenerator.generateUnique(companyId, request.getDateOfBirth(), userRepository);
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
    public UserResponse updateCompanyEmployee(Long companyId, Long employeeId, UpdateUserProfileRequest request) {
        UserEntity employee = findEmployeeByIdAndCompany(employeeId, companyId);

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
    public String uploadEmployeeAvatar(Long companyId, Long employeeId, MultipartFile file) {
        UserEntity employee = findEmployeeByIdAndCompany(employeeId, companyId);

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

    // ==================== Private helper methods ====================

    /**
     * Kiểm tra công ty tồn tại
     */
    private void validateCompanyExists(Long companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw NotFoundException.company(companyId);
        }
    }

    /**
     * Kiểm tra role hợp lệ cho nhân viên công ty
     */
    private void validateCompanyRole(UserRole role) {
        if (!ALLOWED_COMPANY_ROLES.contains(role)) {
            throw BadRequestException.invalidRole(role.name());
        }
    }

    /**
     * Tìm nhân viên theo ID và kiểm tra thuộc công ty
     */
    private UserEntity findEmployeeByIdAndCompany(Long employeeId, Long companyId) {
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));

        // Kiểm tra nhân viên thuộc công ty
        if (!companyId.equals(employee.getCompanyId())) {
            throw ForbiddenException.accessDenied();
        }

        return employee;
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
}
