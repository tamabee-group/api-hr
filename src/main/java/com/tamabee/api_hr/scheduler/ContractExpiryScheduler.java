package com.tamabee.api_hr.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.tamabee.api_hr.util.RegionUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.datasource.TenantDataSourceManager;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.repository.company.CompanyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job để xử lý hợp đồng hết hạn
 * Chạy vào 00:30 mỗi ngày để kiểm tra và cập nhật trạng thái
 * Duyệt qua tất cả tenants vì employment_contracts nằm trong tenant DB
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractExpiryScheduler {

    private final CompanyRepository companyRepository;
    private final TenantDataSourceManager tenantDataSourceManager;

    /**
     * Xử lý hợp đồng hết hạn
     * Chạy vào 00:30 mỗi ngày (cron: giây phút giờ ngày tháng thứ)
     * - Cập nhật trạng thái hợp đồng từ ACTIVE sang EXPIRED
     * - Cập nhật trạng thái nhân viên sang INACTIVE nếu không có hợp đồng mới
     */
    @Scheduled(cron = "0 30 0 * * *")
    public void processExpiredContracts() {
        log.info("=== BẮT ĐẦU SCHEDULED JOB: Contract Expiry Check ===");
        try {
            List<CompanyEntity> companies = companyRepository.findAllByDeletedFalse();
            log.info("Tìm thấy {} công ty để kiểm tra hợp đồng", companies.size());

            for (CompanyEntity company : companies) {
                try {
                    processCompanyContracts(company);
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý hợp đồng cho company {}: {}",
                            company.getId(), e.getMessage(), e);
                }
            }

            log.info("=== KẾT THÚC SCHEDULED JOB: Contract Expiry Check - THÀNH CÔNG ===");
        } catch (Exception e) {
            log.error("=== KẾT THÚC SCHEDULED JOB: Contract Expiry Check - LỖI: {} ===", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Xử lý hợp đồng hết hạn cho từng company bằng JDBC trực tiếp
     */
    private void processCompanyContracts(CompanyEntity company) {
        DataSource tenantDs = tenantDataSourceManager.getDataSource(company.getTenantDomain());
        if (tenantDs == null) {
            log.warn("Tenant DataSource not found for company {}: {}",
                    company.getId(), company.getTenantDomain());
            return;
        }

        LocalDate today = LocalDate.now(RegionUtil.getTimezone(company.getRegion()));
        int contractsUpdated = 0;
        int employeesDeactivated = 0;

        try (Connection conn = tenantDs.getConnection()) {
            // Tìm các hợp đồng đã hết hạn nhưng vẫn đang ACTIVE
            String findExpiredSql = "SELECT id, employee_id FROM employment_contracts " +
                    "WHERE deleted = false AND status = 'ACTIVE' " +
                    "AND end_date IS NOT NULL AND end_date < ?";

            List<long[]> expiredContracts = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(findExpiredSql)) {
                ps.setObject(1, today);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        expiredContracts.add(new long[]{rs.getLong("id"), rs.getLong("employee_id")});
                    }
                }
            }

            if (expiredContracts.isEmpty()) {
                return;
            }

            log.info("Company {}: Tìm thấy {} hợp đồng hết hạn", company.getId(), expiredContracts.size());

            // Cập nhật trạng thái hợp đồng sang EXPIRED
            String updateContractSql = "UPDATE employment_contracts SET status = 'EXPIRED', updated_at = NOW() WHERE id = ?";

            // Kiểm tra nhân viên có hợp đồng active khác không
            String checkActiveContractSql = "SELECT COUNT(*) > 0 FROM employment_contracts " +
                    "WHERE deleted = false AND employee_id = ? AND status = 'ACTIVE' " +
                    "AND start_date <= ? AND (end_date IS NULL OR end_date >= ?)";

            // Cập nhật nhân viên sang INACTIVE
            String deactivateUserSql = "UPDATE users SET status = 'INACTIVE', updated_at = NOW() " +
                    "WHERE id = ? AND status = 'ACTIVE'";

            for (long[] contract : expiredContracts) {
                long contractId = contract[0];
                long employeeId = contract[1];

                // Cập nhật hợp đồng
                try (PreparedStatement ps = conn.prepareStatement(updateContractSql)) {
                    ps.setLong(1, contractId);
                    ps.executeUpdate();
                    contractsUpdated++;
                }

                // Kiểm tra có hợp đồng active khác không
                boolean hasActiveContract = false;
                try (PreparedStatement ps = conn.prepareStatement(checkActiveContractSql)) {
                    ps.setLong(1, employeeId);
                    ps.setObject(2, today);
                    ps.setObject(3, today);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            hasActiveContract = rs.getBoolean(1);
                        }
                    }
                }

                if (!hasActiveContract) {
                    try (PreparedStatement ps = conn.prepareStatement(deactivateUserSql)) {
                        ps.setLong(1, employeeId);
                        int updated = ps.executeUpdate();
                        if (updated > 0) {
                            employeesDeactivated++;
                            log.info("Company {}: Đã cập nhật nhân viên {} sang INACTIVE do hết hợp đồng",
                                    company.getId(), employeeId);
                        }
                    }
                }
            }

            log.info("Company {}: Đã cập nhật {} hợp đồng, {} nhân viên chuyển sang INACTIVE",
                    company.getId(), contractsUpdated, employeesDeactivated);

        } catch (Exception e) {
            log.debug("Company {} không có bảng employment_contracts hoặc lỗi: {}",
                    company.getId(), e.getMessage());
        }
    }
}
