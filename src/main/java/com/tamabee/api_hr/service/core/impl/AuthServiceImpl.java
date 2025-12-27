package com.tamabee.api_hr.service.core.impl;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.exception.UnauthorizedException;
import com.tamabee.api_hr.mapper.core.CompanyMapper;
import com.tamabee.api_hr.mapper.core.UserMapper;
import com.tamabee.api_hr.mapper.core.WalletMapper;
import com.tamabee.api_hr.model.request.LoginRequest;
import com.tamabee.api_hr.model.request.RegisterRequest;
import com.tamabee.api_hr.model.response.LoginResponse;
import com.tamabee.api_hr.util.EmployeeCodeGenerator;
import com.tamabee.api_hr.util.JwtUtil;
import com.tamabee.api_hr.util.ReferralCodeGenerator;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.EmailVerificationRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WalletRepository;
import com.tamabee.api_hr.service.core.IAuthService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final WalletRepository walletRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final WalletMapper walletMapper;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        validateEmailNotExists(request.getEmail());
        validateEmailVerified(request.getEmail());
        validateCompanyNameNotExists(request.getCompanyName());
        validateReferralCode(request.getReferralCode());

        CompanyEntity company = createCompany(request);
        createWallet(company.getId());
        UserEntity user = createUser(request, company.getId());

        // Cập nhật owner cho company
        company.setOwner(user);
        companyRepository.save(company);
    }

    private void validateEmailVerified(String email) {
        boolean isVerified = emailVerificationRepository.existsByEmailAndUsedTrue(email);
        if (!isVerified) {
            throw new BadRequestException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    /**
     * Kiểm tra mã giới thiệu có hợp lệ không
     * Nếu có nhập mã giới thiệu thì phải tồn tại trong hệ thống
     */
    private void validateReferralCode(String referralCode) {
        if (referralCode != null && !referralCode.isEmpty()) {
            boolean exists = userRepository.existsByProfileReferralCode(referralCode);
            if (!exists) {
                throw new BadRequestException(ErrorCode.INVALID_REFERRAL_CODE);
            }
        }
    }

    private CompanyEntity createCompany(RegisterRequest request) {
        CompanyEntity company = companyMapper.toEntity(request);
        return companyRepository.save(company);
    }

    private void createWallet(Long companyId) {
        WalletEntity wallet = walletMapper.createForCompany(companyId);
        walletRepository.save(wallet);
    }

    private UserEntity createUser(RegisterRequest request, Long companyId) {
        UserEntity user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.ADMIN_COMPANY);
        user.setStatus(UserStatus.ACTIVE);
        user.setCompanyId(companyId);

        // Tạo mã nhân viên duy nhất từ companyId và ngày sinh
        String employeeCode = EmployeeCodeGenerator.generateUnique(companyId, null, userRepository);
        user.setEmployeeCode(employeeCode);

        // Tạo profile với mã giới thiệu
        UserProfileEntity profile = new UserProfileEntity();
        String referralCode;
        do {
            referralCode = ReferralCodeGenerator.generate();
        } while (userRepository.existsByProfileReferralCode(referralCode));
        profile.setReferralCode(referralCode);
        profile.setUser(user);
        user.setProfile(profile);

        user.calculateProfileCompleteness();
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Tìm user bằng email hoặc mã nhân viên
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .or(() -> userRepository.findByEmployeeCode(request.getEmail()))
                .orElseThrow(UnauthorizedException::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw UnauthorizedException.invalidCredentials();
        }

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getCompanyId());

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(),
                user.getEmail());

        UserResponse userResponse = userMapper.toResponse(user);

        return new LoginResponse(accessToken, refreshToken, userResponse);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> NotFoundException.user(email));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateEmailExists(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw NotFoundException.email(email);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw ConflictException.emailExists(email);
        }
        if (companyRepository.existsByEmail(email)) {
            throw ConflictException.emailExists(email);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCompanyNameNotExists(String companyName) {
        if (companyRepository.existsByName(companyName)) {
            throw ConflictException.companyNameExists(companyName);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String refreshToken) {
        var claims = jwtUtil.validateToken(refreshToken);
        if (claims == null) {
            throw UnauthorizedException.invalidRefreshToken();
        }

        String email = (String) claims.get("sub");
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> NotFoundException.user(email));

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getCompanyId());

        UserResponse userResponse = userMapper.toResponse(user);

        return new LoginResponse(newAccessToken, refreshToken, userResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw UnauthorizedException.notAuthenticated();
        }

        String email = authentication.getName();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> NotFoundException.user(email));
        return userMapper.toResponse(user);
    }
}
