package com.tamabee.api_hr.constants;

/**
 * Constants cho Plan IDs
 * Tập trung quản lý để tránh hardcode ở nhiều nơi
 */
public final class PlanConstants {

    private PlanConstants() {
    }

    /**
     * Free Plan - Company mới đăng ký và Tamabee được gán plan này
     * Trong thời gian trial, được dùng full tính năng
     */
    public static final Long FREE_PLAN_ID = 0L;

    /**
     * Tamabee tenant domain
     */
    public static final String TAMABEE_TENANT = "tamabee";

    /**
     * Số năm free trial cho Tamabee (10 năm)
     */
    public static final int TAMABEE_FREE_TRIAL_YEARS = 10;
}
