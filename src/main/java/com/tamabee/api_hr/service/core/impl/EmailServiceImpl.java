package com.tamabee.api_hr.service.core.impl;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.tamabee.api_hr.service.core.IEmailService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

            String template = loadTemplate("temporary-password", language);
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

    @Override
    public void sendDepositApproved(String email, String companyName, BigDecimal amount, BigDecimal balance,
            String language) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(email);
            helper.setSubject(getSubject("deposit-approved", language));

            String template = loadTemplate("deposit-approved", language);
            String content = template
                    .replace("{companyName}", companyName)
                    .replace("{amount}", formatCurrency(amount))
                    .replace("{balance}", formatCurrency(balance))
                    .replace("{date}", formatDate(LocalDateTime.now(), language));

            helper.setText(content, true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email thông báo nạp tiền thành công đến: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email deposit-approved đến {}: {}", email, e.getMessage());
        }
    }

    @Override
    public void sendBillingNotification(String email, String companyName, String planName, BigDecimal amount,
            BigDecimal balance, String language) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(email);
            helper.setSubject(getSubject("billing-notification", language));

            String template = loadTemplate("billing-notification", language);
            String content = template
                    .replace("{companyName}", companyName)
                    .replace("{planName}", planName != null ? planName : "N/A")
                    .replace("{amount}", formatCurrency(amount))
                    .replace("{balance}", formatCurrency(balance))
                    .replace("{date}", formatDate(LocalDateTime.now(), language));

            helper.setText(content, true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email thông báo billing đến: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email billing-notification đến {}: {}", email, e.getMessage());
        }
    }

    @Override
    public void sendInsufficientBalance(String email, String companyName, String planName, BigDecimal amount,
            BigDecimal balance, String language) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(email);
            helper.setSubject(getSubject("insufficient-balance", language));

            String template = loadTemplate("insufficient-balance", language);
            String content = template
                    .replace("{companyName}", companyName)
                    .replace("{planName}", planName != null ? planName : "N/A")
                    .replace("{amount}", formatCurrency(amount))
                    .replace("{balance}", formatCurrency(balance));

            helper.setText(content, true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email thông báo insufficient balance đến: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email insufficient-balance đến {}: {}", email, e.getMessage());
        }
    }

    private String loadTemplate(String templateName, String language) {
        try {
            var resource = getClass()
                    .getResourceAsStream("/templates/email/" + language + "/" + templateName + ".html");
            if (resource == null) {
                resource = getClass().getResourceAsStream("/templates/email/en/" + templateName + ".html");
            }
            if (resource != null) {
                return new String(resource.readAllBytes());
            }
            return getDefaultTemplate(templateName);
        } catch (Exception e) {
            log.error("Lỗi khi đọc template email {}: {}", templateName, e.getMessage());
            return getDefaultTemplate(templateName);
        }
    }

    private String getDefaultTemplate(String templateName) {
        return switch (templateName) {
            case "temporary-password" ->
                "<p>Employee Code: {employeeCode}</p><p>Email: {email}</p><p>Temporary Password: {temporaryPassword}</p>";
            case "deposit-approved" ->
                "<p>Dear {companyName},</p><p>Your deposit of {amount} has been approved. Current balance: {balance}</p>";
            case "billing-notification" ->
                "<p>Dear {companyName},</p><p>Your subscription ({planName}) has been billed: {amount}. Current balance: {balance}</p>";
            case "insufficient-balance" ->
                "<p>Dear {companyName},</p><p>Insufficient balance for subscription ({planName}). Required: {amount}, Current: {balance}</p>";
            default -> "<p>Notification from Tamabee HR</p>";
        };
    }

    private String getSubject(String templateName, String language) {
        return switch (templateName) {
            case "deposit-approved" -> switch (language) {
                case "vi" -> "Tamabee HR - Nạp tiền thành công";
                case "ja" -> "Tamabee HR - 入金完了";
                default -> "Tamabee HR - Deposit Approved";
            };
            case "billing-notification" -> switch (language) {
                case "vi" -> "Tamabee HR - Thông báo thanh toán";
                case "ja" -> "Tamabee HR - 請求通知";
                default -> "Tamabee HR - Billing Notification";
            };
            case "insufficient-balance" -> switch (language) {
                case "vi" -> "Tamabee HR - Cảnh báo số dư không đủ";
                case "ja" -> "Tamabee HR - 残高不足警告";
                default -> "Tamabee HR - Insufficient Balance Warning";
            };
            default -> "Tamabee HR Notification";
        };
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null)
            return "0";

        // Luôn format theo JPY vì tiền trong hệ thống là JPY
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        return formatter.format(amount);
    }

    private String formatDate(LocalDateTime dateTime, String language) {
        if (dateTime == null)
            return "";

        DateTimeFormatter formatter = switch (language) {
            case "vi" -> DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            case "ja" -> DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
            default -> DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        };

        return dateTime.format(formatter);
    }
}
