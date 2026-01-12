package com.tamabee.api_hr.service.core.interfaces;

/**
 * Service khởi tạo dữ liệu mặc định cho hệ thống.
 */
public interface IDataInitializerService {

    /**
     * Tạo Tamabee company và wallet trong master DB.
     */
    void createTamabeeCompanyIfNotExists();

    /**
     * Tạo admin user trong tenant DB.
     * QUAN TRỌNG: TenantContext phải được set TRƯỚC khi gọi method này.
     */
    void createDefaultAdminIfNotExists();
}
