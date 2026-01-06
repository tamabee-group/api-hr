package com.tamabee.api_hr.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Tạo database mới cho tenant.
 * Chạy Flyway migration và insert default data.
 * Database tamabee_tamabee được tạo sẵn cho Tamabee.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDatabaseInitializer {

    private static final String DATABASE_PREFIX = "tamabee_";
    private static final String TENANT_MIGRATION_LOCATION = "classpath:db/tenant";

    private final TenantDataSourceManager tenantDataSourceManager;

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    /**
     * Tạo database mới cho tenant và chạy migration.
     * 
     * @param tenantDomain domain của tenant (ví dụ: "acme")
     * @throws TenantDatabaseException nếu tạo database thất bại
     */
    public void createTenantDatabase(String tenantDomain) {
        String dbName = DATABASE_PREFIX + tenantDomain;
        log.info("Creating tenant database: {}", dbName);

        try {
            // 1. Tạo database
            createDatabase(dbName);

            // 2. Thêm DataSource vào pool
            tenantDataSourceManager.addTenant(tenantDomain);

            // 3. Chạy Flyway migration
            runMigrations(tenantDomain);

            log.info("Successfully created tenant database: {}", dbName);
        } catch (Exception e) {
            log.error("Failed to create tenant database: {}", dbName, e);
            // Cleanup nếu thất bại
            tenantDataSourceManager.removeTenant(tenantDomain);
            throw new TenantDatabaseException("Failed to create tenant database: " + dbName, e);
        }
    }

    /**
     * Tạo database mới trong PostgreSQL.
     * Sử dụng master connection để CREATE DATABASE.
     */
    private void createDatabase(String dbName) throws SQLException {
        // Kết nối đến postgres database để tạo database mới
        String postgresUrl = buildPostgresDbUrl();

        try (Connection conn = java.sql.DriverManager.getConnection(postgresUrl, dbUsername, dbPassword);
                Statement stmt = conn.createStatement()) {

            // Kiểm tra database đã tồn tại chưa
            String checkSql = String.format(
                    "SELECT 1 FROM pg_database WHERE datname = '%s'", dbName);
            var rs = stmt.executeQuery(checkSql);

            if (rs.next()) {
                log.info("Database already exists: {}", dbName);
                return;
            }

            // Tạo database mới
            String createSql = String.format("CREATE DATABASE %s", dbName);
            stmt.executeUpdate(createSql);
            log.info("Created database: {}", dbName);
        }
    }

    /**
     * Chạy Flyway migration cho tenant database.
     */
    private void runMigrations(String tenantDomain) {
        DataSource dataSource = tenantDataSourceManager.getDataSource(tenantDomain);
        if (dataSource == null) {
            throw new TenantDatabaseException("DataSource not found for tenant: " + tenantDomain);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(TENANT_MIGRATION_LOCATION)
                .baselineOnMigrate(true)
                .cleanDisabled(true)
                .load();

        flyway.migrate();
        log.info("Completed Flyway migration for tenant: {}", tenantDomain);
    }

    /**
     * Build URL để kết nối đến postgres database (để tạo database mới).
     */
    private String buildPostgresDbUrl() {
        int lastSlash = masterDbUrl.lastIndexOf('/');
        if (lastSlash == -1) {
            throw new IllegalStateException("Invalid master database URL: " + masterDbUrl);
        }
        return masterDbUrl.substring(0, lastSlash + 1) + "postgres";
    }

    /**
     * Kiểm tra tenant database đã tồn tại chưa.
     * 
     * @param tenantDomain domain của tenant
     * @return true nếu database đã tồn tại
     */
    public boolean databaseExists(String tenantDomain) {
        String dbName = DATABASE_PREFIX + tenantDomain;
        String postgresUrl = buildPostgresDbUrl();

        try (Connection conn = java.sql.DriverManager.getConnection(postgresUrl, dbUsername, dbPassword);
                Statement stmt = conn.createStatement()) {

            String checkSql = String.format(
                    "SELECT 1 FROM pg_database WHERE datname = '%s'", dbName);
            var rs = stmt.executeQuery(checkSql);
            return rs.next();
        } catch (SQLException e) {
            log.error("Failed to check database existence: {}", dbName, e);
            return false;
        }
    }

    /**
     * Exception cho các lỗi liên quan đến tenant database.
     */
    public static class TenantDatabaseException extends RuntimeException {
        public TenantDatabaseException(String message) {
            super(message);
        }

        public TenantDatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
