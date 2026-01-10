package com.tamabee.api_hr.service.core.interfaces;

public interface IEmailVerificationService {
    
    /**
     * Gửi mã xác thực 6 số đến email
     */
    void sendVerificationCode(String email, String companyName, String language);
    
    /**
     * Xác thực mã code
     * @return true nếu mã hợp lệ, false nếu không
     */
    boolean verifyCode(String email, String code);
    
    /**
     * Xác thực mã code và đánh dấu là đã sử dụng
     * @return true nếu mã hợp lệ, false nếu không
     */
    boolean verifyAndMarkUsed(String email, String code);
}
