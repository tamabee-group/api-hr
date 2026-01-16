package com.tamabee.api_hr.service.core.impl;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.tamabee.api_hr.service.core.interfaces.IEmailService;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;
    private final String adminEmail;
    private final String baseUrl;
    
    // Logo URLs từ GitHub raw (dùng cho email, không có attachment preview icons)
    private static final String LOGO_URL = "https://raw.githubusercontent.com/tamabee-group/api-hr/main/src/main/resources/templates/images/logo.png";
    private static final String LOGO_TEXT_URL = "https://raw.githubusercontent.com/tamabee-group/api-hr/main/src/main/resources/templates/images/logo-text-light.png";

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.admin-email:admin@tamabee.vn}") String adminEmail,
            @Value("${app.base-url:https://tamabee.vn}") String baseUrl) {
        this.mailSender = mailSender;
        this.adminEmail = adminEmail;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Thay thế CID references bằng URL trong template
     */
    private String replaceLogoWithUrl(String content) {
        content = content.replace("cid:logo", LOGO_URL);
        content = content.replace("cid:logoText", LOGO_TEXT_URL);
        return content;
    }

    @Override
    public void sendWelcomeCompany(String email, String ownerName, String companyName, String tenantDomain,
            LocalDateTime freeTrialEnd, String language) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(email);
            helper.setSubject(getSubject("welcome-company", language));

            String template = loadTemplate("welcome-company", language);
            String loginUrl = baseUrl.replace("tamabee.vn", tenantDomain + ".tamabee.vn") + "/" + language + "/login";
            
            String content = template
                    .replace("{ownerName}", ownerName)
                    .replace("{companyName}", companyName)
                    .replace("{tenantDomain}", tenantDomain)
                    .replace("{loginUrl}", loginUrl)
                    .replace("{freeTrialEnd}", formatDate(freeTrialEnd, language));

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email chào mừng đến: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email welcome-company đến {}: {}", email, e.getMessage());
        }
    }

    @Override
    public void sendNewCompanyNotification(String companyName, String ownerName, String email, String tenantDomain,
            String referralCode, String referrerName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(adminEmail);
            helper.setSubject("[Tamabee] Công ty mới đăng ký: " + companyName);

            String template = loadTemplate("new-company-notification", "vi");
            
            // Format referral info
            String referralInfo = "Không có";
            if (referralCode != null && !referralCode.isEmpty()) {
                referralInfo = referralCode;
                if (referrerName != null && !referrerName.isEmpty()) {
                    referralInfo += " (" + referrerName + ")";
                }
            }
            
            String content = template
                    .replace("{companyName}", companyName)
                    .replace("{ownerName}", ownerName)
                    .replace("{email}", email)
                    .replace("{tenantDomain}", tenantDomain)
                    .replace("{referralCode}", referralInfo)
                    .replace("{date}", formatDate(LocalDateTime.now(), "vi"));

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email thông báo công ty mới đến admin");
        } catch (Exception e) {
            log.error("Lỗi khi gửi email new-company-notification: {}", e.getMessage());
        }
    }

    @Override
    public void sendReferralCommissionNotification(String referrerEmail, String referrerName, String newCompanyName,
            String language) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(referrerEmail);
            helper.setSubject(getSubject("referral-commission", language));

            String template = loadTemplate("referral-commission", language);
            String content = template
                    .replace("{referrerName}", referrerName != null ? referrerName : "")
                    .replace("{newCompanyName}", newCompanyName)
                    .replace("{date}", formatDate(LocalDateTime.now(), language));

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email thông báo hoa hồng đến: {}", referrerEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email referral-commission đến {}: {}", referrerEmail, e.getMessage());
        }
    }

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

            helper.setText(replaceLogoWithUrl(content), true);
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

            helper.setText(replaceLogoWithUrl(content), true);
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

            helper.setText(replaceLogoWithUrl(content), true);
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

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email thông báo insufficient balance đến: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email insufficient-balance đến {}: {}", email, e.getMessage());
        }
    }

    @Override
    public void sendNewDepositNotification(String companyName, BigDecimal amount, String transferProofUrl,
            String requesterName, String requesterEmail) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(adminEmail);
            helper.setSubject("[Tamabee] Yêu cầu nạp tiền mới: " + companyName + " - " + formatCurrency(amount));

            String template = loadTemplate("new-deposit-notification", "vi");
            
            // Build full image URL
            String imageUrl = baseUrl + transferProofUrl;
            
            String content = template
                    .replace("{companyName}", companyName)
                    .replace("{amount}", formatCurrency(amount))
                    .replace("{requesterName}", requesterName != null ? requesterName : "N/A")
                    .replace("{requesterEmail}", requesterEmail != null ? requesterEmail : "N/A")
                    .replace("{transferProofUrl}", imageUrl)
                    .replace("{date}", formatDate(LocalDateTime.now(), "vi"))
                    .replace("{adminUrl}", baseUrl + "/vi/admin/deposits");

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email thông báo yêu cầu nạp tiền mới đến admin");
        } catch (Exception e) {
            log.error("Lỗi khi gửi email new-deposit-notification: {}", e.getMessage());
        }
    }

    @Override
    public void sendCleanupReport(java.util.List<String> deletedCompanies, int retentionDays) {
        // Không gửi email nếu không có company nào bị xóa
        if (deletedCompanies == null || deletedCompanies.isEmpty()) {
            log.info("Không có company nào bị xóa, bỏ qua gửi email báo cáo");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(adminEmail);
            helper.setSubject("[Tamabee] Báo cáo xóa công ty INACTIVE - " + deletedCompanies.size() + " công ty");

            String template = loadTemplate("cleanup-report", "vi");
            
            // Build danh sách companies
            StringBuilder companiesList = new StringBuilder();
            for (int i = 0; i < deletedCompanies.size(); i++) {
                companiesList.append("<tr><td style=\"padding:8px;border:1px solid #ddd;\">")
                        .append(i + 1)
                        .append("</td><td style=\"padding:8px;border:1px solid #ddd;\">")
                        .append(deletedCompanies.get(i))
                        .append("</td></tr>");
            }
            
            String content = template
                    .replace("{count}", String.valueOf(deletedCompanies.size()))
                    .replace("{retentionDays}", String.valueOf(retentionDays))
                    .replace("{companiesList}", companiesList.toString())
                    .replace("{date}", formatDate(LocalDateTime.now(), "vi"));

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email báo cáo cleanup đến admin: {} companies đã xóa", deletedCompanies.size());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email cleanup-report: {}", e.getMessage());
        }
    }

    @Override
    public void sendReactivationNotification(String email, String companyName, BigDecimal balance, String language) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("Tamabee <tamabee.info@gmail.com>");
            helper.setTo(email);
            helper.setSubject(getSubject("reactivation", language));

            String template = loadTemplate("reactivation", language);
            String content = template
                    .replace("{companyName}", companyName)
                    .replace("{balance}", formatCurrency(balance))
                    .replace("{date}", formatDate(LocalDateTime.now(), language))
                    .replace("{year}", String.valueOf(LocalDateTime.now().getYear()));

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email thông báo reactivation đến: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email reactivation đến {}: {}", email, e.getMessage());
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
            case "welcome-company" ->
                "<p>Dear {ownerName},</p><p>Welcome to Tamabee HR! Your company {companyName} has been registered.</p><p>Login at: {loginUrl}</p>";
            case "new-company-notification" ->
                "<p>New company registered: {companyName}</p><p>Owner: {ownerName}</p><p>Email: {email}</p><p>Domain: {tenantDomain}</p>";
            case "referral-commission" ->
                "<p>Dear {referrerName},</p><p>A new company {newCompanyName} has registered using your referral code. You will receive commission!</p>";
            case "new-deposit-notification" ->
                "<p>Yêu cầu nạp tiền mới từ: {companyName}</p><p>Số tiền: {amount}</p><p>Người gửi: {requesterName} ({requesterEmail})</p><p>Thời gian: {date}</p><p><img src=\"{transferProofUrl}\" style=\"max-width:400px;\"/></p><p><a href=\"{adminUrl}\">Xem chi tiết</a></p>";
            case "cleanup-report" ->
                "<p>Báo cáo xóa công ty INACTIVE</p><p>Số công ty đã xóa: {count}</p><p>Thời gian retention: {retentionDays} ngày</p><p>Thời gian: {date}</p><table border=\"1\"><tr><th>STT</th><th>Tên công ty</th></tr>{companiesList}</table>";
            case "reactivation" ->
                "<p>Dear {companyName},</p><p>Your account has been reactivated! Current balance: {balance}</p>";
            default -> "<p>Notification from Tamabee HR</p>";
        };
    }

    private String getSubject(String templateName, String language) {
        return switch (templateName) {
            case "welcome-company" -> switch (language) {
                case "vi" -> "Tamabee HR - Chào mừng đến với Tamabee!";
                case "ja" -> "Tamabee HR - Tamabeeへようこそ！";
                default -> "Tamabee HR - Welcome to Tamabee!";
            };
            case "referral-commission" -> switch (language) {
                case "vi" -> "Tamabee HR - Thông báo hoa hồng giới thiệu";
                case "ja" -> "Tamabee HR - 紹介コミッションのお知らせ";
                default -> "Tamabee HR - Referral Commission Notification";
            };
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
            case "reactivation" -> switch (language) {
                case "vi" -> "Tamabee HR - Tài khoản đã được kích hoạt lại";
                case "ja" -> "Tamabee HR - アカウントが再有効化されました";
                default -> "Tamabee HR - Account Reactivated";
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
