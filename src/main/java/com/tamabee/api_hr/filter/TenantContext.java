package com.tamabee.api_hr.filter;

/**
 * ThreadLocal holder cho tenant domain.
 * Mỗi request có tenant riêng, không ảnh hưởng request khác.
 * Sử dụng để lưu trữ tenantDomain trong suốt lifecycle của request.
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Set tenant domain cho request hiện tại.
     * 
     * @param tenantDomain domain của tenant (ví dụ: "tamabee", "acme")
     */
    public static void setCurrentTenant(String tenantDomain) {
        CURRENT_TENANT.set(tenantDomain);
    }

    /**
     * Lấy tenant domain của request hiện tại.
     * 
     * @return tenantDomain hoặc null nếu chưa được set
     */
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /**
     * Clear tenant context sau khi request hoàn thành.
     * QUAN TRỌNG: Phải gọi method này trong finally block để tránh memory leak.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
