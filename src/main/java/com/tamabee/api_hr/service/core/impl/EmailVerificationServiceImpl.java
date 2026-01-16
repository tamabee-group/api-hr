package com.tamabee.api_hr.service.core.impl;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.entity.core.EmailVerificationEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.InternalServerException;
import com.tamabee.api_hr.repository.core.EmailVerificationRepository;
import com.tamabee.api_hr.service.core.interfaces.IEmailVerificationService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý xác thực email
 * Bao gồm gửi mã xác thực và kiểm tra mã
 */
@Slf4j
@Service
public class EmailVerificationServiceImpl implements IEmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;
    private final String frontendUrl;
    
    // Logo URLs từ GitHub raw
    private static final String LOGO_URL = "https://raw.githubusercontent.com/tamabee-group/api-hr/main/src/main/resources/templates/images/logo.png";
    private static final String LOGO_TEXT_URL = "https://raw.githubusercontent.com/tamabee-group/api-hr/main/src/main/resources/templates/images/logo-text-light.png";

    public EmailVerificationServiceImpl(
            EmailVerificationRepository emailVerificationRepository,
            JavaMailSender mailSender,
            @Value("${app.frontend-url:https://tamabee.vn}") String frontendUrl) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
    }
    
    /**
     * Thay thế CID references bằng URL trong template
     * Lưu ý: replace logoText trước logo để tránh conflict
     */
    private String replaceLogoWithUrl(String content) {
        content = content.replace("cid:logoText", LOGO_TEXT_URL);
        content = content.replace("cid:logo", LOGO_URL);
        return content;
    }

    /**
     * Gửi mã xác thực đến email
     * 
     * @param email       địa chỉ email nhận mã
     * @param companyName tên công ty
     * @param language    ngôn ngữ template email (vi, en, ja)
     */
    @Override
    @Transactional
    public void sendVerificationCode(String email, String companyName, String language) {
        // Xóa tất cả mã cũ của email này
        emailVerificationRepository.deleteByEmail(email);

        String code = generateSixDigitCode();

        EmailVerificationEntity verification = new EmailVerificationEntity();
        verification.setEmail(email);
        verification.setCode(code);
        verification.setCompanyName(companyName);
        verification.setCreatedAt(LocalDateTime.now());
        verification.setExpiredAt(LocalDateTime.now().plusMinutes(10));
        verification.setUsed(false);

        emailVerificationRepository.save(verification);

        // Log code để debug
        log.info("=== VERIFICATION CODE ===");
        log.info("Email: {}", email);
        log.info("Company: {}", companyName);
        log.info("Code: {}", code);
        log.info("Language: {}", language);
        log.info("=========================");

        // Gửi email
        try {
            sendEmail(email, companyName, code, language != null ? language : "vi");
            log.info("Email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra mã xác thực có hợp lệ không
     * 
     * @param email địa chỉ email
     * @param code  mã xác thực
     * @return true nếu mã hợp lệ và chưa hết hạn
     */
    @Override
    @Transactional(readOnly = true)
    public boolean verifyCode(String email, String code) {
        return emailVerificationRepository
                .findValidCode(email, code, LocalDateTime.now())
                .isPresent();
    }

    /**
     * Kiểm tra và đánh dấu mã đã sử dụng
     * 
     * @param email địa chỉ email
     * @param code  mã xác thực
     * @return true nếu mã hợp lệ và đã được đánh dấu sử dụng
     */
    @Override
    @Transactional
    public boolean verifyAndMarkUsed(String email, String code) {
        return emailVerificationRepository
                .findValidCode(email, code, LocalDateTime.now())
                .map(verification -> {
                    verification.setUsed(true);
                    verification.setUpdatedAt(LocalDateTime.now());
                    emailVerificationRepository.save(verification);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Gửi link reset password đến email
     */
    @Override
    @Transactional
    public void sendPasswordResetLink(String email, String userName, String language, String tenantDomain) {
        // Xóa tất cả token cũ của email này
        emailVerificationRepository.deleteByEmail(email);

        // Tạo token UUID
        String token = UUID.randomUUID().toString();

        EmailVerificationEntity verification = new EmailVerificationEntity();
        verification.setEmail(email);
        verification.setCode(token); // Dùng field code để lưu token
        verification.setCompanyName(""); // Không cần company name cho reset password
        verification.setCreatedAt(LocalDateTime.now());
        verification.setExpiredAt(LocalDateTime.now().plusMinutes(30)); // 30 phút
        verification.setUsed(false);

        emailVerificationRepository.save(verification);

        // Log để debug
        log.info("=== PASSWORD RESET TOKEN ===");
        log.info("Email: {}", email);
        log.info("Token: {}", token);
        log.info("Language: {}", language);
        log.info("============================");

        // Gửi email
        try {
            sendPasswordResetEmail(email, userName, token, language != null ? language : "vi", tenantDomain);
            log.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage(), e);
        }
    }

    /**
     * Xác thực token reset password và trả về email
     */
    @Override
    @Transactional(readOnly = true)
    public String verifyResetToken(String token) {
        return emailVerificationRepository
                .findValidCode(token, LocalDateTime.now())
                .map(EmailVerificationEntity::getEmail)
                .orElse(null);
    }

    /**
     * Xác thực token reset password, đánh dấu đã sử dụng và trả về email
     */
    @Override
    @Transactional
    public String verifyResetTokenAndMarkUsed(String token) {
        return emailVerificationRepository
                .findValidCode(token, LocalDateTime.now())
                .map(verification -> {
                    verification.setUsed(true);
                    verification.setUpdatedAt(LocalDateTime.now());
                    emailVerificationRepository.save(verification);
                    return verification.getEmail();
                })
                .orElse(null);
    }

    /**
     * Tạo mã xác thực 6 chữ số ngẫu nhiên
     */
    private String generateSixDigitCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    /**
     * Gửi email chứa mã xác thực
     */
    private void sendEmail(String to, String companyName, String code, String language) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(to);

            String subject = getSubject(language);
            helper.setSubject(subject);

            String htmlContent = loadTemplate(language, companyName, code);
            helper.setText(replaceLogoWithUrl(htmlContent), true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw InternalServerException.emailSendFailed(e);
        }
    }

    /**
     * Lấy tiêu đề email theo ngôn ngữ
     */
    private String getSubject(String language) {
        return switch (language) {
            case "en" -> "Email Verification - Tamabee HR";
            case "ja" -> "メール認証 - Tamabee HR";
            default -> "Xác thực email - Tamabee HR";
        };
    }

    /**
     * Tải template email theo ngôn ngữ
     */
    private String loadTemplate(String language, String companyName, String code) {
        try {
            String templatePath = "/templates/email/" + language + "/email-verification.html";
            var resource = getClass().getResourceAsStream(templatePath);
            if (resource == null) {
                // Fallback về tiếng Việt nếu không tìm thấy template
                templatePath = "/templates/email/vi/email-verification.html";
                resource = getClass().getResourceAsStream(templatePath);
            }
            if (resource == null) {
                throw new InternalServerException(
                        "Không tìm thấy template email: " + templatePath,
                        ErrorCode.EMAIL_TEMPLATE_NOT_FOUND);
            }
            String template = new String(resource.readAllBytes());
            
            return template
                    .replace("{companyName}", companyName)
                    .replace("{code}", code)
                    .replace("{year}", String.valueOf(Year.now().getValue()));
        } catch (InternalServerException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException(
                    "Không thể tải template email",
                    ErrorCode.EMAIL_TEMPLATE_NOT_FOUND,
                    e);
        }
    }

    /**
     * Gửi email reset password với link
     */
    private void sendPasswordResetEmail(String to, String userName, String token, String language, String tenantDomain) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(to);

            String subject = getPasswordResetSubject(language);
            helper.setSubject(subject);

            String htmlContent = loadPasswordResetTemplate(language, userName, token, tenantDomain);
            helper.setText(replaceLogoWithUrl(htmlContent), true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw InternalServerException.emailSendFailed(e);
        }
    }

    /**
     * Lấy tiêu đề email reset password theo ngôn ngữ
     */
    private String getPasswordResetSubject(String language) {
        return switch (language) {
            case "en" -> "Reset your password - Tamabee HR";
            case "ja" -> "パスワードをリセット - Tamabee HR";
            default -> "Đặt lại mật khẩu - Tamabee HR";
        };
    }

    /**
     * Tải template email reset password theo ngôn ngữ
     */
    private String loadPasswordResetTemplate(String language, String userName, String token, String tenantDomain) {
        try {
            String templatePath = "/templates/email/" + language + "/password-reset.html";
            var resource = getClass().getResourceAsStream(templatePath);
            if (resource == null) {
                // Fallback về tiếng Việt nếu không tìm thấy template
                templatePath = "/templates/email/vi/password-reset.html";
                resource = getClass().getResourceAsStream(templatePath);
            }
            if (resource == null) {
                throw new InternalServerException(
                        "Không tìm thấy template email: " + templatePath,
                        ErrorCode.EMAIL_TEMPLATE_NOT_FOUND);
            }
            String template = new String(resource.readAllBytes());
            
            // Build reset link với tenant domain
            String resetLink = buildResetLink(token, language, tenantDomain);
            
            return template
                    .replace("{userName}", userName != null ? userName : "")
                    .replace("{resetLink}", resetLink)
                    .replace("{year}", String.valueOf(Year.now().getValue()));
        } catch (InternalServerException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException(
                    "Không thể tải template email",
                    ErrorCode.EMAIL_TEMPLATE_NOT_FOUND,
                    e);
        }
    }

    /**
     * Build link reset password
     * Format: https://{tenantDomain}.tamabee.vn/{language}/reset-password?token={token}
     */
    private String buildResetLink(String token, String language, String tenantDomain) {
        // Nếu là tamabee thì dùng domain chính
        String baseUrl;
        if ("tamabee".equals(tenantDomain)) {
            baseUrl = frontendUrl;
        } else {
            // Thay thế domain chính bằng tenant subdomain
            baseUrl = frontendUrl.replace("://", "://" + tenantDomain + ".");
        }
        return baseUrl + "/" + language + "/reset-password?token=" + token;
    }
}
