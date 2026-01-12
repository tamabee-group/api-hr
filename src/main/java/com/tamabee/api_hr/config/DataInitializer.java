package com.tamabee.api_hr.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.tamabee.api_hr.constants.PlanConstants.TAMABEE_TENANT;
import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.service.core.interfaces.IDataInitializerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Khởi tạo dữ liệu mặc định cho hệ thống.
 * Chạy sau khi TenantDataSourceLoader đã load xong tenant DataSources.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final IDataInitializerService dataInitializerService;

    /**
     * Khởi tạo dữ liệu sau khi app ready và tenant DataSources đã được load.
     * Order = 10 để chạy sau TenantDataSourceLoader (không có order = default).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void initializeData() {
        // Tạo Tamabee company và wallet trong master DB trước (không cần tenant context)
        dataInitializerService.createTamabeeCompanyIfNotExists();
        
        // Tạo admin user trong tenant DB (cần set tenant context TRƯỚC khi gọi method @Transactional)
        try {
            TenantContext.setCurrentTenant(TAMABEE_TENANT);
            dataInitializerService.createDefaultAdminIfNotExists();
        } finally {
            TenantContext.clear();
        }
    }
}
