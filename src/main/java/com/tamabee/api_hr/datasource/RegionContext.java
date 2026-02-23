package com.tamabee.api_hr.datasource;

/**
 * ThreadLocal holder cho region code của company.
 * Mỗi request có region riêng, không ảnh hưởng request khác.
 * Region được set từ JWT claim "region" trong JwtAuthenticationFilter.
 * Dùng bởi RegionAwareAuditListener để xác định timezone cho createdAt/updatedAt.
 */
public class RegionContext {

    private static final ThreadLocal<String> CURRENT_REGION = new ThreadLocal<>();

    /**
     * Set region cho request hiện tại.
     *
     * @param region mã region (vi, ja)
     */
    public static void setCurrentRegion(String region) {
        CURRENT_REGION.set(region);
    }

    /**
     * Lấy region của request hiện tại.
     *
     * @return region code hoặc null nếu chưa được set (system operation, không có JWT)
     */
    public static String getCurrentRegion() {
        return CURRENT_REGION.get();
    }

    /**
     * Clear region context sau khi request hoàn thành.
     * QUAN TRỌNG: Phải gọi method này trong finally block để tránh memory leak.
     */
    public static void clear() {
        CURRENT_REGION.remove();
    }
}
