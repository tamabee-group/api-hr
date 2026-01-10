package com.tamabee.api_hr.datasource;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service orchestrate việc tạo tenant database.
 * Bao gồm: tạo database, chạy migration, đăng ký DataSource.
 * Hỗ trợ async provisioning để không block request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final TenantDatabaseInitializer tenantDatabaseInitializer;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final javax.sql.DataSource dataSource; // TenantRoutingDataSource (primary)

    /**
     * Provision tenant database đồng bộ.
     * Sử dụng khi cần đảm bảo database sẵn sàng ngay sau khi tạo company.
     *
     * @param tenantDomain domain của tenant (ví dụ: "acme")
     * @throws TenantProvisioningException nếu provisioning thất bại
     */
    public void provisionTenant(String tenantDomain) {
        log.info("Starting tenant provisioning for: {}", tenantDomain);

        try {
            // Kiểm tra tenant đã tồn tại chưa
            if (tenantDataSourceManager.hasTenant(tenantDomain)) {
                log.warn("Tenant already provisioned: {}", tenantDomain);
                // Đảm bảo đã add vào routing
                addToRoutingDataSource(tenantDomain);
                return;
            }

            // Tạo database và chạy migration
            tenantDatabaseInitializer.createTenantDatabase(tenantDomain);

            // Add vào TenantRoutingDataSource để có thể route ngay lập tức
            addToRoutingDataSource(tenantDomain);

            log.info("Successfully provisioned tenant: {}", tenantDomain);
        } catch (Exception e) {
            log.error("Failed to provision tenant: {}", tenantDomain, e);
            throw new TenantProvisioningException("Failed to provision tenant: " + tenantDomain, e);
        }
    }

    /**
     * Add tenant DataSource vào TenantRoutingDataSource.
     */
    private void addToRoutingDataSource(String tenantDomain) {
        if (dataSource instanceof TenantRoutingDataSource routingDataSource) {
            javax.sql.DataSource tenantDs = tenantDataSourceManager.getDataSource(tenantDomain);
            if (tenantDs != null) {
                routingDataSource.addTenantDataSource(tenantDomain, tenantDs);
                log.info("Added tenant {} to TenantRoutingDataSource", tenantDomain);
            }
        }
    }

    /**
     * Provision tenant database bất đồng bộ.
     * Sử dụng khi không cần đợi database sẵn sàng ngay.
     *
     * @param tenantDomain domain của tenant
     * @return CompletableFuture để theo dõi trạng thái
     */
    @Async
    public CompletableFuture<ProvisioningResult> provisionTenantAsync(String tenantDomain) {
        log.info("Starting async tenant provisioning for: {}", tenantDomain);

        try {
            provisionTenant(tenantDomain);
            return CompletableFuture.completedFuture(
                    ProvisioningResult.success(tenantDomain));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    ProvisioningResult.failure(tenantDomain, e.getMessage()));
        }
    }

    /**
     * Kiểm tra tenant đã được provision chưa.
     *
     * @param tenantDomain domain của tenant
     * @return true nếu tenant đã có DataSource
     */
    public boolean isProvisioned(String tenantDomain) {
        return tenantDataSourceManager.hasTenant(tenantDomain);
    }

    /**
     * Deprovision tenant - xóa DataSource khỏi pool.
     * Database vẫn được giữ lại để compliance.
     *
     * @param tenantDomain domain của tenant
     */
    public void deprovisionTenant(String tenantDomain) {
        log.info("Deprovisioning tenant: {}", tenantDomain);
        tenantDataSourceManager.removeTenant(tenantDomain);
    }

    /**
     * Xóa hoàn toàn tenant - xóa cả DataSource và database.
     * Dùng cho rollback khi tạo company thất bại.
     *
     * @param tenantDomain domain của tenant
     */
    public void dropTenant(String tenantDomain) {
        log.info("Dropping tenant completely: {}", tenantDomain);
        tenantDatabaseInitializer.dropTenantDatabase(tenantDomain);
    }

    /**
     * Kết quả của quá trình provisioning.
     */
    public record ProvisioningResult(
            String tenantDomain,
            boolean success,
            String errorMessage) {

        public static ProvisioningResult success(String tenantDomain) {
            return new ProvisioningResult(tenantDomain, true, null);
        }

        public static ProvisioningResult failure(String tenantDomain, String errorMessage) {
            return new ProvisioningResult(tenantDomain, false, errorMessage);
        }
    }

    /**
     * Exception cho các lỗi provisioning.
     */
    public static class TenantProvisioningException extends RuntimeException {
        public TenantProvisioningException(String message) {
            super(message);
        }

        public TenantProvisioningException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
