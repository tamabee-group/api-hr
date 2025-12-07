package com.tamabee.api_hr.service.core.impl;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.mapper.core.CompanyMapper;
import com.tamabee.api_hr.mapper.core.UserMapper;
import com.tamabee.api_hr.mapper.core.WalletMapper;
import com.tamabee.api_hr.model.request.LoginRequest;
import com.tamabee.api_hr.model.request.RegisterRequest;
import com.tamabee.api_hr.model.response.LoginResponse;
import com.tamabee.api_hr.util.JwtUtil;
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
        
        CompanyEntity company = createCompany(request);
        createWallet(company.getId());
        createUser(request, company.getId());
    }
    
    private void validateEmailVerified(String email) {
        boolean isVerified = emailVerificationRepository.existsByEmailAndUsedTrue(email);
        if (!isVerified) {
            throw new RuntimeException("Email not verified");
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
    
    private void createUser(RegisterRequest request, Long companyId) {
        UserEntity user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.ADMIN_COMPANY);
        user.setStatus(UserStatus.ACTIVE);
        user.setCompanyId(companyId);
        userRepository.save(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Tìm user bằng email hoặc mã nhân viên
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .or(() -> userRepository.findByEmployeeCode(request.getEmail()))
                .orElseThrow(() -> new RuntimeException("Thông tin đăng nhập không đúng"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Thông tin đăng nhập không đúng");
        }
        
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getCompanyId()
        );
        
        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(),
                user.getEmail()
        );
        
        UserResponse userResponse = userMapper.toResponse(user);
        
        return new LoginResponse(accessToken, refreshToken, userResponse);
    }
    
    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public void validateEmailExists(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }
        if (companyRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public void validateCompanyNameNotExists(String companyName) {
        if (companyRepository.existsByName(companyName)) {
            throw new RuntimeException("Đã có công ty sử dụng tên này");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String refreshToken) {
        var claims = jwtUtil.validateToken(refreshToken);
        if (claims == null) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        String email = (String) claims.get("sub");
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getCompanyId()
        );
        
        UserResponse userResponse = userMapper.toResponse(user);
        
        return new LoginResponse(newAccessToken, refreshToken, userResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        String email = authentication.getName();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toResponse(user);
    }
}
