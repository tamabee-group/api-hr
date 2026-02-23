package com.tamabee.api_hr.scheduler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;
import com.tamabee.api_hr.service.core.interfaces.IEmailService;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job để xóa vĩnh viễn các company INACTIVE sau thời gian retention
 * Chạy vào 01:00 mỗi ngày
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyCleanupScheduler {

    private final CompanyRepository companyRepository;
    private final ISettingService settingService;
    private final IEmailService emailService;

    /**
     * Xóa vĩnh viễn các company INACTIVE đã quá thời gian retention
     * Chạy vào 01:00 mỗi ngày
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void cleanupInactiveCompanies() {
        log.info("=== BẮT ĐẦU SCHEDULED JOB: Company Cleanup ===");
        
        List<String> deletedCompanyNames = new ArrayList<>();
        int retentionDays = 30;
        
        try {
            retentionDays = settingService.getInactiveRetentionDays();
            LocalDateTime cutoffDate = LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())).minusDays(retentionDays);
            
            List<CompanyEntity> companiesToDelete = companyRepository.findInactiveCompaniesForCleanup(cutoffDate);
            log.info("Tìm thấy {} companies INACTIVE cần xóa (retention: {} ngày)", 
                    companiesToDelete.size(), retentionDays);

            for (CompanyEntity company : companiesToDelete) {
                try {
                    // Soft delete company
                    company.setDeleted(true);
                    companyRepository.save(company);
                    deletedCompanyNames.add(company.getName());
                    log.info("Đã xóa company: {} (ID: {})", company.getName(), company.getId());
                } catch (Exception e) {
                    log.error("Lỗi khi xóa company {}: {}", company.getId(), e.getMessage());
                }
            }

            log.info("=== KẾT THÚC SCHEDULED JOB: Company Cleanup - Đã xóa {} companies ===", 
                    deletedCompanyNames.size());
            
            // Gửi email báo cáo cho admin
            emailService.sendCleanupReport(deletedCompanyNames, retentionDays);
            
        } catch (Exception e) {
            log.error("=== KẾT THÚC SCHEDULED JOB: Company Cleanup - LỖI: {} ===", e.getMessage(), e);
        }
    }
}
