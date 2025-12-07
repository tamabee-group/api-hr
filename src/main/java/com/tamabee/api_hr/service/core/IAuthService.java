package com.tamabee.api_hr.service.core;

import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.model.request.LoginRequest;
import com.tamabee.api_hr.model.request.RegisterRequest;
import com.tamabee.api_hr.model.response.LoginResponse;

public interface IAuthService {
    
    /**
     * Đăng ký tài khoản company mới
     * Tạo Company, Wallet, User (ADMIN_COMPANY), xử lý referral
     */
    void register(RegisterRequest request);
    
    /**
     * Đăng nhập bằng email và password
     * Trả về JWT token và thông tin user
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * Đặt lại mật khẩu cho user
     */
    void resetPassword(String email, String newPassword);
    
    /**
     * Kiểm tra email có tồn tại trong hệ thống
     */
    void validateEmailExists(String email);
    
    /**
     * Kiểm tra email chưa tồn tại trong hệ thống
     */
    void validateEmailNotExists(String email);
    
    /**
     * Kiểm tra tên công ty chưa tồn tại trong hệ thống
     */
    void validateCompanyNameNotExists(String companyName);
    
    /**
     * Lấy access token mới từ refresh token
     */
    LoginResponse refreshToken(String refreshToken);
    
    /**
     * Lấy thông tin user hiện tại từ JWT token
     */
    UserResponse getCurrentUser();
}
