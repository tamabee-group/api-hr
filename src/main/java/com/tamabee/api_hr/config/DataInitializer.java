package com.tamabee.api_hr.config;

import java.time.LocalDateTime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.tamabee.api_hr.constants.PlanConstants.FREE_PLAN_ID;
import static com.tamabee.api_hr.constants.PlanConstants.TAMABEE_FREE_TRIAL_YEARS;
import static com.tamabee.api_hr.constants.PlanConstants.TAMABEE_TENANT;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.filter.TenantContext;
import com.tamabee.api_hr.mapper.core.WalletFactory;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WalletRepository;
import com.tamabee.api_hr.util.EmployeeCodeGenerator;
import com.tamabee.api_hr.util.ReferralCodeGenerator;

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

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final WalletRepository walletRepository;
    private final WalletFactory walletFactory;
    private final PasswordEncoder passwordEncoder;

    /**
     * Khởi tạo dữ liệu sau khi app ready và tenant DataSources đã được load.
     * Order = 10 để chạy sau TenantDataSourceLoader (không có order = default).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void initializeData() {
        // Tạo Tamabee company và wallet trong master DB trước
        createTamabeeCompanyIfNotExists();
        
        // Set TenantContext để query đúng tenant DB
        try {
            TenantContext.setCurrentTenant(TAMABEE_TENANT);
            createDefaultAdminIfNotExists();
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Tạo hoặc cập nhật Tamabee company và wallet trong master DB.
     */
    private void createTamabeeCompanyIfNotExists() {
        CompanyEntity company = companyRepository.findByTenantDomainAndDeletedFalse(TAMABEE_TENANT)
                .orElse(null);
        
        if (company == null) {
            // Tạo mới Tamabee company
            company = new CompanyEntity();
            company.setName("Tamabee株式会社");
            company.setEmail("contact@tamabee.vn");
            company.setTenantDomain(TAMABEE_TENANT);
            company.setPlanId(FREE_PLAN_ID);
            company.setStatus(CompanyStatus.ACTIVE);
            company.setLocale("ja");
            company.setLanguage("ja");
            company.setOwnerName("Tamabee Admin");
            company.setDeleted(false);
            
            company = companyRepository.save(company);
            log.info("✅ Created Tamabee company with id: {}", company.getId());
        } else {
            // Cập nhật nếu thiếu thông tin (fix data cũ)
            boolean needUpdate = false;
            if (company.getPlanId() == null || !company.getPlanId().equals(FREE_PLAN_ID)) {
                company.setPlanId(FREE_PLAN_ID);
                needUpdate = true;
            }
            if (company.getStatus() == null) {
                company.setStatus(CompanyStatus.ACTIVE);
                needUpdate = true;
            }
            if (needUpdate) {
                company = companyRepository.save(company);
                log.info("✅ Updated Tamabee company with plan_id: {}", FREE_PLAN_ID);
            } else {
                log.info("ℹ️ Tamabee company already exists with id: {}", company.getId());
            }
        }
        
        // Kiểm tra và tạo wallet nếu chưa có
        if (!walletRepository.existsByCompanyIdAndDeletedFalse(company.getId())) {
            createWalletForTamabee(company.getId());
        }
    }

    /**
     * Tạo wallet cho Tamabee với free trial 10 năm
     */
    private void createWalletForTamabee(Long companyId) {
        LocalDateTime freeTrialEndDate = LocalDateTime.now().plusYears(TAMABEE_FREE_TRIAL_YEARS);
        WalletEntity wallet = walletFactory.createForCompany(companyId, freeTrialEndDate);
        walletRepository.save(wallet);
        log.info("✅ Created wallet for Tamabee with free trial until: {}", freeTrialEndDate);
    }

    private void createDefaultAdminIfNotExists() {
        String adminEmail = "hiepdeptrai0908@gmail.com";
        String adminDateOfBirth = "1997-09-08"; // Format yyyy-MM-dd

        if (!userRepository.existsByEmail(adminEmail)) {
            UserEntity admin = new UserEntity();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole(UserRole.ADMIN_TAMABEE);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setLocale("vi");
            admin.setLanguage("vi");
            admin.setTenantDomain(TAMABEE_TENANT);

            // Tạo mã nhân viên duy nhất
            String employeeCode = EmployeeCodeGenerator.generateUnique(0L, adminDateOfBirth, userRepository);
            admin.setEmployeeCode(employeeCode);

            // Tạo profile với mã giới thiệu
            UserProfileEntity profile = new UserProfileEntity();
            profile.setDateOfBirth(adminDateOfBirth);
            String referralCode;
            do {
                referralCode = ReferralCodeGenerator.generate();
            } while (userRepository.existsByProfileReferralCodeAndDeletedFalse(referralCode));
            profile.setReferralCode(referralCode);
            profile.setUser(admin);
            admin.setProfile(profile);

            // Tính % hoàn thiện profile
            admin.calculateProfileCompleteness();

            userRepository.save(admin);
            log.info("✅ Created default Tamabee admin: {} with employeeCode: {}", adminEmail, employeeCode);
        } else {
            log.info("ℹ️ Tamabee admin already exists: {}", adminEmail);
        }
    }
}
