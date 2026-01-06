package com.tamabee.api_hr.datasource;

import com.tamabee.api_hr.filter.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * DataSource routing dựa trên TenantContext.
 * Spring tự động gọi determineCurrentLookupKey() trước mỗi query.
 * "tamabee" → tamabee_tamabee database
 * "acme" → tamabee_acme database
 */
@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    /**
     * Xác định tenant key để route đến đúng DataSource.
     * Method này được Spring gọi tự động trước mỗi database operation.
     * 
     * @return tenantDomain từ TenantContext, hoặc null nếu không có tenant
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String tenant = TenantContext.getCurrentTenant();
        log.debug("Routing to tenant database: {}", tenant);
        return tenant;
    }
}
