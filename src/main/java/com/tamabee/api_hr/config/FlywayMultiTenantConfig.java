package com.tamabee.api_hr.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Cấu hình Flyway cho multi-tenant architecture.
 * - Master DB: Chứa companies, users, plans, wallets, etc.
 * - Tenant DB: Chứa HR data (attendance, payroll, leaves, etc.)
 */
@Configuration
@Slf4j
public class FlywayMultiTenantConfig {

    @Value("${spring.flyway.target:}")
    private String flywayTarget;

    /**
     * Flyway cho Master Database.
     * Chạy migrations từ db/master/
     * Sử dụng masterDataSource cụ thể, không phải TenantRoutingDataSource.
     */
    @Bean
    @Primary
    @DependsOn("masterDataSource")
    public Flyway masterFlyway(@Qualifier("masterDataSource") DataSource masterDataSource) {
        log.info("Khởi tạo Flyway cho Master Database");

        FluentConfiguration config = Flyway.configure()
                .dataSource(masterDataSource)
                .locations("classpath:db/master")
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .defaultSchema("public")
                .createSchemas(true);

        // Áp dụng target version nếu được cấu hình
        if (flywayTarget != null && !flywayTarget.isEmpty()) {
            config.target(flywayTarget);
        }

        Flyway flyway = config.load();
        // flyway.clean();
        flyway.migrate();

        log.info("Hoàn thành migration cho Master Database");
        return flyway;
    }

    /**
     * Chạy migration cho một tenant database cụ thể.
     * Được gọi bởi TenantDatabaseInitializer khi tạo tenant mới.
     * 
     * @param jdbcUrl  JDBC URL của tenant database
     * @param username Database username
     * @param password Database password
     */
    public void migrateTenantDatabase(String jdbcUrl, String username, String password) {
        log.info("Chạy migration cho Tenant Database: {}", jdbcUrl);

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/tenant")
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .load();

        // flyway.clean();
        flyway.migrate();

        log.info("Hoàn thành migration cho Tenant Database: {}", jdbcUrl);
    }

    /**
     * Chạy migration cho tenant database với DataSource.
     * 
     * @param tenantDataSource DataSource của tenant
     */
    public void migrateTenantDatabase(DataSource tenantDataSource) {
        log.info("Chạy migration cho Tenant Database với DataSource");

        Flyway flyway = Flyway.configure()
                .dataSource(tenantDataSource)
                .locations("classpath:db/tenant")
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .load();

        // flyway.clean();
        flyway.migrate();

        log.info("Hoàn thành migration cho Tenant Database");
    }

    /**
     * Lấy migration location cho Master DB.
     */
    public static String getMasterMigrationLocation() {
        return "classpath:db/master";
    }

    /**
     * Lấy migration location cho Tenant DB.
     */
    public static String getTenantMigrationLocation() {
        return "classpath:db/tenant";
    }
}
