package com.tamabee.api_hr.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tamabee.api_hr.datasource.TenantDataSourceManager;
import com.tamabee.api_hr.datasource.TenantRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Cấu hình DataSource cho multi-tenant.
 * TenantRoutingDataSource sẽ route queries đến đúng tenant database
 * dựa trên TenantContext được set bởi TenantFilter.
 */
@Configuration
@Slf4j
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    /**
     * Master DataSource - dùng cho master database (users, companies, plans,
     * wallets...).
     */
    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        log.info("Creating Master DataSource: {}", masterDbUrl);
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(masterDbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setPoolName("master-pool");
        dataSource.setMinimumIdle(2);
        dataSource.setMaximumPoolSize(10);
        return dataSource;
    }

    /**
     * JdbcTemplate cho master database.
     * Dùng khi cần query master DB từ tenant context.
     */
    @Bean(name = "masterJdbcTemplate")
    public JdbcTemplate masterJdbcTemplate(@Qualifier("masterDataSource") DataSource masterDataSource) {
        return new JdbcTemplate(masterDataSource);
    }

    /**
     * TenantRoutingDataSource - Primary DataSource cho application.
     * Route queries đến đúng tenant database dựa trên TenantContext.
     * Khi TenantContext = null, sẽ dùng master database (default).
     */
    @Bean
    @Primary
    @DependsOn("tenantDataSourceManager")
    public DataSource dataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            TenantDataSourceManager tenantDataSourceManager) {
        log.info("Creating TenantRoutingDataSource");

        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource();

        // Default DataSource khi không có tenant context (master DB)
        routingDataSource.setDefaultTargetDataSource(masterDataSource);

        // Target DataSources - ban đầu chỉ có master
        // TenantDataSourceLoader sẽ add thêm tenant DataSources sau khi app ready
        Map<Object, Object> targetDataSources = new HashMap<>();

        // Add existing tenant DataSources nếu có
        Map<String, DataSource> tenantSources = tenantDataSourceManager.getAllDataSources();
        targetDataSources.putAll(tenantSources);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.afterPropertiesSet();

        log.info("TenantRoutingDataSource created with {} tenant(s), default = master DB", tenantSources.size());
        return routingDataSource;
    }
}
