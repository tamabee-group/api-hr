package com.tamabee.api_hr.scheduler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.datasource.TenantDataSourceManager;
import com.tamabee.api_hr.dto.config.PayrollConfig;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.payroll.PayrollItemEntity;
import com.tamabee.api_hr.entity.payroll.PayrollPeriodEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.payroll.PayrollItemRepository;
import com.tamabee.api_hr.repository.payroll.PayrollPeriodRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.core.interfaces.IEmailService;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler tự động xử lý payroll vào ngày trả lương
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollScheduler {

    private final CompanyRepository companyRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final PayrollItemRepository itemRepository;
    private final UserRepository userRepository;
    private final IEmailService emailService;
    private final ObjectMapper objectMapper;
    private final TenantDataSourceManager tenantDataSourceManager;

    /**
     * Chạy vào 5:00 sáng mỗi ngày
     * Kiểm tra các công ty có ngày trả lương = hôm nay
     * Tự động chuyển status APPROVED -> PAID và gửi email thông báo
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void processPayrollPaymentDay() {
        log.info("=== BẮT ĐẦU SCHEDULED JOB: Payroll Payment Day ===");

        try {
            // Lấy tất cả companies (từ master DB)
            List<CompanyEntity> companies = companyRepository.findAllByDeletedFalse();
            log.info("Tìm thấy {} công ty để kiểm tra", companies.size());

            for (CompanyEntity company : companies) {
                try {
                    // Mỗi company dùng timezone riêng theo region
                    ZoneId zone = RegionUtil.getTimezone(company.getRegion());
                    LocalDate today = LocalDate.now(zone);
                    int currentDay = today.getDayOfMonth();
                    processCompanyPayroll(company, currentDay, today);
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý payroll cho company {}: {}", 
                            company.getId(), e.getMessage(), e);
                }
            }

            log.info("=== KẾT THÚC SCHEDULED JOB: Payroll Payment Day ===");
        } catch (Exception e) {
            log.error("Lỗi trong scheduled job processPayrollPaymentDay: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    private void processCompanyPayroll(CompanyEntity company, int currentDay, LocalDate today) {

        // Lấy DataSource của tenant trực tiếp (KHÔNG set TenantContext trước)
        DataSource tenantDs = tenantDataSourceManager.getDataSource(company.getTenantDomain());
        if (tenantDs == null) {
            log.warn("Tenant DataSource not found for company {}: {}", 
                    company.getId(), company.getTenantDomain());
            return;
        }

        // Query payroll_config từ company_settings bằng JDBC trực tiếp
        String payrollConfigJson = null;
        try (Connection conn = tenantDs.getConnection()) {
            String sql = "SELECT payroll_config FROM company_settings WHERE deleted = false LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // PostgreSQL JSONB trả về dạng PGobject, cần convert sang String
                        Object jsonObj = rs.getObject("payroll_config");
                        payrollConfigJson = jsonObj != null ? jsonObj.toString() : null;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Company {} không có company_settings hoặc chưa setup: {}", 
                    company.getId(), e.getMessage());
            return;
        }

        if (payrollConfigJson == null) {
            log.debug("Company {} không có payroll config", company.getId());
            return;
        }

        // Parse payroll config
        PayrollConfig payrollConfig;
        try {
            payrollConfig = objectMapper.readValue(payrollConfigJson, PayrollConfig.class);
        } catch (Exception e) {
            log.error("Lỗi parse payroll config cho company {}: {}", company.getId(), e.getMessage());
            return;
        }

        Integer payDay = payrollConfig.getPayDay();
        if (payDay == null || payDay != currentDay) {
            log.debug("Company {} có payDay={}, không khớp với ngày hiện tại {}", 
                    company.getId(), payDay, currentDay);
            return;
        }

        log.info("Company {} có payDay={}, bắt đầu xử lý payroll", 
                company.getId(), payDay);

        // Xử lý payroll periods bằng JDBC trực tiếp
        try {
            processPayrollPeriodsWithJdbc(company, today, tenantDs);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý payroll cho company {}: {}", company.getId(), e.getMessage(), e);
        }
    }

    /**
     * Xử lý payroll periods bằng JDBC trực tiếp (không dùng JPA)
     * Tìm periods có status=APPROVED của THÁNG TRƯỚC, update sang PAID và gửi email
     * Vì payDay là ngày trả lương cho tháng trước đó
     */
    private void processPayrollPeriodsWithJdbc(CompanyEntity company, LocalDate today, DataSource tenantDs) {
        try (Connection conn = tenantDs.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Tính tháng trước (tháng cần trả lương)
                LocalDate previousMonth = today.minusMonths(1);
                int targetYear = previousMonth.getYear();
                int targetMonth = previousMonth.getMonthValue();
                
                log.info("Tìm payroll periods của tháng {}/{} (tháng trước) cho company {}", 
                        targetMonth, targetYear, company.getId());
                
                // Tìm payroll periods có status = APPROVED của tháng trước
                String selectSql = """
                    SELECT id, year, month, period_start, period_end 
                    FROM payroll_periods 
                    WHERE status = 'APPROVED' AND year = ? AND month = ?
                    """;
                
                List<Long> periodIds = new java.util.ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, targetYear);
                    ps.setInt(2, targetMonth);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            periodIds.add(rs.getLong("id"));
                        }
                    }
                }
                
                if (periodIds.isEmpty()) {
                    log.info("Company {} không có payroll period nào ở trạng thái APPROVED cho tháng {}/{}", 
                            company.getId(), targetMonth, targetYear);
                    conn.rollback();
                    return;
                }
                
                log.info("Tìm thấy {} payroll periods cần xử lý cho company {}", periodIds.size(), company.getId());
                
                // Update status sang PAID
                String updateSql = """
                    UPDATE payroll_periods 
                    SET status = 'PAID', paid_at = ?, updated_at = NOW() 
                    WHERE id = ?
                    """;
                
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    for (Long periodId : periodIds) {
                        ps.setObject(1, today.atStartOfDay());
                        ps.setLong(2, periodId);
                        ps.executeUpdate();
                        
                        log.info("Đã cập nhật payroll period {} sang status PAID", periodId);
                    }
                }
                
                conn.commit();
                
                // Gửi email thông báo cho nhân viên (sau khi commit thành công)
                for (Long periodId : periodIds) {
                    try {
                        sendPayrollNotificationEmailsWithJdbc(company, periodId, today, tenantDs);
                    } catch (Exception e) {
                        log.error("Lỗi khi gửi email cho period {}: {}", periodId, e.getMessage(), e);
                    }
                }
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý payroll periods cho company {}: {}", company.getId(), e.getMessage(), e);
        }
    }

    /**
     * Gửi email thông báo lương cho nhân viên bằng JDBC
     */
    private void sendPayrollNotificationEmailsWithJdbc(CompanyEntity company, Long periodId, LocalDate today, DataSource tenantDs) {
        try (Connection conn = tenantDs.getConnection()) {
            // Lấy thông tin period
            Integer year = null;
            Integer month = null;
            String periodSql = "SELECT year, month FROM payroll_periods WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(periodSql)) {
                ps.setLong(1, periodId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        year = rs.getInt("year");
                        month = rs.getInt("month");
                    }
                }
            }
            
            if (year == null || month == null) {
                log.warn("Không tìm thấy period {}", periodId);
                return;
            }
            
            String periodStr = String.format("%02d/%d", month, year);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String paymentDate = today.format(dateFormatter);
            
            // Lấy danh sách payroll items và thông tin user
            String itemsSql = """
                SELECT 
                    pi.employee_id,
                    pi.calculated_base_salary,
                    pi.total_overtime_pay,
                    pi.total_allowances,
                    pi.total_deductions,
                    pi.net_salary,
                    u.email,
                    u.language,
                    up.name
                FROM payroll_items pi
                JOIN users u ON pi.employee_id = u.id
                LEFT JOIN user_profiles up ON u.id = up.user_id
                WHERE pi.payroll_period_id = ? AND u.deleted = false
                """;
            
            int emailCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
                ps.setLong(1, periodId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            String email = rs.getString("email");
                            String language = rs.getString("language");
                            String name = rs.getString("name");
                            
                            if (email == null) {
                                log.warn("Employee {} không có email", rs.getLong("employee_id"));
                                continue;
                            }
                            
                            String userName = (name != null) ? name : email;
                            String region = company.getRegion() != null ? company.getRegion() : "vi";
                            
                            // Format currency
                            String baseSalary = formatCurrency(rs.getBigDecimal("calculated_base_salary"), region);
                            String totalOvertime = formatCurrency(rs.getBigDecimal("total_overtime_pay"), region);
                            String totalAllowances = formatCurrency(rs.getBigDecimal("total_allowances"), region);
                            String totalDeductions = formatCurrency(rs.getBigDecimal("total_deductions"), region);
                            String netSalary = formatCurrency(rs.getBigDecimal("net_salary"), region);
                            
                            // Gửi email
                            emailService.sendPayrollNotification(
                                    email,
                                    userName,
                                    periodStr,
                                    baseSalary,
                                    totalOvertime,
                                    totalAllowances,
                                    totalDeductions,
                                    netSalary,
                                    paymentDate,
                                    language != null ? language : "vi"
                            );
                            
                            emailCount++;
                            log.info("Đã gửi email thông báo lương cho {} ({})", userName, email);
                            
                        } catch (Exception e) {
                            log.error("Lỗi khi gửi email cho employee {}: {}", 
                                    rs.getLong("employee_id"), e.getMessage(), e);
                        }
                    }
                }
            }
            
            log.info("Đã gửi {} email thông báo lương cho company {}", emailCount, company.getId());
            
        } catch (Exception e) {
            log.error("Lỗi khi gửi email thông báo payroll: {}", e.getMessage(), e);
        }
    }

    private void sendPayrollNotificationEmails(CompanyEntity company, PayrollPeriodEntity period) {
        try {
            // Lấy danh sách payroll items trong period
            List<PayrollItemEntity> items = itemRepository.findByPayrollPeriodId(period.getId());
            
            if (items.isEmpty()) {
                log.debug("Không có payroll items nào trong period {}", period.getId());
                return;
            }

            // Lấy danh sách employee IDs
            Set<Long> employeeIds = items.stream()
                    .map(PayrollItemEntity::getEmployeeId)
                    .collect(Collectors.toSet());

            // Lấy thông tin employees
            Map<Long, UserEntity> userMap = userRepository.findAllById(employeeIds).stream()
                    .collect(Collectors.toMap(UserEntity::getId, user -> user));

            // Gửi email cho từng nhân viên
            for (PayrollItemEntity item : items) {
                try {
                    UserEntity user = userMap.get(item.getEmployeeId());
                    if (user == null || user.getEmail() == null) {
                        log.warn("Không tìm thấy user hoặc email cho employee {}", item.getEmployeeId());
                        continue;
                    }

                    // Lấy tên từ profile
                    String userName = (user.getProfile() != null && user.getProfile().getName() != null) 
                            ? user.getProfile().getName() 
                            : user.getEmail();

                    // Format currency theo region của company
                    String region = company.getRegion() != null ? company.getRegion() : "vi";
                    String baseSalary = formatCurrency(item.getCalculatedBaseSalary(), region);
                    String totalOvertime = formatCurrency(item.getTotalOvertimePay(), region);
                    String totalAllowances = formatCurrency(item.getTotalAllowances(), region);
                    String totalDeductions = formatCurrency(item.getTotalDeductions(), region);
                    String netSalary = formatCurrency(item.getNetSalary(), region);

                    // Format period
                    String periodStr = String.format("%02d/%d", period.getMonth(), period.getYear());

                    // Format payment date
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    String paymentDate = period.getPaidAt() != null 
                            ? period.getPaidAt().format(dateFormatter)
                            : LocalDate.now(RegionUtil.getTimezone(company.getRegion())).format(dateFormatter);

                    // Gửi email
                    emailService.sendPayrollNotification(
                            user.getEmail(),
                            userName,
                            periodStr,
                            baseSalary,
                            totalOvertime,
                            totalAllowances,
                            totalDeductions,
                            netSalary,
                            paymentDate,
                            user.getLanguage() != null ? user.getLanguage() : "vi"
                    );

                    log.info("Đã gửi email thông báo lương cho employee {} ({})", 
                            userName, user.getEmail());

                } catch (Exception e) {
                    log.error("Lỗi khi gửi email cho employee {}: {}", 
                            item.getEmployeeId(), e.getMessage(), e);
                }
            }

            log.info("Đã gửi {} email thông báo lương cho company {}", items.size(), company.getId());
            
        } catch (Exception e) {
            log.error("Lỗi khi gửi email thông báo payroll: {}", e.getMessage(), e);
        }
    }

    private String formatCurrency(BigDecimal amount, String region) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        
        NumberFormat formatter;
        if ("ja".equals(region)) {
            formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        } else if ("en".equals(region)) {
            formatter = NumberFormat.getCurrencyInstance(Locale.US);
        } else {
            formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        }
        
        return formatter.format(amount);
    }
}
