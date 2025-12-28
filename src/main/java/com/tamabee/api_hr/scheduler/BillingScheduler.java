package com.tamabee.api_hr.scheduler;

import com.tamabee.api_hr.service.admin.IBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job cho billing tự động hàng tháng
 * Chạy vào 00:00 mỗi ngày để kiểm tra và xử lý billing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillingScheduler {

    private final IBillingService billingService;

    /**
     * Xử lý billing hàng tháng
     * Chạy vào 00:00 mỗi ngày (cron: giây phút giờ ngày tháng thứ)
     * Kiểm tra các company có nextBillingDate <= now và đã hết free trial
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processMonthlyBilling() {
        log.info("=== BÁT ĐẦU SCHEDULED JOB: Monthly Billing ===");
        try {
            billingService.processMonthlyBilling();
            log.info("=== KẾT THÚC SCHEDULED JOB: Monthly Billing - THÀNH CÔNG ===");
        } catch (Exception e) {
            log.error("=== KẾT THÚC SCHEDULED JOB: Monthly Billing - LỖI: {} ===", e.getMessage(), e);
        }
    }
}
