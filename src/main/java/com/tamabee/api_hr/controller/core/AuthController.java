package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.model.request.RegisterRequest;
import com.tamabee.api_hr.model.request.SendVerificationRequest;
import com.tamabee.api_hr.model.request.VerifyEmailRequest;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.core.AuthService;
import com.tamabee.api_hr.service.core.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;
    
    @PostMapping("/send-verification")
    public BaseResponse<Void> sendVerification(@Valid @RequestBody SendVerificationRequest request) {
        emailVerificationService.sendVerificationCode(request.getEmail(), request.getCompanyName(), request.getLanguage());
        return BaseResponse.success(null, "Verification code sent successfully");
    }
    
    @PostMapping("/verify-email")
    public BaseResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        boolean isValid = emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        
        if (isValid) {
            return BaseResponse.success(null, "Email verified successfully");
        } else {
            return BaseResponse.error("Invalid or expired verification code", "INVALID_CODE");
        }
    }
    
    @PostMapping("/register")
    public BaseResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return BaseResponse.success(null, "Registration successful");
        } catch (RuntimeException e) {
            return BaseResponse.error(e.getMessage(), "REGISTRATION_FAILED");
        }
    }
}
