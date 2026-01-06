package com.tamabee.api_hr.service.core.impl;

import com.tamabee.api_hr.entity.attendance.AttendanceAdjustmentRequestEntity;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import com.tamabee.api_hr.entity.leave.LeaveRequestEntity;
import com.tamabee.api_hr.entity.payroll.PayrollRecordEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.LeaveType;
import com.tamabee.api_hr.repository.AttendanceAdjustmentRequestRepository;
import com.tamabee.api_hr.repository.BreakRecordRepository;
import com.tamabee.api_hr.repository.LeaveRequestRepository;
import com.tamabee.api_hr.repository.PayrollRecordRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.core.INotificationEmailService;
import com.tamabee.api_hr.util.LocaleUtil;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Service gửi email thông báo cho nhân viên.
 * Hỗ trợ đa ngôn ngữ (vi, en, ja) với fallback sang English.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailServiceImpl implements INotificationEmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final AttendanceAdjustmentRequestRepository adjustmentRequestRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final BreakRecordRepository breakRecordRepository;

    private static final String FROM_EMAIL = "Tamabee <tamabee.info@gmail.com>";

    // ==================== Salary Notification ====================

    @Override
    public void sendSalaryNotification(Long employeeId, PayrollRecordEntity payroll) {
        try {
            UserEntity employee = userRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                log.warn("Không tìm thấy nhân viên {} để gửi thông báo lương", employeeId);
                return;
            }

            String language = getLanguageFromLocale(employee.getLocale());
            String employeeName = getEmployeeName(employee);
            String period = formatPeriod(payroll.getYear(), payroll.getMonth(), language);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(FROM_EMAIL);
            helper.setTo(employee.getEmail());
            helper.setSubject(getSalaryNotificationSubject(language, period));

            String template = loadTemplate("salary-notification", language);
            String content = template
                    .replace("{employeeName}", employeeName)
                    .replace("{period}", period)
                    .replace("{netSalary}", formatCurrency(payroll.getNetSalary(), language))
                    .replace("{baseSalary}", formatCurrency(payroll.getBaseSalary(), language))
                    .replace("{totalOvertime}", formatCurrency(payroll.getTotalOvertimePay(), language))
                    .replace("{totalAllowances}", formatCurrency(payroll.getTotalAllowances(), language))
                    .replace("{totalDeductions}", formatCurrency(payroll.getTotalDeductions(), language))
                    .replace("{paymentDate}", formatDate(LocalDateTime.now(), language));

            helper.setText(content, true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi thông báo lương đến nhân viên {}", employee.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo lương cho nhân viên {}: {}", employeeId, e.getMessage());
        }
    }

    @Override
    public void sendBulkSalaryNotifications(Long companyId, Integer year, Integer month) {
        List<PayrollRecordEntity> records = payrollRecordRepository
                .findPendingNotifications(companyId, year, month);

        for (PayrollRecordEntity record : records) {
            sendSalaryNotification(record.getEmployeeId(), record);
        }

        log.info("Đã gửi {} thông báo lương cho công ty {}", records.size(), companyId);
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

            String language = getLanguageFromLocale(employee.getLocale());
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

            helper.setFrom(FROM_EMAIL);
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

            helper.setText(content, true);
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

            String language = getLanguageFromLocale(employee.getLocale());
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

            helper.setFrom(FROM_EMAIL);
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

            helper.setText(content, true);
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

            String language = getLanguageFromLocale(employee.getLocale());
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

            helper.setFrom(FROM_EMAIL);
            helper.setTo(employee.getEmail());
            helper.setSubject(getLeaveApprovedSubject(language));

            String template = loadTemplate("leave-approved", language);
            String content = template
                    .replace("{employeeName}", employeeName)
                    .replace("{leaveType}", translateLeaveType(request.getLeaveType(), language))
                    .replace("{startDate}", formatLocalDate(request.getStartDate(), language))
                    .replace("{endDate}", formatLocalDate(request.getEndDate(), language))
                    .replace("{approverName}", approverName);

            helper.setText(content, true);
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

            String language = getLanguageFromLocale(employee.getLocale());
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

            helper.setFrom(FROM_EMAIL);
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

            helper.setText(content, true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi thông báo nghỉ phép bị từ chối đến nhân viên {}", employee.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo nghỉ phép bị từ chối cho nhân viên {}: {}", employeeId, e.getMessage());
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Lấy language code từ locale/timezone
     */
    private String getLanguageFromLocale(String locale) {
        if (locale == null) {
            return "en";
        }
        return LocaleUtil.timezoneToLocale(locale);
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
                return new String(resource.readAllBytes());
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
     * Format currency theo ngôn ngữ
     */
    private String formatCurrency(BigDecimal amount, String language) {
        if (amount == null) {
            return "0";
        }

        Locale locale = switch (language) {
            case "vi" -> Locale.of("vi", "VN");
            case "ja" -> Locale.JAPAN;
            default -> Locale.US;
        };

        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
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
                case PERSONAL -> "Nghỉ việc riêng";
                case UNPAID -> "Nghỉ không lương";
            };
            case "ja" -> switch (leaveType) {
                case ANNUAL -> "年次有給休暇";
                case SICK -> "病気休暇";
                case PERSONAL -> "私用休暇";
                case UNPAID -> "無給休暇";
            };
            default -> switch (leaveType) {
                case ANNUAL -> "Annual Leave";
                case SICK -> "Sick Leave";
                case PERSONAL -> "Personal Leave";
                case UNPAID -> "Unpaid Leave";
            };
        };
    }

    /**
     * Build break section HTML cho email adjustment approved
     */
    private String buildBreakSectionApproved(AttendanceAdjustmentRequestEntity request, String language) {
        // Nếu không có thông tin break, trả về empty string
        if (request.getOriginalBreakStart() == null && request.getRequestedBreakStart() == null
                && request.getBreakRecordId() == null) {
            return "";
        }

        // Lấy breakNumber từ BreakRecordEntity nếu có breakRecordId
        // BreakRecord không có soft delete
        Integer breakNumber = null;
        if (request.getBreakRecordId() != null) {
            BreakRecordEntity breakRecord = breakRecordRepository.findById(request.getBreakRecordId())
                    .orElse(null);
            if (breakRecord != null) {
                breakNumber = breakRecord.getBreakNumber();
            }
        }

        StringBuilder sb = new StringBuilder();

        // Header cho break section
        String originalBreakLabel = switch (language) {
            case "vi" -> "Giờ giải lao ban đầu:";
            case "ja" -> "元の休憩時間:";
            default -> "Original Break Time:";
        };

        String approvedBreakLabel = switch (language) {
            case "vi" -> "Giờ giải lao đã duyệt:";
            case "ja" -> "承認された休憩時間:";
            default -> "Approved Break Time:";
        };

        String breakStartLabel = switch (language) {
            case "vi" -> "Bắt đầu:";
            case "ja" -> "開始:";
            default -> "Start:";
        };

        String breakEndLabel = switch (language) {
            case "vi" -> "Kết thúc:";
            case "ja" -> "終了:";
            default -> "End:";
        };

        String breakNumberLabel = switch (language) {
            case "vi" -> "Lần nghỉ thứ:";
            case "ja" -> "休憩回数:";
            default -> "Break #:";
        };

        // Original break times
        sb.append(
                "<tr><td colspan=\"2\" style=\"padding: 12px 0 4px 0; color: #666; font-size: 13px; font-weight: bold;\">")
                .append(originalBreakLabel).append("</td></tr>");

        if (breakNumber != null) {
            sb.append("<tr><td style=\"padding: 4px 0 4px 16px; color: #666;\">").append(breakNumberLabel)
                    .append("</td>")
                    .append("<td style=\"padding: 4px 0; text-align: right;\">").append(breakNumber)
                    .append("</td></tr>");
        }

        sb.append("<tr><td style=\"padding: 4px 0 4px 16px; color: #666;\">").append(breakStartLabel).append("</td>")
                .append("<td style=\"padding: 4px 0; text-align: right;\">")
                .append(formatTime(request.getOriginalBreakStart(), language)).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 4px 0 4px 16px; color: #666;\">").append(breakEndLabel).append("</td>")
                .append("<td style=\"padding: 4px 0; text-align: right;\">")
                .append(formatTime(request.getOriginalBreakEnd(), language)).append("</td></tr>");

        // Approved break times
        sb.append(
                "<tr><td colspan=\"2\" style=\"padding: 12px 0 4px 0; color: #28a745; font-size: 13px; font-weight: bold;\">")
                .append(approvedBreakLabel).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 4px 0 4px 16px; color: #666;\">").append(breakStartLabel).append("</td>")
                .append("<td style=\"padding: 4px 0; text-align: right; font-weight: bold; color: #28a745;\">")
                .append(formatTime(request.getRequestedBreakStart(), language)).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 4px 0 4px 16px; color: #666;\">").append(breakEndLabel).append("</td>")
                .append("<td style=\"padding: 4px 0; text-align: right; font-weight: bold; color: #28a745;\">")
                .append(formatTime(request.getRequestedBreakEnd(), language)).append("</td></tr>");

        return sb.toString();
    }

    /**
     * Build break section HTML cho email adjustment rejected
     */
    private String buildBreakSectionRejected(AttendanceAdjustmentRequestEntity request, String language) {
        // Nếu không có thông tin break, trả về empty string
        if (request.getOriginalBreakStart() == null && request.getRequestedBreakStart() == null
                && request.getBreakRecordId() == null) {
            return "";
        }

        // Lấy breakNumber từ BreakRecordEntity nếu có breakRecordId
        // BreakRecord không có soft delete
        Integer breakNumber = null;
        if (request.getBreakRecordId() != null) {
            BreakRecordEntity breakRecord = breakRecordRepository.findById(request.getBreakRecordId())
                    .orElse(null);
            if (breakRecord != null) {
                breakNumber = breakRecord.getBreakNumber();
            }
        }

        StringBuilder sb = new StringBuilder();

        // Header cho break section
        String originalBreakLabel = switch (language) {
            case "vi" -> "Giờ giải lao ban đầu:";
            case "ja" -> "元の休憩時間:";
            default -> "Original Break Time:";
        };

        String requestedBreakLabel = switch (language) {
            case "vi" -> "Giờ giải lao yêu cầu:";
            case "ja" -> "申請した休憩時間:";
            default -> "Requested Break Time:";
        };

        String breakStartLabel = switch (language) {
            case "vi" -> "Bắt đầu:";
            case "ja" -> "開始:";
            default -> "Start:";
        };

        String breakEndLabel = switch (language) {
            case "vi" -> "Kết thúc:";
            case "ja" -> "終了:";
            default -> "End:";
        };

        String breakNumberLabel = switch (language) {
            case "vi" -> "Lần nghỉ thứ:";
            case "ja" -> "休憩回数:";
            default -> "Break #:";
        };

        // Original break times
        sb.append(
                "<tr><td colspan=\"2\" style=\"padding: 12px 0 4px 0; color: #666; font-size: 13px; font-weight: bold;\">")
                .append(originalBreakLabel).append("</td></tr>");

        if (breakNumber != null) {
            sb.append("<tr><td style=\"padding: 4px 0 4px 16px; color: #666;\">").append(breakNumberLabel)
                    .append("</td>")
                    .append("<td style=\"padding: 4px 0; text-align: right;\">").append(breakNumber)
                    .append("</td></tr>");
        }

        sb.append("<tr><td style=\"padding: 4px 0 4px 16px; color: #666;\">").append(breakStartLabel).append("</td>")
                .append("<td style=\"padding: 4px 0; text-align: right;\">")
                .append(formatTime(request.getOriginalBreakStart(), language)).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 4px 0 4px 16px; color: #666;\">").append(breakEndLabel).append("</td>")
                .append("<td style=\"padding: 4px 0; text-align: right;\">")
                .append(formatTime(request.getOriginalBreakEnd(), language)).append("</td></tr>");

        // Requested break times (rejected)
        sb.append(
                "<tr><td colspan=\"2\" style=\"padding: 12px 0 4px 0; color: #dc3545; font-size: 13px; font-weight: bold;\">")
                .append(requestedBreakLabel).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 4px 0 4px 16px; color: #666;\">").append(breakStartLabel).append("</td>")
                .append("<td style=\"padding: 4px 0; text-align: right; color: #dc3545;\">")
                .append(formatTime(request.getRequestedBreakStart(), language)).append("</td></tr>");
        sb.append("<tr><td style=\"padding: 4px 0 4px 16px; color: #666;\">").append(breakEndLabel).append("</td>")
                .append("<td style=\"padding: 4px 0; text-align: right; color: #dc3545;\">")
                .append(formatTime(request.getRequestedBreakEnd(), language)).append("</td></tr>");

        return sb.toString();
    }
}
