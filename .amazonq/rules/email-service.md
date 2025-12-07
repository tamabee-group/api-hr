# Email Service Rules

## Email Template Structure

### Template Location
```
src/main/resources/templates/email/
├── ja/
│   ├── temporary-password.html
│   └── email-verification.html
├── vi/
│   ├── temporary-password.html
│   └── email-verification.html
└── en/
    ├── temporary-password.html
    └── email-verification.html
```

### Template Naming Convention
- Use kebab-case: `temporary-password.html`, `email-verification.html`
- Organize by language folder: `{language}/{template-name}.html`
- Always provide fallback to English template

## HTML Template Guidelines

### Simple HTML Structure
```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
</head>
<body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
        <h2 style="color: #00b1ce;">Tamabee HR</h2>
        <p>Content here...</p>
        <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0;">
            <p><strong>Label:</strong> {placeholder}</p>
        </div>
        <p>Footer text...</p>
    </div>
</body>
</html>
```

### Template Rules
1. **Inline CSS Only** - All styles must be inline for email compatibility
2. **Simple Layout** - Use basic HTML tags (div, p, h2, strong)
3. **Max Width 600px** - Standard email width
4. **UTF-8 Encoding** - Support all languages
5. **Placeholders** - Use `{variableName}` format
6. **Brand Color** - Use `#00b1ce` for Tamabee branding

## Email Service Implementation

### Service Interface
```java
public interface IEmailService {
    void sendTemporaryPassword(String email, String employeeCode, String temporaryPassword, String language);
    void sendVerificationCode(String email, String code, String language);
}
```

### Service Implementation Pattern
```java
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
            helper.setSubject(getSubject(language));
            
            String template = loadTemplate(language, "temporary-password");
            String content = template
                .replace("{employeeCode}", employeeCode)
                .replace("{email}", email)
                .replace("{temporaryPassword}", temporaryPassword);
            
            helper.setText(content, true);
            mailSender.send(mimeMessage);
            log.info("Email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", email, e.getMessage());
        }
    }
    
    private String loadTemplate(String language, String templateName) {
        try {
            var resource = getClass().getResourceAsStream(
                "/templates/email/" + language + "/" + templateName + ".html"
            );
            if (resource == null) {
                resource = getClass().getResourceAsStream(
                    "/templates/email/en/" + templateName + ".html"
                );
            }
            return new String(resource.readAllBytes());
        } catch (Exception e) {
            log.error("Failed to load template: {}", e.getMessage());
            return "<p>Error loading template</p>";
        }
    }
    
    private String getSubject(String language) {
        return switch (language) {
            case "ja" -> "Tamabee HR アカウント";
            case "vi" -> "Tài khoản Tamabee HR";
            default -> "Tamabee HR Account";
        };
    }
}
```

## Email Configuration

### application.yaml
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: tamabee.info@gmail.com
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: false
          starttls:
            enable: true
            required: true
        debug: false
```

## Best Practices

### 1. Language Support
- Always pass language parameter from user settings
- Fallback to English if template not found
- Support ja, vi, en by default

### 2. Template Variables
- Use descriptive placeholder names: `{employeeCode}`, `{temporaryPassword}`
- Document all placeholders in template comments
- Validate all variables before replacement

### 3. Error Handling
- Log all email sending attempts
- Don't throw exceptions on email failure
- Provide fallback plain text content

### 4. Email Sender
- Always use: `Tamabee <tamabee.info@gmail.com>`
- Consistent sender name across all emails
- Use MimeMessageHelper for HTML emails

### 5. Subject Lines
- Keep short and descriptive
- Localize based on language
- Include brand name "Tamabee HR"

### 6. Testing
- Test all language templates
- Verify placeholder replacement
- Check email rendering in multiple clients

## Common Email Templates

### 1. Temporary Password
**Purpose**: Send login credentials to new users
**Placeholders**: `{employeeCode}`, `{email}`, `{temporaryPassword}`
**Languages**: ja, vi, en

### 2. Email Verification
**Purpose**: Verify email during registration
**Placeholders**: `{companyName}`, `{code}`
**Languages**: ja, vi, en

### 3. Password Reset
**Purpose**: Send password reset link
**Placeholders**: `{resetLink}`, `{expiryTime}`
**Languages**: ja, vi, en

### 4. Wallet Low Balance
**Purpose**: Notify company of low wallet balance
**Placeholders**: `{companyName}`, `{balance}`, `{monthlyFee}`
**Languages**: ja, vi, en

## Template Creation Checklist

- [ ] Create HTML template for all 3 languages (ja, vi, en)
- [ ] Use inline CSS only
- [ ] Set max-width: 600px
- [ ] Include Tamabee branding color (#00b1ce)
- [ ] Use `{placeholder}` format for variables
- [ ] Test template rendering
- [ ] Add to EmailService with proper method
- [ ] Document placeholders
- [ ] Add subject line localization
- [ ] Test email sending
