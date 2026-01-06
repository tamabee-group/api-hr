package com.tamabee.api_hr.datasource;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý DataSource pool cho tất cả tenants.
 * Hỗ trợ thêm/xóa tenant runtime.
 * Luôn có sẵn "tamabee" DataSource cho Tamabee company.
 */
@Component
@Slf4j
public class TenantDataSourceManager {

    private static final String TAMABEE_TENANT = "tamabee";
    private static final String DATABASE_PREFIX = "tamabee_";

    private final Map<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    /**
     * Thêm DataSource cho tenant mới.
     * Database name format: tamabee_{tenantDomain}
     * 
     * @param tenantDomain domain của tenant (ví dụ: "acme")
     */
    public void addTenant(String tenantDomain) {
        if (tenantDataSources.containsKey(tenantDomain)) {
            log.warn("Tenant DataSource already exists: {}", tenantDomain);
            return;
        }

        DataSource dataSource = createDataSource(tenantDomain);
        tenantDataSources.put(tenantDomain, dataSource);
        log.info("Added tenant DataSource: {}", tenantDomain);
    }

    /**
     * Lấy DataSource của tenant.
     * 
     * @param tenantDomain domain của tenant
     * @return DataSource hoặc null nếu không tồn tại
     */
    public DataSource getDataSource(String tenantDomain) {
        return tenantDataSources.get(tenantDomain);
    }

    /**
     * Xóa DataSource của tenant khỏi pool.
     * Sử dụng khi company bị deactivate.
     * 
     * @param tenantDomain domain của tenant
     */
    public void removeTenant(String tenantDomain) {
        if (TAMABEE_TENANT.equals(tenantDomain)) {
            log.warn("Cannot remove Tamabee tenant DataSource");
            return;
        }

        DataSource removed = tenantDataSources.remove(tenantDomain);
        if (removed != null) {
            closeDataSource(removed);
            log.info("Removed tenant DataSource: {}", tenantDomain);
        }
    }

    /**
     * Lấy tất cả tenant DataSources.
     * Sử dụng để cấu hình TenantRoutingDataSource.
     * 
     * @return Map của tenantDomain -> DataSource
     */
    public Map<String, DataSource> getAllDataSources() {
        return new ConcurrentHashMap<>(tenantDataSources);
    }

    /**
     * Kiểm tra tenant DataSource có tồn tại không.
     * 
     * @param tenantDomain domain của tenant
     * @return true nếu tồn tại
     */
    public boolean hasTenant(String tenantDomain) {
        return tenantDataSources.containsKey(tenantDomain);
    }

    /**
     * Tạo DataSource cho tenant.
     * URL format: jdbc:postgresql://host:port/tamabee_{tenantDomain}
     */
    private DataSource createDataSource(String tenantDomain) {
        String dbName = DATABASE_PREFIX + tenantDomain;
        String tenantDbUrl = buildTenantDbUrl(dbName);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(tenantDbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setPoolName("tenant-" + tenantDomain);

        // Connection pool settings
        dataSource.setMinimumIdle(2);
        dataSource.setMaximumPoolSize(10);
        dataSource.setIdleTimeout(300000); // 5 minutes
        dataSource.setMaxLifetime(600000); // 10 minutes
        dataSource.setConnectionTimeout(30000); // 30 seconds

        log.debug("Created DataSource for tenant: {} with URL: {}", tenantDomain, tenantDbUrl);
        return dataSource;
    }

    /**
     * Build tenant database URL từ master URL.
     * Thay thế database name trong URL.
     */
    private String buildTenantDbUrl(String dbName) {
        // URL format: jdbc:postgresql://host:port/database
        int lastSlash = masterDbUrl.lastIndexOf('/');
        if (lastSlash == -1) {
            throw new IllegalStateException("Invalid master database URL: " + masterDbUrl);
        }
        return masterDbUrl.substring(0, lastSlash + 1) + dbName;
    }

    /**
     * Đóng DataSource khi remove tenant.
     */
    private void closeDataSource(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            hikariDataSource.close();
        }
    }
}
