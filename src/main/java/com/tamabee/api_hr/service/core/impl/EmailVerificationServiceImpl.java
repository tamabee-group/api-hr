package com.tamabee.api_hr.service.core.impl;

import com.tamabee.api_hr.entity.core.EmailVerificationEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.InternalServerException;
import com.tamabee.api_hr.repository.core.EmailVerificationRepository;
import com.tamabee.api_hr.service.core.interfaces.IEmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Service xử lý xác thực email
 * Bao gồm gửi mã xác thực và kiểm tra mã
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements IEmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;

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
            helper.setText(htmlContent, true);

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
                    .replace("{code}", code);
        } catch (InternalServerException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException(
                    "Không thể tải template email",
                    ErrorCode.EMAIL_TEMPLATE_NOT_FOUND,
                    e);
        }
    }
}
