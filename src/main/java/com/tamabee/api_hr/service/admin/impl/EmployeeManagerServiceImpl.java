package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.dto.request.CreateTamabeeUserRequest;
import com.tamabee.api_hr.dto.request.UpdateUserProfileRequest;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.core.UserMapper;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.admin.IEmployeeManagerService;
import com.tamabee.api_hr.service.core.IEmailService;
import com.tamabee.api_hr.service.core.IUploadService;
import com.tamabee.api_hr.util.EmployeeCodeGenerator;
import com.tamabee.api_hr.util.ReferralCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeManagerServiceImpl implements IEmployeeManagerService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;
    private final IUploadService uploadService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getTamabeeUsers(Pageable pageable) {
        Page<UserEntity> users = userRepository.findByCompanyId(0L, pageable);
        return users.map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getTamabeeUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.user(id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createTamabeeUser(CreateTamabeeUserRequest request) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ConflictException.emailExists(request.getEmail());
        }

        // Tạo mật khẩu tạm thời
        String temporaryPassword = UUID.randomUUID().toString().substring(0, 8);

        // Tạo user entity (chưa có employeeCode)
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ACTIVE);
        user.setLanguage(request.getLanguage());
        user.setLocale("Asia/Tokyo"); // Tamabee mặc định múi giờ Nhật Bản
        user.setCompanyId(0L); // Tamabee user companyId = 0

        // Tạo mã giới thiệu duy nhất
        String referralCode;
        do {
            referralCode = ReferralCodeGenerator.generate();
        } while (userRepository.existsByProfileReferralCode(referralCode));

        // Tạo user profile
        UserProfileEntity profile = new UserProfileEntity();
        profile.setName(request.getName());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setZipCode(request.getZipCode());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setReferralCode(referralCode);
        profile.setUser(user);

        user.setProfile(profile);

        // Đếm số nhân viên hiện tại của Tamabee để tạo mã
        long employeeCount = userRepository.countByCompanyId(0L);
        String employeeCode = EmployeeCodeGenerator.generate(0L, employeeCount + 1);
        user.setEmployeeCode(employeeCode);

        // Lưu vào database
        UserEntity savedUser = userRepository.save(user);

        // Gửi email mật khẩu tạm thời
        emailService.sendTemporaryPassword(savedUser.getEmail(), savedUser.getEmployeeCode(), temporaryPassword,
                savedUser.getLanguage());

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(Long id, UpdateUserProfileRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.user(id));

        // Cập nhật thông tin user
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getLanguage() != null) {
            user.setLanguage(request.getLanguage());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        // Cập nhật profile
        if (user.getProfile() == null) {
            user.setProfile(new UserProfileEntity());
            user.getProfile().setUser(user);
        }

        UserProfileEntity profile = user.getProfile();
        if (request.getName() != null)
            profile.setName(request.getName());
        if (request.getPhone() != null)
            profile.setPhone(request.getPhone());
        if (request.getZipCode() != null)
            profile.setZipCode(request.getZipCode());
        if (request.getAddress() != null)
            profile.setAddress(request.getAddress());
        if (request.getBankName() != null)
            profile.setBankName(request.getBankName());
        if (request.getBankAccount() != null)
            profile.setBankAccount(request.getBankAccount());
        if (request.getBankAccountName() != null)
            profile.setBankAccountName(request.getBankAccountName());
        if (request.getEmergencyContactName() != null)
            profile.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null)
            profile.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getEmergencyContactRelation() != null)
            profile.setEmergencyContactRelation(request.getEmergencyContactRelation());
        if (request.getEmergencyContactAddress() != null)
            profile.setEmergencyContactAddress(request.getEmergencyContactAddress());

        UserEntity savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public String uploadAvatar(Long id, MultipartFile file) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.user(id));

        // Xóa ảnh cũ nếu có
        if (user.getProfile() != null && user.getProfile().getAvatar() != null) {
            uploadService.deleteFile(user.getProfile().getAvatar());
        }

        // Upload file mới
        String avatarUrl = uploadService.uploadFile(file, "avatar", user.getEmployeeCode());

        // Cập nhật vào database
        if (user.getProfile() == null) {
            user.setProfile(new UserProfileEntity());
            user.getProfile().setUser(user);
        }
        user.getProfile().setAvatar(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }
}
