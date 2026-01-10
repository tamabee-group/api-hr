package com.tamabee.api_hr.datasource;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
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
    private final DataSource dataSource; // TenantRoutingDataSource (primary)

    /**
     * Load tất cả tenant DataSources khi application sẵn sàng.
     * Chạy sau khi tất cả beans đã được khởi tạo.
     * Order = 1 để chạy trước các listeners khác (như DataInitializer).
     */
    @EventListener(ApplicationReadyEvent.class)
    @org.springframework.core.annotation.Order(1)
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
        log.info("Loading Tamabee tenant DataSource...");
        try {
            if (tenantDataSourceManager.hasTenant(TAMABEE_TENANT)) {
                log.info("Tamabee tenant already loaded in manager, adding to routing...");
                addToRoutingDataSource(TAMABEE_TENANT);
                return;
            }

            // Kiểm tra database tamabee_tamabee có tồn tại không
            boolean dbExists = tenantDatabaseInitializer.databaseExists(TAMABEE_TENANT);
            log.info("Tamabee database exists: {}", dbExists);

            if (dbExists) {
                tenantDataSourceManager.addTenant(TAMABEE_TENANT);
                log.info("Added Tamabee to TenantDataSourceManager");
                // Chạy migration để đảm bảo schema up-to-date
                runTenantMigration(TAMABEE_TENANT);
                // Add vào routing DataSource
                addToRoutingDataSource(TAMABEE_TENANT);
                log.info("Loaded Tamabee tenant DataSource successfully");
            } else {
                log.warn("Tamabee database does not exist. Creating...");
                tenantDatabaseInitializer.createTenantDatabase(TAMABEE_TENANT);
                // Add vào routing DataSource
                addToRoutingDataSource(TAMABEE_TENANT);
                log.info("Created and loaded Tamabee tenant DataSource");
            }
        } catch (Exception e) {
            log.error("Failed to load Tamabee tenant DataSource", e);
        }
    }

    /**
     * Add tenant DataSource vào TenantRoutingDataSource.
     */
    private void addToRoutingDataSource(String tenantDomain) {
        if (dataSource instanceof TenantRoutingDataSource routingDataSource) {
            DataSource tenantDs = tenantDataSourceManager.getDataSource(tenantDomain);
            if (tenantDs != null) {
                routingDataSource.addTenantDataSource(tenantDomain, tenantDs);
            }
        }
    }

    /**
     * Chạy Flyway migration cho tenant database.
     * Đảm bảo schema luôn up-to-date khi load tenant.
     */
    private void runTenantMigration(String tenantDomain) {
        try {
            javax.sql.DataSource tenantDs = tenantDataSourceManager.getDataSource(tenantDomain);
            if (tenantDs == null) {
                log.warn("DataSource not found for tenant: {}", tenantDomain);
                return;
            }

            org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                    .dataSource(tenantDs)
                    .locations("classpath:db/tenant")
                    .baselineOnMigrate(true)
                    .cleanDisabled(false)
                    .load();

            // Clean và migrate lại từ đầu (DB local mới)
            flyway.clean();
            flyway.migrate();
            log.info("Completed Flyway migration for tenant: {}", tenantDomain);
        } catch (Exception e) {
            log.error("Failed to run migration for tenant: {}", tenantDomain, e);
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
                    // Chạy migration để đảm bảo schema up-to-date
                    runTenantMigration(tenantDomain);
                    // Add vào routing DataSource
                    addToRoutingDataSource(tenantDomain);
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
