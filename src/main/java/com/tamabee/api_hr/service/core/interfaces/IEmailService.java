package com.tamabee.api_hr.service.core.interfaces;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IEmailService {
    void sendTemporaryPassword(String email, String employeeCode, String temporaryPassword, String language);

    /**
     * Gửi email chào mừng admin company sau khi đăng ký thành công
     *
     * @param email          email của admin
     * @param ownerName      tên chủ công ty
     * @param companyName    tên công ty
     * @param tenantDomain   domain của tenant
     * @param freeTrialEnd   ngày hết hạn dùng thử
     * @param language       ngôn ngữ (vi, en, ja)
     */
    void sendWelcomeCompany(String email, String ownerName, String companyName, String tenantDomain,
            LocalDateTime freeTrialEnd, String language);

    /**
     * Gửi email thông báo cho Tamabee admin khi có công ty mới đăng ký
     *
     * @param companyName    tên công ty
     * @param ownerName      tên chủ công ty
     * @param email          email công ty
     * @param tenantDomain   domain của tenant
     * @param referralCode   mã giới thiệu (nếu có)
     * @param referrerName   tên người giới thiệu (nếu có)
     */
    void sendNewCompanyNotification(String companyName, String ownerName, String email, String tenantDomain,
            String referralCode, String referrerName);

    /**
     * Gửi email thông báo hoa hồng cho người giới thiệu
     *
     * @param referrerEmail  email người giới thiệu
     * @param referrerName   tên người giới thiệu
     * @param newCompanyName tên công ty mới đăng ký
     * @param language       ngôn ngữ (vi, en, ja)
     */
    void sendReferralCommissionNotification(String referrerEmail, String referrerName, String newCompanyName,
            String language);

    /**
     * Gửi email thông báo nạp tiền thành công
     *
     * @param email       email của company
     * @param companyName tên company
     * @param amount      số tiền nạp
     * @param balance     số dư sau khi nạp
     * @param language    ngôn ngữ (vi, en, ja)
     */
    void sendDepositApproved(String email, String companyName, BigDecimal amount, BigDecimal balance, String language);

    /**
     * Gửi email thông báo trừ tiền subscription thành công
     *
     * @param email       email của company
     * @param companyName tên company
     * @param planName    tên gói dịch vụ
     * @param amount      số tiền trừ
     * @param balance     số dư còn lại
     * @param language    ngôn ngữ (vi, en, ja)
     */
    void sendBillingNotification(String email, String companyName, String planName, BigDecimal amount,
            BigDecimal balance, String language);

    /**
     * Gửi email thông báo số dư không đủ để billing
     *
     * @param email       email của company
     * @param companyName tên company
     * @param planName    tên gói dịch vụ
     * @param amount      số tiền cần thanh toán
     * @param balance     số dư hiện tại
     * @param language    ngôn ngữ (vi, en, ja)
     */
    void sendInsufficientBalance(String email, String companyName, String planName, BigDecimal amount,
            BigDecimal balance, String language);

    /**
     * Gửi email thông báo cho Tamabee admin khi có yêu cầu nạp tiền mới
     *
     * @param companyName      tên công ty
     * @param amount           số tiền yêu cầu nạp
     * @param transferProofUrl URL ảnh chứng từ chuyển khoản
     * @param requesterName    tên người gửi yêu cầu
     * @param requesterEmail   email người gửi yêu cầu
     */
    void sendNewDepositNotification(String companyName, BigDecimal amount, String transferProofUrl,
            String requesterName, String requesterEmail);

    /**
     * Gửi email báo cáo kết quả cleanup companies cho admin
     *
     * @param deletedCompanies danh sách tên các company đã bị xóa
     * @param retentionDays    số ngày retention
     */
    void sendCleanupReport(List<String> deletedCompanies, int retentionDays);

    /**
     * Gửi email thông báo cho Tamabee admin khi có feedback mới từ khách hàng
     *
     * @param feedbackType   loại feedback
     * @param title          tiêu đề feedback
     * @param userName       tên người gửi
     * @param userEmail      email người gửi
     * @param companyName    tên công ty
     */
    void sendFeedbackNotification(String feedbackType, String title, String userName,
            String userEmail, String companyName);

    /**
     * Gửi email thông báo tài khoản đã được kích hoạt lại sau khi nạp tiền
     *
     * @param email       email của company
     * @param companyName tên company
     * @param balance     số dư hiện tại
     * @param language    ngôn ngữ (vi, en, ja)
     */
    void sendReactivationNotification(String email, String companyName, BigDecimal balance, String language);

    /**
     * Gửi email thông báo lương cho nhân viên
     *
     * @param email            email nhân viên
     * @param employeeName     tên nhân viên
     * @param period           kỳ lương (ví dụ: "01/2025")
     * @param baseSalary       lương cơ bản
     * @param totalOvertime    tổng tăng ca
     * @param totalAllowances  tổng phụ cấp
     * @param totalDeductions  tổng khấu trừ
     * @param netSalary        lương thực nhận
     * @param paymentDate      ngày thanh toán
     * @param language         ngôn ngữ (vi, en, ja)
     */
    void sendPayrollNotification(String email, String employeeName, String period, String baseSalary,
            String totalOvertime, String totalAllowances, String totalDeductions, String netSalary,
            String paymentDate, String language);

    /**
     * Gửi email thông báo cho admin khi có kỳ lương gửi duyệt
     *
     * @param email          email admin
     * @param recipientName  tên admin
     * @param submitterName  tên người gửi duyệt
     * @param period         kỳ lương (ví dụ: "01/2026")
     * @param totalEmployees số nhân viên
     * @param totalNetSalary tổng thực nhận (đã format)
     * @param language       ngôn ngữ (vi, en, ja)
     */
    void sendPayrollSubmittedNotification(String email, String recipientName, String submitterName,
            String period, String totalEmployees, String totalNetSalary, String language);

    /**
     * Gửi email thông báo cho người gửi duyệt khi kỳ lương được phê duyệt
     *
     * @param email          email người gửi duyệt
     * @param recipientName  tên người gửi duyệt
     * @param period         kỳ lương (ví dụ: "01/2026")
     * @param totalEmployees số nhân viên
     * @param totalNetSalary tổng thực nhận (đã format)
     * @param language       ngôn ngữ (vi, en, ja)
     */
    void sendPayrollApprovedNotification(String email, String recipientName,
            String period, String totalEmployees, String totalNetSalary, String language);

    /**
     * Gửi email thông báo cho người gửi duyệt khi kỳ lương bị từ chối
     *
     * @param email           email người gửi duyệt
     * @param recipientName   tên người gửi duyệt
     * @param period          kỳ lương (ví dụ: "01/2026")
     * @param totalEmployees  số nhân viên
     * @param totalNetSalary  tổng thực nhận (đã format)
     * @param rejectionReason lý do từ chối
     * @param language        ngôn ngữ (vi, en, ja)
     */
    void sendPayrollRejectedNotification(String email, String recipientName,
            String period, String totalEmployees, String totalNetSalary, String rejectionReason, String language);

    /**
     * Gửi email thông báo lịch phân ca cho nhân viên
     *
     * @param email         email nhân viên
     * @param employeeName  tên nhân viên
     * @param weekInfo      thông tin tuần (ví dụ: "Tuần 25: 16/06 - 22/06/2025")
     * @param message       tin nhắn tùy chỉnh từ manager (nullable)
     * @param language      ngôn ngữ (vi, en, ja)
     */
    void sendShiftScheduleNotification(String email, String employeeName, String weekInfo,
            String message, String language);
}
