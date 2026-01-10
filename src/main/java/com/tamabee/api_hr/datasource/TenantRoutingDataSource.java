package com.tamabee.api_hr.datasource;

import com.tamabee.api_hr.filter.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * DataSource routing dựa trên TenantContext.
 * Spring tự động gọi determineCurrentLookupKey() trước mỗi query.
 * "tamabee" → tamabee_tamabee database
 * "acme" → tamabee_acme database
 */
@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> tenantDataSources = new HashMap<>();

    /**
     * Xác định tenant key để route đến đúng DataSource.
     * Method này được Spring gọi tự động trước mỗi database operation.
     * 
     * @return tenantDomain từ TenantContext, hoặc null nếu không có tenant (dùng
     *         default)
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String tenant = TenantContext.getCurrentTenant();
        log.debug("Routing to tenant database: {}", tenant);
        return tenant;
    }

    /**
     * Thêm tenant DataSource vào routing map.
     * Gọi method này khi có tenant mới được tạo hoặc load.
     */
    public void addTenantDataSource(String tenantDomain, DataSource dataSource) {
        tenantDataSources.put(tenantDomain, dataSource);
        setTargetDataSources(tenantDataSources);
        afterPropertiesSet(); // Refresh resolved DataSources
        log.info("Added tenant DataSource to routing: {}", tenantDomain);
    }

    /**
     * Xóa tenant DataSource khỏi routing map.
     */
    public void removeTenantDataSource(String tenantDomain) {
        tenantDataSources.remove(tenantDomain);
        setTargetDataSources(tenantDataSources);
        afterPropertiesSet();
        log.info("Removed tenant DataSource from routing: {}", tenantDomain);
    }

    /**
     * Khởi tạo với default DataSource.
     */
    public void initializeWithDefault(DataSource defaultDataSource) {
        setDefaultTargetDataSource(defaultDataSource);
        setTargetDataSources(tenantDataSources);
        afterPropertiesSet();
    }
}
