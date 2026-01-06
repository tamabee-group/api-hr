package com.tamabee.api_hr.datasource;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Load tất cả tenant DataSources khi application khởi động.
 * Bao gồm "tamabee" DataSource cho Tamabee company.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantDataSourceLoader {

    private static final String TAMABEE_TENANT = "tamabee";

    private final CompanyRepository companyRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final TenantDatabaseInitializer tenantDatabaseInitializer;

    /**
     * Load tất cả tenant DataSources khi application sẵn sàng.
     * Chạy sau khi tất cả beans đã được khởi tạo.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadAllTenantDataSources() {
        log.info("Starting to load tenant DataSources...");

        // 1. Load Tamabee tenant trước
        loadTamabeeTenant();

        // 2. Load tất cả active company tenants
        loadActiveCompanyTenants();

        log.info("Finished loading tenant DataSources. Total: {}",
                tenantDataSourceManager.getAllDataSources().size());
    }

    /**
     * Load Tamabee tenant DataSource.
     * Tamabee là tenant đặc biệt với tenantDomain = "tamabee".
     */
    private void loadTamabeeTenant() {
        try {
            if (tenantDataSourceManager.hasTenant(TAMABEE_TENANT)) {
                log.debug("Tamabee tenant already loaded");
                return;
            }

            // Kiểm tra database tamabee_tamabee có tồn tại không
            if (tenantDatabaseInitializer.databaseExists(TAMABEE_TENANT)) {
                tenantDataSourceManager.addTenant(TAMABEE_TENANT);
                log.info("Loaded Tamabee tenant DataSource");
            } else {
                log.warn("Tamabee database does not exist. Creating...");
                tenantDatabaseInitializer.createTenantDatabase(TAMABEE_TENANT);
                log.info("Created and loaded Tamabee tenant DataSource");
            }
        } catch (Exception e) {
            log.error("Failed to load Tamabee tenant DataSource", e);
        }
    }

    /**
     * Load DataSources cho tất cả active companies.
     * Chỉ load companies có status = ACTIVE và deleted = false.
     */
    private void loadActiveCompanyTenants() {
        List<CompanyEntity> activeCompanies = companyRepository.findAllByStatusAndDeletedFalse(CompanyStatus.ACTIVE);

        int loaded = 0;
        int failed = 0;

        for (CompanyEntity company : activeCompanies) {
            String tenantDomain = company.getTenantDomain();

            // Skip Tamabee (đã load ở trên)
            if (TAMABEE_TENANT.equals(tenantDomain)) {
                continue;
            }

            // Skip nếu đã load
            if (tenantDataSourceManager.hasTenant(tenantDomain)) {
                continue;
            }

            try {
                // Kiểm tra database có tồn tại không
                if (tenantDatabaseInitializer.databaseExists(tenantDomain)) {
                    tenantDataSourceManager.addTenant(tenantDomain);
                    loaded++;
                    log.debug("Loaded tenant DataSource: {}", tenantDomain);
                } else {
                    log.warn("Database not found for tenant: {}. Company may need re-provisioning.", tenantDomain);
                    failed++;
                }
            } catch (Exception e) {
                log.error("Failed to load tenant DataSource: {}", tenantDomain, e);
                failed++;
            }
        }

        log.info("Loaded {} tenant DataSources, {} failed", loaded, failed);
    }
}
