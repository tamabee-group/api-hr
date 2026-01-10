package com.tamabee.api_hr.scheduler;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job để archive/delete tenant databases đã inactive quá thời gian
 * retention.
 * Mặc định: 90 ngày sau khi deactivate.
 * Chạy vào 02:00 mỗi ngày để tránh ảnh hưởng đến hoạt động bình thường.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantCleanupScheduler {

    private final CompanyRepository companyRepository;

    /**
     * Số ngày giữ lại tenant database sau khi deactivate (mặc định 90 ngày)
     */
    @Value("${tenant.cleanup.retention-days:90}")
    private int retentionDays;

    /**
     * Bật/tắt tính năng cleanup (mặc định tắt để an toàn)
     */
    @Value("${tenant.cleanup.enabled:false}")
    private boolean cleanupEnabled;

    /**
     * Xử lý cleanup tenant databases đã quá thời gian retention.
     * Chạy vào 02:00 mỗi ngày (cron: giây phút giờ ngày tháng thứ)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupInactiveTenants() {
        if (!cleanupEnabled) {
            log.debug("Tenant cleanup is disabled. Skipping...");
            return;
        }

        log.info("=== BẮT ĐẦU SCHEDULED JOB: Tenant Cleanup ===");
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            List<CompanyEntity> companiesForCleanup = companyRepository
                    .findInactiveCompaniesForCleanup(cutoffDate);

            if (companiesForCleanup.isEmpty()) {
                log.info("Không có tenant nào cần cleanup");
                return;
            }

            log.info("Tìm thấy {} tenant(s) cần cleanup (inactive > {} ngày)",
                    companiesForCleanup.size(), retentionDays);

            for (CompanyEntity company : companiesForCleanup) {
                try {
                    archiveTenant(company);
                } catch (Exception e) {
                    log.error("Lỗi khi cleanup tenant: {} ({})",
                            company.getId(), company.getTenantDomain(), e);
                }
            }

            log.info("=== KẾT THÚC SCHEDULED JOB: Tenant Cleanup - THÀNH CÔNG ===");
        } catch (Exception e) {
            log.error("=== KẾT THÚC SCHEDULED JOB: Tenant Cleanup - LỖI: {} ===",
                    e.getMessage(), e);
        }
    }

    /**
     * Archive tenant - đánh dấu company là deleted (soft delete).
     * Database vẫn được giữ lại nhưng company không còn hiển thị trong hệ thống.
     * Việc xóa database thực sự nên được thực hiện thủ công bởi DBA.
     */
    private void archiveTenant(CompanyEntity company) {
        log.info("Archiving tenant: {} (tenantDomain: {}, deactivatedAt: {})",
                company.getId(), company.getTenantDomain(), company.getDeactivatedAt());

        // Soft delete company
        company.setDeleted(true);
        companyRepository.save(company);

        log.info("Archived tenant: {} (tenantDomain: {})",
                company.getId(), company.getTenantDomain());

        // NOTE: Việc DROP DATABASE nên được thực hiện thủ công bởi DBA
        // để đảm bảo backup đã được tạo trước khi xóa
    }
}
