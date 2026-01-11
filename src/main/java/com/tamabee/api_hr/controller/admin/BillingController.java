package com.tamabee.api_hr.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.wallet.WalletRepository;
import com.tamabee.api_hr.service.admin.interfaces.IBillingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller để test và trigger billing thủ công
 * Chỉ dành cho ADMIN_TAMABEE
 */
@RestController
@RequestMapping("/api/admin/billing")
@PreAuthorize("hasRole('ADMIN_TAMABEE')")
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final IBillingService billingService;
    private final WalletRepository walletRepository;

    /**
     * Trigger billing thủ công cho tất cả company đến hạn
     * POST /api/admin/billing/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<BaseResponse<String>> triggerBilling() {
        log.info("=== TRIGGER BILLING THỦ CÔNG ===");
        billingService.processMonthlyBilling();
        return ResponseEntity.ok(BaseResponse.success("Billing đã được xử lý", "Billing processed successfully"));
    }

    /**
     * Kiểm tra company có đang trong free trial không
     * GET /api/admin/billing/free-trial/{companyId}
     */
    @GetMapping("/free-trial/{companyId}")
    public ResponseEntity<BaseResponse<Boolean>> checkFreeTrial(@PathVariable Long companyId) {
        boolean isInFreeTrial = billingService.isInFreeTrial(companyId);
        return ResponseEntity.ok(BaseResponse.success(isInFreeTrial, "Free trial status"));
    }

    /**
     * Setup test data cho billing - set free trial end date và next billing date
     * POST /api/admin/billing/setup-test/{companyId}?daysAgo=1&balance=10000
     */
    @PostMapping("/setup-test/{companyId}")
    public ResponseEntity<BaseResponse<String>> setupTestData(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "1") int daysAgo,
            @RequestParam(defaultValue = "10000") BigDecimal balance) {
        
        WalletEntity wallet = walletRepository.findByCompanyId(companyId)
                .orElseThrow(() -> NotFoundException.wallet(companyId));

        LocalDateTime targetDate = LocalDateTime.now().minusDays(daysAgo);
        wallet.setFreeTrialEndDate(targetDate);
        wallet.setNextBillingDate(targetDate);
        wallet.setBalance(balance);
        walletRepository.save(wallet);

        log.info("Setup test data cho company {}: freeTrialEndDate={}, nextBillingDate={}, balance={}",
                companyId, targetDate, targetDate, balance);

        return ResponseEntity.ok(BaseResponse.success(
                String.format("Setup thành công: freeTrialEndDate=%s, balance=%s", targetDate, balance),
                "Test data setup successfully"));
    }
}
