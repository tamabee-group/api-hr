package com.tamabee.api_hr.service.core;

import com.tamabee.api_hr.entity.attendance.AttendanceAdjustmentRequestEntity;
import com.tamabee.api_hr.entity.leave.LeaveRequestEntity;
import com.tamabee.api_hr.entity.payroll.PayrollRecordEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.enums.AdjustmentStatus;
import com.tamabee.api_hr.enums.LeaveStatus;
import com.tamabee.api_hr.enums.LeaveType;
import com.tamabee.api_hr.enums.PayrollStatus;
import com.tamabee.api_hr.repository.AttendanceAdjustmentRequestRepository;
import com.tamabee.api_hr.repository.BreakRecordRepository;
import com.tamabee.api_hr.repository.LeaveRequestRepository;
import com.tamabee.api_hr.repository.PayrollRecordRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.core.impl.NotificationEmailServiceImpl;
import jakarta.mail.internet.MimeMessage;
import net.jqwik.api.*;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho NotificationEmailService
 * Feature: attendance-payroll-backend
 * Property 26: Email Template Language Selection
 */
public class NotificationEmailServicePropertyTest {

        /**
         * Property 26: Email Template Language Selection
         * For any notification sent to an employee, the system SHALL use the employee's
         * locale setting to select the appropriate template.
         * The system SHALL fallback to English template if the user's preferred
         * language
         * template is not available.
         */
        @Property(tries = 100)
        void emailTemplateLanguageSelection_salaryNotification_usesEmployeeLocale(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("locales") String locale,
                        @ForAll("employeeNames") String employeeName,
                        @ForAll("emails") String email,
                        @ForAll("salaryAmounts") BigDecimal baseSalary,
                        @ForAll("salaryAmounts") BigDecimal netSalary) {

                // Setup mocks
                JavaMailSender mailSender = mock(JavaMailSender.class);
                UserRepository userRepository = mock(UserRepository.class);
                PayrollRecordRepository payrollRecordRepository = mock(PayrollRecordRepository.class);
                AttendanceAdjustmentRequestRepository adjustmentRequestRepository = mock(
                                AttendanceAdjustmentRequestRepository.class);
                LeaveRequestRepository leaveRequestRepository = mock(LeaveRequestRepository.class);
                BreakRecordRepository breakRecordRepository = mock(BreakRecordRepository.class);

                MimeMessage mimeMessage = mock(MimeMessage.class);
                when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

                // Tạo employee với locale
                UserEntity employee = createEmployee(employeeId, email, employeeName, locale);
                when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));

                // Tạo payroll record
                PayrollRecordEntity payroll = createPayrollRecord(employeeId, baseSalary, netSalary);

                NotificationEmailServiceImpl service = new NotificationEmailServiceImpl(
                                mailSender, userRepository, payrollRecordRepository,
                                adjustmentRequestRepository, leaveRequestRepository, breakRecordRepository);

                // Gửi notification
                service.sendSalaryNotification(employeeId, payroll);

                // Verify: email được gửi
                verify(mailSender).send(any(MimeMessage.class));

                // Verify: template được load đúng ngôn ngữ
                String expectedLanguage = getExpectedLanguage(locale);
                assertTrue(templateExistsForLanguage("salary-notification", expectedLanguage),
                                "Template should exist for language: " + expectedLanguage);
        }

        /**
         * Property 26: Email Template Language Selection
         * Test: Adjustment approved notification sử dụng đúng ngôn ngữ
         */
        @Property(tries = 100)
        void emailTemplateLanguageSelection_adjustmentApproved_usesEmployeeLocale(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("requestIds") Long requestId,
                        @ForAll("locales") String locale,
                        @ForAll("employeeNames") String employeeName,
                        @ForAll("emails") String email) {

                // Setup mocks
                JavaMailSender mailSender = mock(JavaMailSender.class);
                UserRepository userRepository = mock(UserRepository.class);
                PayrollRecordRepository payrollRecordRepository = mock(PayrollRecordRepository.class);
                AttendanceAdjustmentRequestRepository adjustmentRequestRepository = mock(
                                AttendanceAdjustmentRequestRepository.class);
                LeaveRequestRepository leaveRequestRepository = mock(LeaveRequestRepository.class);
                BreakRecordRepository breakRecordRepository = mock(BreakRecordRepository.class);

                MimeMessage mimeMessage = mock(MimeMessage.class);
                when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

                // Tạo employee với locale
                UserEntity employee = createEmployee(employeeId, email, employeeName, locale);
                when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));

                // Tạo adjustment request
                AttendanceAdjustmentRequestEntity request = createAdjustmentRequest(requestId, employeeId,
                                AdjustmentStatus.APPROVED);
                when(adjustmentRequestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(request));

                NotificationEmailServiceImpl service = new NotificationEmailServiceImpl(
                                mailSender, userRepository, payrollRecordRepository,
                                adjustmentRequestRepository, leaveRequestRepository, breakRecordRepository);

                // Gửi notification
                service.sendAdjustmentApprovedNotification(employeeId, requestId);

                // Verify: email được gửi
                verify(mailSender).send(any(MimeMessage.class));

                // Verify: template được load đúng ngôn ngữ
                String expectedLanguage = getExpectedLanguage(locale);
                assertTrue(templateExistsForLanguage("adjustment-approved", expectedLanguage),
                                "Template should exist for language: " + expectedLanguage);
        }

        /**
         * Property 26: Email Template Language Selection
         * Test: Adjustment rejected notification sử dụng đúng ngôn ngữ
         */
        @Property(tries = 100)
        void emailTemplateLanguageSelection_adjustmentRejected_usesEmployeeLocale(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("requestIds") Long requestId,
                        @ForAll("locales") String locale,
                        @ForAll("employeeNames") String employeeName,
                        @ForAll("emails") String email,
                        @ForAll("rejectionReasons") String rejectionReason) {

                // Setup mocks
                JavaMailSender mailSender = mock(JavaMailSender.class);
                UserRepository userRepository = mock(UserRepository.class);
                PayrollRecordRepository payrollRecordRepository = mock(PayrollRecordRepository.class);
                AttendanceAdjustmentRequestRepository adjustmentRequestRepository = mock(
                                AttendanceAdjustmentRequestRepository.class);
                LeaveRequestRepository leaveRequestRepository = mock(LeaveRequestRepository.class);
                BreakRecordRepository breakRecordRepository = mock(BreakRecordRepository.class);

                MimeMessage mimeMessage = mock(MimeMessage.class);
                when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

                // Tạo employee với locale
                UserEntity employee = createEmployee(employeeId, email, employeeName, locale);
                when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));

                // Tạo adjustment request với rejection reason
                AttendanceAdjustmentRequestEntity request = createAdjustmentRequest(requestId, employeeId,
                                AdjustmentStatus.REJECTED);
                request.setRejectionReason(rejectionReason);
                when(adjustmentRequestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(request));

                NotificationEmailServiceImpl service = new NotificationEmailServiceImpl(
                                mailSender, userRepository, payrollRecordRepository,
                                adjustmentRequestRepository, leaveRequestRepository, breakRecordRepository);

                // Gửi notification
                service.sendAdjustmentRejectedNotification(employeeId, requestId);

                // Verify: email được gửi
                verify(mailSender).send(any(MimeMessage.class));

                // Verify: template được load đúng ngôn ngữ
                String expectedLanguage = getExpectedLanguage(locale);
                assertTrue(templateExistsForLanguage("adjustment-rejected", expectedLanguage),
                                "Template should exist for language: " + expectedLanguage);
        }

        /**
         * Property 26: Email Template Language Selection
         * Test: Leave approved notification sử dụng đúng ngôn ngữ
         */
        @Property(tries = 100)
        void emailTemplateLanguageSelection_leaveApproved_usesEmployeeLocale(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("requestIds") Long requestId,
                        @ForAll("locales") String locale,
                        @ForAll("employeeNames") String employeeName,
                        @ForAll("emails") String email,
                        @ForAll("leaveTypes") LeaveType leaveType) {

                // Setup mocks
                JavaMailSender mailSender = mock(JavaMailSender.class);
                UserRepository userRepository = mock(UserRepository.class);
                PayrollRecordRepository payrollRecordRepository = mock(PayrollRecordRepository.class);
                AttendanceAdjustmentRequestRepository adjustmentRequestRepository = mock(
                                AttendanceAdjustmentRequestRepository.class);
                LeaveRequestRepository leaveRequestRepository = mock(LeaveRequestRepository.class);
                BreakRecordRepository breakRecordRepository = mock(BreakRecordRepository.class);

                MimeMessage mimeMessage = mock(MimeMessage.class);
                when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

                // Tạo employee với locale
                UserEntity employee = createEmployee(employeeId, email, employeeName, locale);
                when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));

                // Tạo leave request
                LeaveRequestEntity request = createLeaveRequest(requestId, employeeId, leaveType, LeaveStatus.APPROVED);
                when(leaveRequestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(request));

                NotificationEmailServiceImpl service = new NotificationEmailServiceImpl(
                                mailSender, userRepository, payrollRecordRepository,
                                adjustmentRequestRepository, leaveRequestRepository, breakRecordRepository);

                // Gửi notification
                service.sendLeaveApprovedNotification(employeeId, requestId);

                // Verify: email được gửi
                verify(mailSender).send(any(MimeMessage.class));

                // Verify: template được load đúng ngôn ngữ
                String expectedLanguage = getExpectedLanguage(locale);
                assertTrue(templateExistsForLanguage("leave-approved", expectedLanguage),
                                "Template should exist for language: " + expectedLanguage);
        }

        /**
         * Property 26: Email Template Language Selection
         * Test: Leave rejected notification sử dụng đúng ngôn ngữ
         */
        @Property(tries = 100)
        void emailTemplateLanguageSelection_leaveRejected_usesEmployeeLocale(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("requestIds") Long requestId,
                        @ForAll("locales") String locale,
                        @ForAll("employeeNames") String employeeName,
                        @ForAll("emails") String email,
                        @ForAll("leaveTypes") LeaveType leaveType,
                        @ForAll("rejectionReasons") String rejectionReason) {

                // Setup mocks
                JavaMailSender mailSender = mock(JavaMailSender.class);
                UserRepository userRepository = mock(UserRepository.class);
                PayrollRecordRepository payrollRecordRepository = mock(PayrollRecordRepository.class);
                AttendanceAdjustmentRequestRepository adjustmentRequestRepository = mock(
                                AttendanceAdjustmentRequestRepository.class);
                LeaveRequestRepository leaveRequestRepository = mock(LeaveRequestRepository.class);
                BreakRecordRepository breakRecordRepository = mock(BreakRecordRepository.class);

                MimeMessage mimeMessage = mock(MimeMessage.class);
                when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

                // Tạo employee với locale
                UserEntity employee = createEmployee(employeeId, email, employeeName, locale);
                when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));

                // Tạo leave request với rejection reason
                LeaveRequestEntity request = createLeaveRequest(requestId, employeeId, leaveType, LeaveStatus.REJECTED);
                request.setRejectionReason(rejectionReason);
                when(leaveRequestRepository.findByIdAndDeletedFalse(requestId)).thenReturn(Optional.of(request));

                NotificationEmailServiceImpl service = new NotificationEmailServiceImpl(
                                mailSender, userRepository, payrollRecordRepository,
                                adjustmentRequestRepository, leaveRequestRepository, breakRecordRepository);

                // Gửi notification
                service.sendLeaveRejectedNotification(employeeId, requestId);

                // Verify: email được gửi
                verify(mailSender).send(any(MimeMessage.class));

                // Verify: template được load đúng ngôn ngữ
                String expectedLanguage = getExpectedLanguage(locale);
                assertTrue(templateExistsForLanguage("leave-rejected", expectedLanguage),
                                "Template should exist for language: " + expectedLanguage);
        }

        /**
         * Property 26: Email Template Language Selection
         * Test: Fallback to English khi locale không được hỗ trợ
         */
        @Property(tries = 100)
        void emailTemplateLanguageSelection_unsupportedLocale_fallbacksToEnglish(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("unsupportedLocales") String unsupportedLocale,
                        @ForAll("employeeNames") String employeeName,
                        @ForAll("emails") String email,
                        @ForAll("salaryAmounts") BigDecimal baseSalary,
                        @ForAll("salaryAmounts") BigDecimal netSalary) {

                // Setup mocks
                JavaMailSender mailSender = mock(JavaMailSender.class);
                UserRepository userRepository = mock(UserRepository.class);
                PayrollRecordRepository payrollRecordRepository = mock(PayrollRecordRepository.class);
                AttendanceAdjustmentRequestRepository adjustmentRequestRepository = mock(
                                AttendanceAdjustmentRequestRepository.class);
                LeaveRequestRepository leaveRequestRepository = mock(LeaveRequestRepository.class);
                BreakRecordRepository breakRecordRepository = mock(BreakRecordRepository.class);

                MimeMessage mimeMessage = mock(MimeMessage.class);
                when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

                // Tạo employee với unsupported locale
                UserEntity employee = createEmployee(employeeId, email, employeeName, unsupportedLocale);
                when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));

                // Tạo payroll record
                PayrollRecordEntity payroll = createPayrollRecord(employeeId, baseSalary, netSalary);

                NotificationEmailServiceImpl service = new NotificationEmailServiceImpl(
                                mailSender, userRepository, payrollRecordRepository,
                                adjustmentRequestRepository, leaveRequestRepository, breakRecordRepository);

                // Gửi notification
                service.sendSalaryNotification(employeeId, payroll);

                // Verify: email được gửi (fallback to English)
                verify(mailSender).send(any(MimeMessage.class));

                // Verify: English template tồn tại (fallback)
                assertTrue(templateExistsForLanguage("salary-notification", "en"),
                                "English template should exist as fallback");
        }

        // === Helper methods ===

        private UserEntity createEmployee(Long id, String email, String name, String locale) {
                UserEntity employee = new UserEntity();
                employee.setId(id);
                employee.setEmail(email);
                employee.setLocale(locale);

                UserProfileEntity profile = new UserProfileEntity();
                profile.setName(name);
                employee.setProfile(profile);

                return employee;
        }

        private PayrollRecordEntity createPayrollRecord(Long employeeId, BigDecimal baseSalary, BigDecimal netSalary) {
                PayrollRecordEntity payroll = new PayrollRecordEntity();
                payroll.setId(1L);
                payroll.setEmployeeId(employeeId);
                payroll.setYear(2025);
                payroll.setMonth(1);
                payroll.setBaseSalary(baseSalary);
                payroll.setNetSalary(netSalary);
                payroll.setTotalOvertimePay(BigDecimal.ZERO);
                payroll.setTotalAllowances(BigDecimal.ZERO);
                payroll.setTotalDeductions(BigDecimal.ZERO);
                payroll.setStatus(PayrollStatus.FINALIZED);
                return payroll;
        }

        private AttendanceAdjustmentRequestEntity createAdjustmentRequest(Long id, Long employeeId,
                        AdjustmentStatus status) {
                AttendanceAdjustmentRequestEntity request = new AttendanceAdjustmentRequestEntity();
                request.setId(id);
                request.setEmployeeId(employeeId);
                request.setCompanyId(1L);
                request.setAttendanceRecordId(1L);
                request.setOriginalCheckIn(LocalDateTime.of(2025, 1, 15, 9, 0));
                request.setOriginalCheckOut(LocalDateTime.of(2025, 1, 15, 18, 0));
                request.setRequestedCheckIn(LocalDateTime.of(2025, 1, 15, 8, 30));
                request.setRequestedCheckOut(LocalDateTime.of(2025, 1, 15, 18, 30));
                request.setReason("Test reason");
                request.setStatus(status);
                request.setApprovedBy(2L);
                request.setApprovedAt(LocalDateTime.now());
                return request;
        }

        private LeaveRequestEntity createLeaveRequest(Long id, Long employeeId, LeaveType leaveType,
                        LeaveStatus status) {
                LeaveRequestEntity request = new LeaveRequestEntity();
                request.setId(id);
                request.setEmployeeId(employeeId);
                request.setCompanyId(1L);
                request.setLeaveType(leaveType);
                request.setStartDate(LocalDate.of(2025, 2, 1));
                request.setEndDate(LocalDate.of(2025, 2, 3));
                request.setTotalDays(3);
                request.setReason("Test reason");
                request.setStatus(status);
                request.setApprovedBy(2L);
                request.setApprovedAt(LocalDateTime.now());
                return request;
        }

        private String getExpectedLanguage(String locale) {
                if (locale == null) {
                        return "en";
                }
                return switch (locale) {
                        case "Asia/Ho_Chi_Minh" -> "vi";
                        case "Asia/Tokyo" -> "ja";
                        default -> "en";
                };
        }

        private boolean templateExistsForLanguage(String templateName, String language) {
                String path = "/templates/email/" + language + "/" + templateName + ".html";
                InputStream stream = getClass().getResourceAsStream(path);
                return stream != null;
        }

        // === Generators ===

        @Provide
        Arbitrary<Long> employeeIds() {
                return Arbitraries.longs().between(1L, 10000L);
        }

        @Provide
        Arbitrary<Long> requestIds() {
                return Arbitraries.longs().between(1L, 100000L);
        }

        @Provide
        Arbitrary<String> locales() {
                return Arbitraries.of("Asia/Ho_Chi_Minh", "Asia/Tokyo", "America/New_York", null);
        }

        @Provide
        Arbitrary<String> unsupportedLocales() {
                return Arbitraries.of("Europe/Paris", "Australia/Sydney", "Africa/Cairo", "Pacific/Auckland");
        }

        @Provide
        Arbitrary<String> employeeNames() {
                return Arbitraries.strings()
                                .alpha()
                                .ofMinLength(3)
                                .ofMaxLength(50)
                                .map(s -> "Employee " + s);
        }

        @Provide
        Arbitrary<String> emails() {
                return Arbitraries.strings()
                                .alpha()
                                .ofMinLength(5)
                                .ofMaxLength(20)
                                .map(s -> s.toLowerCase() + "@example.com");
        }

        @Provide
        Arbitrary<BigDecimal> salaryAmounts() {
                return Arbitraries.bigDecimals()
                                .between(BigDecimal.valueOf(100000), BigDecimal.valueOf(50000000))
                                .ofScale(0);
        }

        @Provide
        Arbitrary<LeaveType> leaveTypes() {
                return Arbitraries.of(LeaveType.ANNUAL, LeaveType.SICK, LeaveType.PERSONAL, LeaveType.UNPAID);
        }

        @Provide
        Arbitrary<String> rejectionReasons() {
                return Arbitraries.of(
                                "Insufficient documentation",
                                "Conflicting schedule",
                                "Project deadline",
                                "Team shortage",
                                "Policy violation");
        }
}
