package com.tamabee.api_hr.controller.core;

import com.tamabee.api_hr.dto.response.DomainAvailabilityResponse;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.model.request.*;
import com.tamabee.api_hr.model.response.LoginResponse;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.service.core.IAuthService;
import com.tamabee.api_hr.service.core.IEmailVerificationService;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý authentication
 * Public API - không yêu cầu đăng nhập (trừ /me và /logout)
 */
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
    public ResponseEntity<BaseResponse<Void>> sendVerification(@Valid @RequestBody SendVerificationRequest request) {
        authService.validateEmailNotExists(request.getEmail());
        authService.validateCompanyNameNotExists(request.getCompanyName());
        emailVerificationService.sendVerificationCode(
                request.getEmail(),
                request.getCompanyName(),
                request.getLanguage());
        return ResponseEntity.ok(BaseResponse.success(null, "Mã xác thực đã được gửi"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<BaseResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        boolean isValid = emailVerificationService.verifyAndMarkUsed(request.getEmail(), request.getCode());
        if (!isValid) {
            throw new BadRequestException(ErrorCode.INVALID_CODE);
        }
        return ResponseEntity.ok(BaseResponse.success(null, "Xác thực email thành công"));
    }

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.created(null, "Đăng ký thành công"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.validateEmailExists(request.getEmail());
        emailVerificationService.sendVerificationCode(request.getEmail(), "", "vi");
        return ResponseEntity.ok(BaseResponse.success(null, "Mã đặt lại mật khẩu đã được gửi"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean isValid = emailVerificationService.verifyAndMarkUsed(request.getEmail(), request.getCode());
        if (!isValid) {
            throw new BadRequestException(ErrorCode.INVALID_CODE);
        }
        authService.resetPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(BaseResponse.success(null, "Đặt lại mật khẩu thành công"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<BaseResponse<UserResponse>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            throw new UnauthorizedException(ErrorCode.NO_REFRESH_TOKEN);
        }

        LoginResponse loginResponse = authService.refreshToken(refreshToken);

        // Set new access token cookie
        Cookie accessCookie = new Cookie("accessToken", loginResponse.getAccessToken());
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (accessTokenExpiration / 1000));
        accessCookie.setHttpOnly(false);
        response.addCookie(accessCookie);

        return ResponseEntity.ok(BaseResponse.success(loginResponse.getUser(), "Làm mới token thành công"));
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(HttpServletResponse response) {
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

        return ResponseEntity.ok(BaseResponse.success(null, "Đăng xuất thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserResponse>> getCurrentUser() {
        UserResponse user = authService.getCurrentUser();
        return ResponseEntity.ok(BaseResponse.success(user, "Lấy thông tin user thành công"));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(request);

        // Set cookies
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

        return ResponseEntity.ok(BaseResponse.success(loginResponse.getUser(), "Đăng nhập thành công"));
    }

    @GetMapping("/check-domain")
    public ResponseEntity<BaseResponse<DomainAvailabilityResponse>> checkDomain(
            @RequestParam String domain) {
        DomainAvailabilityResponse result = authService.checkDomainAvailability(domain);
        String message = result.isAvailable()
                ? "Tenant domain khả dụng"
                : "Tenant domain không khả dụng";
        return ResponseEntity.ok(BaseResponse.success(result, message));
    }
}
