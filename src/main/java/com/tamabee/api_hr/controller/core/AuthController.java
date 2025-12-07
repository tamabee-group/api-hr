package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.model.request.*;
import com.tamabee.api_hr.model.response.LoginResponse;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.core.IAuthService;
import com.tamabee.api_hr.service.core.IEmailVerificationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CookieValue;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final IEmailVerificationService emailVerificationService;
    private final IAuthService authService;
    
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;
    
    @PostMapping("/send-verification")
    public BaseResponse<Void> sendVerification(@Valid @RequestBody SendVerificationRequest request) {
        try {
            authService.validateEmailNotExists(request.getEmail());
            authService.validateCompanyNameNotExists(request.getCompanyName());
            emailVerificationService.sendVerificationCode(request.getEmail(), request.getCompanyName(), request.getLanguage());
            return BaseResponse.success(null, "Verification code sent successfully");
        } catch (RuntimeException e) {
            return BaseResponse.error(e.getMessage(), "SEND_VERIFICATION_FAILED");
        }
    }
    
    @PostMapping("/verify-email")
    public BaseResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        boolean isValid = emailVerificationService.verifyAndMarkUsed(request.getEmail(), request.getCode());
        
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
    
    @PostMapping("/forgot-password")
    public BaseResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.validateEmailExists(request.getEmail());
            emailVerificationService.sendVerificationCode(request.getEmail(), "", "vi");
            return BaseResponse.success(null, "Reset code sent successfully");
        } catch (RuntimeException e) {
            return BaseResponse.error(e.getMessage(), "SEND_CODE_FAILED");
        }
    }
    
    @PostMapping("/reset-password")
    public BaseResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            boolean isValid = emailVerificationService.verifyAndMarkUsed(request.getEmail(), request.getCode());
            if (!isValid) {
                return BaseResponse.error("Invalid or expired code", "INVALID_CODE");
            }
            
            authService.resetPassword(request.getEmail(), request.getNewPassword());
            return BaseResponse.success(null, "Password reset successfully");
        } catch (RuntimeException e) {
            return BaseResponse.error(e.getMessage(), "RESET_PASSWORD_FAILED");
        }
    }
    
    @PostMapping("/refresh-token")
    public BaseResponse<UserResponse> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        try {
            if (refreshToken == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return BaseResponse.error("Refresh token not found", "NO_REFRESH_TOKEN");
            }
            
            LoginResponse loginResponse = authService.refreshToken(refreshToken);
            
            // Set new access token cookie
            Cookie accessCookie = new Cookie("accessToken", loginResponse.getAccessToken());
            accessCookie.setPath("/");
            accessCookie.setMaxAge((int) (accessTokenExpiration / 1000));
            accessCookie.setHttpOnly(false);
            response.addCookie(accessCookie);
            
            return BaseResponse.success(loginResponse.getUser(), "Token refreshed successfully");
        } catch (RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return BaseResponse.error(e.getMessage(), "REFRESH_FAILED");
        }
    }
    
    @PostMapping("/logout")
    public BaseResponse<Void> logout(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie("accessToken", "");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        response.addCookie(accessTokenCookie);
        
        Cookie refreshTokenCookie = new Cookie("refreshToken", "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);
        
        return BaseResponse.success(null, "Logout successful");
    }
    
    @GetMapping("/me")
    public BaseResponse<UserResponse> getCurrentUser() {
        try {
            UserResponse user = authService.getCurrentUser();
            return BaseResponse.success(user, "User retrieved successfully");
        } catch (RuntimeException e) {
            return BaseResponse.error(e.getMessage(), "GET_USER_FAILED");
        }
    }
    
    @PostMapping("/login")
    public BaseResponse<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        try {
            LoginResponse loginResponse = authService.login(request);
            
            // Set cookies
            // Set cookies using Cookie objects
            Cookie accessCookie = new Cookie("accessToken", loginResponse.getAccessToken());
            accessCookie.setPath("/");
            accessCookie.setMaxAge((int) (accessTokenExpiration / 1000));
            accessCookie.setHttpOnly(false);
            response.addCookie(accessCookie);
            
            Cookie refreshCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge((int) (refreshTokenExpiration / 1000));
            refreshCookie.setHttpOnly(true);
            response.addCookie(refreshCookie);
            
            return BaseResponse.success(loginResponse.getUser(), "Login successful");
        } catch (RuntimeException e) {
            return BaseResponse.error(e.getMessage(), "LOGIN_FAILED");
        }
    }
}
