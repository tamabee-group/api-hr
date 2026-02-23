package com.tamabee.api_hr.service.core.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.entity.attendance.AttendanceAdjustmentRequestEntity;
import com.tamabee.api_hr.entity.leave.LeaveRequestEntity;
import com.tamabee.api_hr.entity.payroll.PayrollItemEntity;
import com.tamabee.api_hr.entity.payroll.PayrollPeriodEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.LeaveType;
import com.tamabee.api_hr.repository.attendance.AttendanceAdjustmentRequestRepository;
import com.tamabee.api_hr.repository.attendance.BreakRecordRepository;
import com.tamabee.api_hr.repository.leave.LeaveRequestRepository;
import com.tamabee.api_hr.repository.payroll.PayrollItemRepository;
import com.tamabee.api_hr.repository.payroll.PayrollPeriodRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.core.interfaces.INotificationEmailService;
import com.tamabee.api_hr.util.RegionUtil;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Service gửi email thông báo cho nhân viên.
 * Hỗ trợ đa ngôn ngữ (vi, en, ja) với fallback sang English.
 */
@Slf4j
@Service
public class NotificationEmailServiceImpl implements INotificationEmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final AttendanceAdjustmentRequestRepository adjustmentRequestRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final String mailFrom;

    public NotificationEmailServiceImpl(
            JavaMailSender mailSender,
            UserRepository userRepository,
            PayrollItemRepository payrollItemRepository,
            PayrollPeriodRepository payrollPeriodRepository,
            AttendanceAdjustmentRequestRepository adjustmentRequestRepository,
            LeaveRequestRepository leaveRequestRepository,
            BreakRecordRepository breakRecordRepository,
            @Value("${app.mail-from:Tamabee <info@tamabee.vn>}") String mailFrom) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.payrollItemRepository = payrollItemRepository;
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.adjustmentRequestRepository = adjustmentRequestRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.breakRecordRepository = breakRecordRepository;
        this.mailFrom = mailFrom;
    }
    
    // Logo URLs từ GitHub raw
    private static final String LOGO_URL = "https://raw.githubusercontent.com/tamabee-group/api-hr/main/src/main/resources/templates/images/logo.png";
    private static final String LOGO_TEXT_URL = "https://raw.githubusercontent.com/tamabee-group/api-hr/main/src/main/resources/templates/images/logo-text-light.png";
    
    /**
     * Thay thế CID references bằng URL trong template
     * Lưu ý: replace logoText trước logo để tránh conflict
     */
    private String replaceLogoWithUrl(String content) {
        content = content.replace("cid:logoText", LOGO_TEXT_URL);
        content = content.replace("cid:logo", LOGO_URL);
        return content;
    }

    // ==================== Salary Notification ====================

    @Override
    public void sendSalaryNotification(Long employeeId, PayrollItemEntity payrollItem) {
        try {
            UserEntity employee = userRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                log.warn("Không tìm thấy nhân viên {} để gửi thông báo lương", employeeId);
                return;
            }

            // Lấy thông tin period để có year/month
            PayrollPeriodEntity period = payrollPeriodRepository.findById(payrollItem.getPayrollPeriodId())
                    .orElse(null);
            if (period == null) {
                log.warn("Không tìm thấy kỳ lương {} để gửi thông báo", payrollItem.getPayrollPeriodId());
                return;
            }

            String language = getLanguageFromRegion(employee.getLanguage());
            String employeeName = getEmployeeName(employee);
            String periodStr = formatPeriod(period.getYear(), period.getMonth(), language);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(employee.getEmail());
            helper.setSubject(getSalaryNotificationSubject(language, periodStr));

            String template = loadTemplate("salary-notification", language);
            String content = template
                    .replace("{employeeName}", employeeName)
                    .replace("{period}", periodStr)
                    .replace("{netSalary}", formatCurrency(payrollItem.getNetSalary(), language))
                    .replace("{baseSalary}", formatCurrency(payrollItem.getCalculatedBaseSalary(), language))
                    .replace("{totalOvertime}", formatCurrency(payrollItem.getTotalOvertimePay(), language))
                    .replace("{totalAllowances}", formatCurrency(payrollItem.getTotalAllowances(), language))
                    .replace("{totalDeductions}", formatCurrency(payrollItem.getTotalDeductions(), language))
                    .replace("{paymentDate}", formatDate(LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())), language));

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi thông báo lương đến nhân viên {}", employee.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo lương cho nhân viên {}: {}", employeeId, e.getMessage());
        }
    }

    @Override
    public void sendBulkSalaryNotifications(Long companyId, Integer year, Integer month) {
        // Tìm period theo year/month
        PayrollPeriodEntity period = payrollPeriodRepository.findByYearAndMonth(year, month).orElse(null);
        if (period == null) {
            log.warn("Không tìm thấy kỳ lương {}/{}", year, month);
            return;
        }

        // Lấy tất cả payroll items của period
        java.util.List<PayrollItemEntity> items = payrollItemRepository.findByPayrollPeriodId(period.getId());

        for (PayrollItemEntity item : items) {
            sendSalaryNotification(item.getEmployeeId(), item);
        }

        log.info("Đã gửi {} thông báo lương", items.size());
    }

    // ==================== Adjustment Notification ====================

    @Override
    public void sendAdjustmentApprovedNotification(Long employeeId, Long requestId) {
        try {
            UserEntity employee = userRepository.findById(employeeId).orElse(null);
            AttendanceAdjustmentRequestEntity request = adjustmentRequestRepository.findById(requestId)
                    .orElse(null);

            if (employee == null || request == null) {
                log.warn("Không tìm thấy nhân viên {} hoặc yêu cầu {} để gửi thông báo", employeeId, requestId);
                return;
            }

            String language = getLanguageFromRegion(employee.getLanguage());
            String employeeName = getEmployeeName(employee);

            // Lấy tên người phê duyệt
            String approverName = "Manager";
            if (request.getApprovedBy() != null) {
                UserEntity approver = userRepository.findById(request.getApprovedBy()).orElse(null);
                if (approver != null) {
                    approverName = getEmployeeName(approver);
                }
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(employee.getEmail());
            helper.setSubject(getAdjustmentApprovedSubject(language));

            String template = loadTemplate("adjustment-approved", language);
            String breakSection = buildBreakSectionApproved(request, language);
            String content = template
                    .replace("{employeeName}", employeeName)
                    .replace("{workDate}", formatDateFromDateTime(request.getOriginalCheckIn(), language))
                    .replace("{originalCheckIn}", formatTime(request.getOriginalCheckIn(), language))
                    .replace("{originalCheckOut}", formatTime(request.getOriginalCheckOut(), language))
                    .replace("{approvedCheckIn}", formatTime(request.getRequestedCheckIn(), language))
                    .replace("{approvedCheckOut}", formatTime(request.getRequestedCheckOut(), language))
                    .replace("{breakSection}", breakSection)
                    .replace("{approverName}", approverName);

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi thông báo điều chỉnh được duyệt đến nhân viên {}", employee.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo điều chỉnh được duyệt cho nhân viên {}: {}", employeeId, e.getMessage());
        }
    }

    @Override
    public void sendAdjustmentRejectedNotification(Long employeeId, Long requestId) {
        try {
            UserEntity employee = userRepository.findById(employeeId).orElse(null);
            AttendanceAdjustmentRequestEntity request = adjustmentRequestRepository.findById(requestId)
                    .orElse(null);

            if (employee == null || request == null) {
                log.warn("Không tìm thấy nhân viên {} hoặc yêu cầu {} để gửi thông báo", employeeId, requestId);
                return;
            }

            String language = getLanguageFromRegion(employee.getLanguage());
            String employeeName = getEmployeeName(employee);

            // Lấy tên người từ chối
            String approverName = "Manager";
            if (request.getApprovedBy() != null) {
                UserEntity approver = userRepository.findById(request.getApprovedBy()).orElse(null);
                if (approver != null) {
                    approverName = getEmployeeName(approver);
                }
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(employee.getEmail());
            helper.setSubject(getAdjustmentRejectedSubject(language));

            String template = loadTemplate("adjustment-rejected", language);
            String breakSection = buildBreakSectionRejected(request, language);
            String content = template
                    .replace("{employeeName}", employeeName)
                    .replace("{workDate}", formatDateFromDateTime(request.getOriginalCheckIn(), language))
                    .replace("{originalCheckIn}", formatTime(request.getOriginalCheckIn(), language))
                    .replace("{originalCheckOut}", formatTime(request.getOriginalCheckOut(), language))
                    .replace("{requestedCheckIn}", formatTime(request.getRequestedCheckIn(), language))
                    .replace("{requestedCheckOut}", formatTime(request.getRequestedCheckOut(), language))
                    .replace("{breakSection}", breakSection)
                    .replace("{rejectionReason}",
                            request.getRejectionReason() != null ? request.getRejectionReason() : "")
                    .replace("{approverName}", approverName);

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi thông báo điều chỉnh bị từ chối đến nhân viên {}", employee.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo điều chỉnh bị từ chối cho nhân viên {}: {}", employeeId, e.getMessage());
        }
    }

    // ==================== Leave Notification ====================

    @Override
    public void sendLeaveApprovedNotification(Long employeeId, Long requestId) {
        try {
            UserEntity employee = userRepository.findById(employeeId).orElse(null);
            LeaveRequestEntity request = leaveRequestRepository.findById(requestId).orElse(null);

            if (employee == null || request == null) {
                log.warn("Không tìm thấy nhân viên {} hoặc yêu cầu {} để gửi thông báo", employeeId, requestId);
                return;
            }

            String language = getLanguageFromRegion(employee.getLanguage());
            String employeeName = getEmployeeName(employee);

            // Lấy tên người phê duyệt
            String approverName = "Manager";
            if (request.getApprovedBy() != null) {
                UserEntity approver = userRepository.findById(request.getApprovedBy()).orElse(null);
                if (approver != null) {
                    approverName = getEmployeeName(approver);
                }
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(employee.getEmail());
            helper.setSubject(getLeaveApprovedSubject(language));

            String template = loadTemplate("leave-approved", language);
            String content = template
                    .replace("{employeeName}", employeeName)
                    .replace("{leaveType}", translateLeaveType(request.getLeaveType(), language))
                    .replace("{startDate}", formatLocalDate(request.getStartDate(), language))
                    .replace("{endDate}", formatLocalDate(request.getEndDate(), language))
                    .replace("{approverName}", approverName);

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi thông báo nghỉ phép được duyệt đến nhân viên {}", employee.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo nghỉ phép được duyệt cho nhân viên {}: {}", employeeId, e.getMessage());
        }
    }

    @Override
    public void sendLeaveRejectedNotification(Long employeeId, Long requestId) {
        try {
            UserEntity employee = userRepository.findById(employeeId).orElse(null);
            LeaveRequestEntity request = leaveRequestRepository.findById(requestId).orElse(null);

            if (employee == null || request == null) {
                log.warn("Không tìm thấy nhân viên {} hoặc yêu cầu {} để gửi thông báo", employeeId, requestId);
                return;
            }

            String language = getLanguageFromRegion(employee.getLanguage());
            String employeeName = getEmployeeName(employee);

            // Lấy tên người từ chối
            String approverName = "Manager";
            if (request.getApprovedBy() != null) {
                UserEntity approver = userRepository.findById(request.getApprovedBy()).orElse(null);
                if (approver != null) {
                    approverName = getEmployeeName(approver);
                }
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(employee.getEmail());
            helper.setSubject(getLeaveRejectedSubject(language));

            String template = loadTemplate("leave-rejected", language);
            String content = template
                    .replace("{employeeName}", employeeName)
                    .replace("{leaveType}", translateLeaveType(request.getLeaveType(), language))
                    .replace("{startDate}", formatLocalDate(request.getStartDate(), language))
                    .replace("{endDate}", formatLocalDate(request.getEndDate(), language))
                    .replace("{rejectionReason}",
                            request.getRejectionReason() != null ? request.getRejectionReason() : "")
                    .replace("{approverName}", approverName);

            helper.setText(replaceLogoWithUrl(content), true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi thông báo nghỉ phép bị từ chối đến nhân viên {}", employee.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo nghỉ phép bị từ chối cho nhân viên {}: {}", employeeId, e.getMessage());
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Lấy language code từ region code
     * Region "vi" → language "vi", "ja" → "ja", default → "en"
     */
    private String getLanguageFromRegion(String language) {
        if (language == null) {
            return "vi";
        }
        if (RegionUtil.isValidRegion(language)) {
            return language;
        }
        return "vi";
    }

    /**
     * Lấy tên nhân viên
     */
    private String getEmployeeName(UserEntity employee) {
        if (employee.getProfile() != null && employee.getProfile().getName() != null) {
            return employee.getProfile().getName();
        }
        return employee.getEmail();
    }

    /**
     * Load email template với fallback sang English
     */
    private String loadTemplate(String templateName, String language) {
        try {
            var resource = getClass()
                    .getResourceAsStream("/templates/email/" + language + "/" + templateName + ".html");
            if (resource == null) {
                // Fallback to English
                resource = getClass().getResourceAsStream("/templates/email/en/" + templateName + ".html");
            }
            if (resource != null) {
                return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            }
            return getDefaultTemplate(templateName);
        } catch (Exception e) {
            log.error("Lỗi khi đọc template email {}: {}", templateName, e.getMessage());
            return getDefaultTemplate(templateName);
        }
    }

    /**
     * Template mặc định khi không tìm thấy file
     */
    private String getDefaultTemplate(String templateName) {
        return switch (templateName) {
            case "salary-notification" -> """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <h2 style="color: #00b1ce;">Salary Notification</h2>
                        <p>Dear {employeeName},</p>
                        <p>Your salary for {period} has been processed.</p>
                        <table style="width: 100%; border-collapse: collapse;">
                            <tr><td>Base Salary:</td><td style="text-align: right;">{baseSalary}</td></tr>
                            <tr><td>Overtime:</td><td style="text-align: right;">{totalOvertime}</td></tr>
                            <tr><td>Allowances:</td><td style="text-align: right;">{totalAllowances}</td></tr>
                            <tr><td>Deductions:</td><td style="text-align: right;">-{totalDeductions}</td></tr>
                            <tr style="font-weight: bold; border-top: 2px solid #00b1ce;">
                                <td>Net Salary:</td><td style="text-align: right;">{netSalary}</td>
                            </tr>
                        </table>
                        <p>Payment Date: {paymentDate}</p>
                        <p>Best regards,<br>Tamabee HR</p>
                    </div>
                    """;
            default -> "<p>Notification from Tamabee HR</p>";
        };
    }

    /**
     * Lấy subject cho email thông báo lương
     */
    private String getSalaryNotificationSubject(String language, String period) {
        return switch (language) {
            case "vi" -> "Tamabee HR - Thông báo lương " + period;
            case "ja" -> "Tamabee HR - 給与通知 " + period;
            default -> "Tamabee HR - Salary Notification " + period;
        };
    }

    /**
     * Lấy subject cho email thông báo điều chỉnh được duyệt
     */
    private String getAdjustmentApprovedSubject(String language) {
        return switch (language) {
            case "vi" -> "Tamabee HR - Yêu cầu điều chỉnh chấm công đã được duyệt";
            case "ja" -> "Tamabee HR - 勤怠修正申請が承認されました";
            default -> "Tamabee HR - Attendance Adjustment Request Approved";
        };
    }

    /**
     * Lấy subject cho email thông báo điều chỉnh bị từ chối
     */
    private String getAdjustmentRejectedSubject(String language) {
        return switch (language) {
            case "vi" -> "Tamabee HR - Yêu cầu điều chỉnh chấm công bị từ chối";
            case "ja" -> "Tamabee HR - 勤怠修正申請が却下されました";
            default -> "Tamabee HR - Attendance Adjustment Request Rejected";
        };
    }

    /**
     * Lấy subject cho email thông báo nghỉ phép được duyệt
     */
    private String getLeaveApprovedSubject(String language) {
        return switch (language) {
            case "vi" -> "Tamabee HR - Đơn xin nghỉ phép đã được duyệt";
            case "ja" -> "Tamabee HR - 休暇申請が承認されました";
            default -> "Tamabee HR - Leave Request Approved";
        };
    }

    /**
     * Lấy subject cho email thông báo nghỉ phép bị từ chối
     */
    private String getLeaveRejectedSubject(String language) {
        return switch (language) {
            case "vi" -> "Tamabee HR - Đơn xin nghỉ phép bị từ chối";
            case "ja" -> "Tamabee HR - 休暇申請が却下されました";
            default -> "Tamabee HR - Leave Request Rejected";
        };
    }

    /**
     * Format period theo ngôn ngữ
     */
    private String formatPeriod(Integer year, Integer month, String language) {
        return switch (language) {
            case "vi" -> String.format("Tháng %d/%d", month, year);
            case "ja" -> String.format("%d年%d月", year, month);
            default -> String.format("%d-%02d", year, month);
        };
    }

    /**
     * Format currency theo ngôn ngữ của nhân viên (lương)
     */
    private String formatCurrency(BigDecimal amount, String language) {
        if (amount == null) {
            return "0";
        }

        Locale region = switch (language) {
            case "vi" -> Locale.of("vi", "VN");
            case "ja" -> Locale.JAPAN;
            default -> Locale.US;
        };

        NumberFormat formatter = NumberFormat.getCurrencyInstance(region);
        return formatter.format(amount);
    }

    /**
     * Format date theo ngôn ngữ
     */
    private String formatDate(LocalDateTime dateTime, String language) {
        if (dateTime == null) {
            return "";
        }

        DateTimeFormatter formatter = switch (language) {
            case "vi" -> DateTimeFormatter.ofPattern("dd/MM/yyyy");
            case "ja" -> DateTimeFormatter.ofPattern("yyyy年MM月dd日");
            default -> DateTimeFormatter.ofPattern("MMM dd, yyyy");
        };

        return dateTime.format(formatter);
    }

    /**
     * Format LocalDate theo ngôn ngữ
     */
    private String formatLocalDate(LocalDate date, String language) {
        if (date == null) {
            return "";
        }

        DateTimeFormatter formatter = switch (language) {
            case "vi" -> DateTimeFormatter.ofPattern("dd/MM/yyyy");
            case "ja" -> DateTimeFormatter.ofPattern("yyyy年MM月dd日");
            default -> DateTimeFormatter.ofPattern("MMM dd, yyyy");
        };

        return date.format(formatter);
    }

    /**
     * Format date từ LocalDateTime (chỉ lấy phần ngày)
     */
    private String formatDateFromDateTime(LocalDateTime dateTime, String language) {
        if (dateTime == null) {
            return "";
        }
        return formatLocalDate(dateTime.toLocalDate(), language);
    }

    /**
     * Format time từ LocalDateTime (chỉ lấy phần giờ:phút)
     */
    private String formatTime(LocalDateTime dateTime, String language) {
        if (dateTime == null) {
            return "-";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
    }

    /**
     * Dịch loại nghỉ phép theo ngôn ngữ
     */
    private String translateLeaveType(LeaveType leaveType, String language) {
        if (leaveType == null) {
            return "";
        }

        return switch (language) {
            case "vi" -> switch (leaveType) {
                case ANNUAL -> "Nghỉ phép năm";
                case SICK -> "Nghỉ ốm";
                case MATERNITY -> "Nghỉ thai sản (nữ)";
                case PATERNITY -> "Nghỉ thai sản (nam)";
                case BEREAVEMENT -> "Nghỉ tang";
                case UNPAID -> "Nghỉ không lương";
                case OTHER -> "Khác";
            };
            case "ja" -> switch (leaveType) {
                case ANNUAL -> "年次有給休暇";
                case SICK -> "病気休暇";
                case MATERNITY -> "産休（女性）";
                case PATERNITY -> "産休（男性）";
                case BEREAVEMENT -> "忌引休暇";
                case UNPAID -> "無給休暇";
                case OTHER -> "その他";
            };
            default -> switch (leaveType) {
                case ANNUAL -> "Annual Leave";
                case SICK -> "Sick Leave";
                case MATERNITY -> "Maternity Leave";
                case PATERNITY -> "Paternity Leave";
                case BEREAVEMENT -> "Bereavement Leave";
                case UNPAID -> "Unpaid Leave";
                case OTHER -> "Other";
            };
        };
    }

    /**
     * Build break section HTML cho email adjustment approved
     * Hỗ trợ nhiều break items trong 1 request
     */
    private String buildBreakSectionApproved(AttendanceAdjustmentRequestEntity request, String language) {
        // Nếu không có break items, trả về empty string
        if (request.getBreakItems() == null || request.getBreakItems().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Labels
        String breakSectionLabel = switch (language) {
            case "vi" -> "Điều chỉnh giờ giải lao:";
            case "ja" -> "休憩時間の調整:";
            default -> "Break Time Adjustments:";
        };

        String breakNumberLabel = switch (language) {
            case "vi" -> "Lần nghỉ thứ";
            case "ja" -> "休憩";
            default -> "Break #";
        };

        String originalLabel = switch (language) {
            case "vi" -> "Ban đầu:";
            case "ja" -> "元:";
            default -> "Original:";
        };

        String approvedLabel = switch (language) {
            case "vi" -> "Đã duyệt:";
            case "ja" -> "承認:";
            default -> "Approved:";
        };

        String deleteLabel = switch (language) {
            case "vi" -> "Đã xóa";
            case "ja" -> "削除済み";
            default -> "Deleted";
        };

        // Header
        sb.append("<tr><td colspan=\"2\" style=\"padding: 12px 0 4px 0; color: #666; font-size: 13px; font-weight: bold;\">")
                .append(breakSectionLabel).append("</td></tr>");

        // Render từng break item
        for (var item : request.getBreakItems()) {
            Integer breakNumber = item.getBreakNumber();
            String breakLabel = breakNumber != null ? breakNumberLabel + " " + breakNumber : breakNumberLabel;

            sb.append("<tr><td colspan=\"2\" style=\"padding: 8px 0 4px 8px; color: #333; font-weight: 500;\">")
                    .append(breakLabel).append("</td></tr>");

            // Original time
            sb.append("<tr><td style=\"padding: 2px 0 2px 16px; color: #666; font-size: 12px;\">").append(originalLabel).append("</td>")
                    .append("<td style=\"padding: 2px 0; text-align: right; font-size: 12px;\">")
                    .append(formatTime(item.getOriginalBreakStart(), language))
                    .append(" - ")
                    .append(formatTime(item.getOriginalBreakEnd(), language))
                    .append("</td></tr>");

            // Approved time hoặc Deleted
            if (item.getActionType() == com.tamabee.api_hr.enums.BreakActionType.DELETE) {
                sb.append("<tr><td style=\"padding: 2px 0 2px 16px; color: #666; font-size: 12px;\">").append(approvedLabel).append("</td>")
                        .append("<td style=\"padding: 2px 0; text-align: right; font-size: 12px; color: #dc3545; font-weight: bold;\">")
                        .append(deleteLabel).append("</td></tr>");
            } else {
                sb.append("<tr><td style=\"padding: 2px 0 2px 16px; color: #666; font-size: 12px;\">").append(approvedLabel).append("</td>")
                        .append("<td style=\"padding: 2px 0; text-align: right; font-size: 12px; color: #28a745; font-weight: bold;\">")
                        .append(formatTime(item.getRequestedBreakStart(), language))
                        .append(" - ")
                        .append(formatTime(item.getRequestedBreakEnd(), language))
                        .append("</td></tr>");
            }
        }

        return sb.toString();
    }

        // Header cho break section
    /**
     * Build break section HTML cho email adjustment rejected
     * Hỗ trợ nhiều break items trong 1 request
     */
    private String buildBreakSectionRejected(AttendanceAdjustmentRequestEntity request, String language) {
        // Nếu không có break items, trả về empty string
        if (request.getBreakItems() == null || request.getBreakItems().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Labels
        String breakSectionLabel = switch (language) {
            case "vi" -> "Yêu cầu điều chỉnh giờ giải lao:";
            case "ja" -> "休憩時間の調整申請:";
            default -> "Break Time Adjustment Requests:";
        };

        String breakNumberLabel = switch (language) {
            case "vi" -> "Lần nghỉ thứ";
            case "ja" -> "休憩";
            default -> "Break #";
        };

        String originalLabel = switch (language) {
            case "vi" -> "Ban đầu:";
            case "ja" -> "元:";
            default -> "Original:";
        };

        String requestedLabel = switch (language) {
            case "vi" -> "Yêu cầu:";
            case "ja" -> "申請:";
            default -> "Requested:";
        };

        String deleteRequestLabel = switch (language) {
            case "vi" -> "Yêu cầu xóa";
            case "ja" -> "削除申請";
            default -> "Delete Request";
        };

        // Header
        sb.append("<tr><td colspan=\"2\" style=\"padding: 12px 0 4px 0; color: #666; font-size: 13px; font-weight: bold;\">")
                .append(breakSectionLabel).append("</td></tr>");

        // Render từng break item
        for (var item : request.getBreakItems()) {
            Integer breakNumber = item.getBreakNumber();
            String breakLabel = breakNumber != null ? breakNumberLabel + " " + breakNumber : breakNumberLabel;

            sb.append("<tr><td colspan=\"2\" style=\"padding: 8px 0 4px 8px; color: #333; font-weight: 500;\">")
                    .append(breakLabel).append("</td></tr>");

            // Original time
            sb.append("<tr><td style=\"padding: 2px 0 2px 16px; color: #666; font-size: 12px;\">").append(originalLabel).append("</td>")
                    .append("<td style=\"padding: 2px 0; text-align: right; font-size: 12px;\">")
                    .append(formatTime(item.getOriginalBreakStart(), language))
                    .append(" - ")
                    .append(formatTime(item.getOriginalBreakEnd(), language))
                    .append("</td></tr>");

            // Requested time hoặc Delete request
            if (item.getActionType() == com.tamabee.api_hr.enums.BreakActionType.DELETE) {
                sb.append("<tr><td style=\"padding: 2px 0 2px 16px; color: #666; font-size: 12px;\">").append(requestedLabel).append("</td>")
                        .append("<td style=\"padding: 2px 0; text-align: right; font-size: 12px; color: #dc3545;\">")
                        .append(deleteRequestLabel).append("</td></tr>");
            } else {
                sb.append("<tr><td style=\"padding: 2px 0 2px 16px; color: #666; font-size: 12px;\">").append(requestedLabel).append("</td>")
                        .append("<td style=\"padding: 2px 0; text-align: right; font-size: 12px; color: #dc3545;\">")
                        .append(formatTime(item.getRequestedBreakStart(), language))
                        .append(" - ")
                        .append(formatTime(item.getRequestedBreakEnd(), language))
                        .append("</td></tr>");
            }
        }

        return sb.toString();
    }
}
