package com.tamabee.api_hr.service.core.interfaces;

public interface IEmailVerificationService {
    
    /**
     * Gửi mã xác thực 6 số đến email (dùng cho đăng ký)
     */
    void sendVerificationCode(String email, String companyName, String language);
    
    /**
     * Gửi link reset password đến email (dùng cho forgot password)
     */
    void sendPasswordResetLink(String email, String userName, String language, String tenantDomain);
    
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
    
    /**
     * Xác thực token reset password và trả về email
     * @return email nếu token hợp lệ, null nếu không
     */
    String verifyResetToken(String token);
    
    /**
     * Xác thực token reset password, đánh dấu đã sử dụng và trả về email
     * @return email nếu token hợp lệ, null nếu không
     */
    String verifyResetTokenAndMarkUsed(String token);
}
