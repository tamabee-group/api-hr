package com.tamabee.api_hr.service.core;

import com.tamabee.api_hr.entity.core.EmailVerificationEntity;
import com.tamabee.api_hr.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    
    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;
    
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
        System.out.println("=== VERIFICATION CODE ===");
        System.out.println("Email: " + email);
        System.out.println("Company: " + companyName);
        System.out.println("Code: " + code);
        System.out.println("Language: " + language);
        System.out.println("=========================");
        
        // Gửi email
        try {
            sendEmail(email, companyName, code, language != null ? language : "vi");
            System.out.println("Email sent successfully to: " + email);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
            // Không throw exception để vẫn lưu được vào DB
        }
    }
    
    @Transactional
    public boolean verifyCode(String email, String code) {
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
    
    private String generateSixDigitCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }
    
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
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    private String getSubject(String language) {
        return switch (language) {
            case "en" -> "Email Verification - Tamabee HR";
            case "ja" -> "メール認証 - Tamabee HR";
            default -> "Xác thực email - Tamabee HR";
        };
    }
    
    private String loadTemplate(String language, String companyName, String code) {
        try {
            String templatePath = "/templates/email/verification-" + language + ".html";
            var resource = getClass().getResourceAsStream(templatePath);
            if (resource == null) {
                templatePath = "/templates/email/verification-vi.html";
                resource = getClass().getResourceAsStream(templatePath);
            }
            String template = new String(resource.readAllBytes());
            return template
                .replace("{companyName}", companyName)
                .replace("{code}", code);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load email template", e);
        }
    }
}
