package com.tamabee.api_hr.service.core;

import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.model.request.RegisterRequest;
import com.tamabee.api_hr.repository.EmailVerificationRepository;
import com.tamabee.api_hr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public void register(RegisterRequest request) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Kiểm tra email đã được verify
        var usedVerifications = emailVerificationRepository.findByEmailAndUsedTrue(request.getEmail());
        if (usedVerifications.isEmpty()) {
            throw new RuntimeException("Email not verified");
        }
        
        // Tạo user mới
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.ADMIN_COMPANY);
        user.setStatus(UserStatus.ACTIVE);
        user.setLocale(request.getLocale());
        user.setLanguage(request.getLanguage());
        
        userRepository.save(user);
        
        // TODO: Tạo Company, Wallet, xử lý referral code
        // Note: Verification đã được set used=true khi verify email
    }
}
