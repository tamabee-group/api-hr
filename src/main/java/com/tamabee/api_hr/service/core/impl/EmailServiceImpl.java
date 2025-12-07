package com.tamabee.api_hr.service.core.impl;

import com.tamabee.api_hr.service.core.IEmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {
    
    private final JavaMailSender mailSender;
    
    @Override
    public void sendTemporaryPassword(String email, String employeeCode, String temporaryPassword, String language) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(email);
            helper.setSubject("Tamabee HR Account");
            
            String template = loadTemplate(language);
            String content = template
                .replace("{employeeCode}", employeeCode)
                .replace("{email}", email)
                .replace("{temporaryPassword}", temporaryPassword);
            
            helper.setText(content, true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email mật khẩu tạm thời đến: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email đến {}: {}", email, e.getMessage());
        }
    }
    
    private String loadTemplate(String language) {
        try {
            var resource = getClass().getResourceAsStream("/templates/email/" + language + "/temporary-password.html");
            if (resource == null) {
                resource = getClass().getResourceAsStream("/templates/email/en/temporary-password.html");
            }
            return new String(resource.readAllBytes());
        } catch (Exception e) {
            log.error("Lỗi khi đọc template email: {}", e.getMessage());
            return "<p>Employee Code: {employeeCode}</p><p>Email: {email}</p><p>Temporary Password: {temporaryPassword}</p>";
        }
    }
}
