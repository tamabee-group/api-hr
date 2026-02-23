package com.tamabee.api_hr.controller.system_admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.wallet.WalletRepository;
import com.tamabee.api_hr.scheduler.CompanyCleanupScheduler;
import com.tamabee.api_hr.scheduler.ContractExpiryScheduler;
import com.tamabee.api_hr.scheduler.PayrollScheduler;
import com.tamabee.api_hr.scheduler.TenantCleanupScheduler;
import com.tamabee.api_hr.service.admin.interfaces.IBillingService;
import com.tamabee.api_hr.service.company.interfaces.IPayrollPeriodService;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller quản lý scheduled jobs và system admin tasks (chỉ dành cho Tamabee admin)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/schedulers")
@PreAuthorize("hasRole('ADMIN_TAMABEE')")
@RequiredArgsConstructor
public class SchedulerController {

    private final PayrollScheduler payrollScheduler;
    private final ContractExpiryScheduler contractExpiryScheduler;
    private final CompanyCleanupScheduler companyCleanupScheduler;
    private final TenantCleanupScheduler tenantCleanupScheduler;
    private final IBillingService billingService;
    private final WalletRepository walletRepository;
    private final CompanyRepository companyRepository;
    private final IPayrollPeriodService payrollPeriodService;

    /**
     * Chạy thủ công job xử lý payroll payment day
     * Kiểm tra và update status payroll periods sang PAID vào ngày trả lương
     */
    @PostMapping("/payroll-payment")
    public ResponseEntity<BaseResponse<String>> runPayrollPaymentScheduler() {
        log.info("Admin trigger: Chạy thủ công payroll payment scheduler");
        
        try {
            payrollScheduler.processPayrollPaymentDay();
            return ResponseEntity.ok(
                BaseResponse.success("Đã chạy payroll payment scheduler thành công", 
                    "Scheduler completed")
            );
        } catch (Exception e) {
            log.error("Lỗi khi chạy payroll payment scheduler: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                BaseResponse.error("Lỗi khi chạy scheduler: " + e.getMessage(), "SCHEDULER_ERROR")
            );
        }
    }

    /**
     * Trigger billing thủ công cho tất cả company đến hạn
     * POST /api/admin/schedulers/billing
     */
    @PostMapping("/billing")
    public ResponseEntity<BaseResponse<String>> triggerBilling() {
        log.info("Admin trigger: Chạy thủ công billing scheduler");
        
        try {
            billingService.processMonthlyBilling();
            return ResponseEntity.ok(
                BaseResponse.success("Đã xử lý billing thành công", "Billing processed successfully")
            );
        } catch (Exception e) {
            log.error("Lỗi khi chạy billing scheduler: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                BaseResponse.error("Lỗi khi chạy billing: " + e.getMessage(), "BILLING_ERROR")
            );
        }
    }

    /**
     * Kiểm tra company có đang trong free trial không
     * GET /api/admin/schedulers/free-trial/{companyId}
     */
    @GetMapping("/free-trial/{companyId}")
    public ResponseEntity<BaseResponse<Boolean>> checkFreeTrial(@PathVariable Long companyId) {
        boolean isInFreeTrial = billingService.isInFreeTrial(companyId);
        return ResponseEntity.ok(BaseResponse.success(isInFreeTrial, "Free trial status"));
    }

    /**
     * Setup test data cho billing - set free trial end date và next billing date
     * POST /api/admin/schedulers/setup-test/{companyId}?daysAgo=1&balance=10000
     */
    @PostMapping("/setup-test/{companyId}")
    public ResponseEntity<BaseResponse<String>> setupTestData(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "1") int daysAgo,
            @RequestParam(defaultValue = "10000") BigDecimal balance) {
        
        WalletEntity wallet = walletRepository.findByCompanyId(companyId)
                .orElseThrow(() -> NotFoundException.wallet(companyId));

        LocalDateTime targetDate = LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())).minusDays(daysAgo);
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

    /**
     * Chạy thủ công job kiểm tra hợp đồng hết hạn
     * POST /api/admin/schedulers/contract-expiry
     */
    @PostMapping("/contract-expiry")
    public ResponseEntity<BaseResponse<String>> runContractExpiryScheduler() {
        log.info("Admin trigger: Chạy thủ công contract expiry scheduler");
        
        try {
            contractExpiryScheduler.processExpiredContracts();
            return ResponseEntity.ok(
                BaseResponse.success("Đã chạy contract expiry scheduler thành công",
                    "Scheduler completed")
            );
        } catch (Exception e) {
            log.error("Lỗi khi chạy contract expiry scheduler: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                BaseResponse.error("Lỗi khi chạy scheduler: " + e.getMessage(), "SCHEDULER_ERROR")
            );
        }
    }

    /**
     * Chạy thủ công job dọn dẹp company inactive
     * POST /api/admin/schedulers/company-cleanup
     */
    @PostMapping("/company-cleanup")
    public ResponseEntity<BaseResponse<String>> runCompanyCleanupScheduler() {
        log.info("Admin trigger: Chạy thủ công company cleanup scheduler");
        
        try {
            companyCleanupScheduler.cleanupInactiveCompanies();
            return ResponseEntity.ok(
                BaseResponse.success("Đã chạy company cleanup scheduler thành công",
                    "Scheduler completed")
            );
        } catch (Exception e) {
            log.error("Lỗi khi chạy company cleanup scheduler: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                BaseResponse.error("Lỗi khi chạy scheduler: " + e.getMessage(), "SCHEDULER_ERROR")
            );
        }
    }

    /**
     * Chạy thủ công job dọn dẹp tenant databases
     * POST /api/admin/schedulers/tenant-cleanup
     */
    @PostMapping("/tenant-cleanup")
    public ResponseEntity<BaseResponse<String>> runTenantCleanupScheduler() {
        log.info("Admin trigger: Chạy thủ công tenant cleanup scheduler");
        
        try {
            tenantCleanupScheduler.cleanupInactiveTenants();
            return ResponseEntity.ok(
                BaseResponse.success("Đã chạy tenant cleanup scheduler thành công",
                    "Scheduler completed")
            );
        } catch (Exception e) {
            log.error("Lỗi khi chạy tenant cleanup scheduler: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                BaseResponse.error("Lỗi khi chạy scheduler: " + e.getMessage(), "SCHEDULER_ERROR")
            );
        }
    }

    /**
     * Lấy danh sách tất cả companies (tenants)
     * GET /api/admin/schedulers/tenants
     */
    @GetMapping("/tenants")
    public ResponseEntity<BaseResponse<List<TenantInfo>>> getAllTenants() {
        List<CompanyEntity> companies = companyRepository.findAllByDeletedFalse();
        
        List<TenantInfo> tenants = companies.stream()
                .map(company -> new TenantInfo(
                    company.getId(),
                    company.getName(),
                    company.getTenantDomain()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(BaseResponse.success(tenants, "Tenants retrieved successfully"));
    }

    /**
     * Rollback payroll periods về trạng thái DRAFT (dùng cho testing)
     * POST /api/admin/schedulers/rollback-payroll?companyId=1&year=2025&month=1
     * 
     * @param companyId ID công ty (optional, null = tất cả công ty)
     * @param year      Năm (optional, null = tất cả năm)
     * @param month     Tháng (optional, null = tất cả tháng)
     */
    @PostMapping("/rollback-payroll")
    public ResponseEntity<BaseResponse<RollbackResult>> rollbackPayroll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        log.info("Admin trigger: Rollback payroll - companyId={}, year={}, month={}", companyId, year, month);
        
        try {
            int count = payrollPeriodService.rollbackPayrollPeriodsToDraft(companyId, year, month);
            
            String message = String.format("Đã rollback %d payroll periods về DRAFT", count);
            RollbackResult result = new RollbackResult(count, companyId, year, month);
            
            return ResponseEntity.ok(BaseResponse.success(result, message));
            
        } catch (Exception e) {
            log.error("Lỗi khi rollback payroll: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                BaseResponse.error("Lỗi khi rollback: " + e.getMessage(), "ROLLBACK_ERROR")
            );
        }
    }

    /**
     * DTO cho tenant info
     */
    public record TenantInfo(
        Long id,
        String name,
        String tenantDomain
    ) {}

    /**
     * DTO cho rollback result
     */
    public record RollbackResult(
        int count,
        Long companyId,
        Integer year,
        Integer month
    ) {}
}
