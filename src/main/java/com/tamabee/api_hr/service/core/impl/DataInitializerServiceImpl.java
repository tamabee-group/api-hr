package com.tamabee.api_hr.service.core.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.tamabee.api_hr.constants.PlanConstants.FREE_PLAN_ID;
import static com.tamabee.api_hr.constants.PlanConstants.TAMABEE_FREE_TRIAL_YEARS;
import static com.tamabee.api_hr.constants.PlanConstants.TAMABEE_TENANT;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.mapper.core.WalletFactory;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.repository.wallet.WalletRepository;
import com.tamabee.api_hr.service.core.interfaces.IDataInitializerService;
import com.tamabee.api_hr.util.EmployeeCodeGenerator;
import com.tamabee.api_hr.util.ReferralCodeGenerator;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý khởi tạo dữ liệu mặc định.
 * Tách riêng để @Transactional hoạt động đúng với Spring proxy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitializerServiceImpl implements IDataInitializerService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final WalletRepository walletRepository;
    private final WalletFactory walletFactory;
    private final PasswordEncoder passwordEncoder;

    /**
     * Tạo Tamabee company và wallet trong master DB.
     * Tamabee company có id = 0 (đặc biệt).
     */
    @Override
    @Transactional
    public void createTamabeeCompanyIfNotExists() {
        CompanyEntity company = companyRepository.findByTenantDomainAndDeletedFalse(TAMABEE_TENANT)
                .orElse(null);
        
        if (company == null) {
            // Dùng native query để insert với id = 0
            companyRepository.insertTamabeeCompany(
                "Tamabee株式会社",
                "Tamabee Admin",
                "contact@tamabee.vn",
                "0311111111",
                "Tokyo",
                "technology",
                "1000001",
                "ja",
                "ja",
                TAMABEE_TENANT,
                FREE_PLAN_ID,
                CompanyStatus.ACTIVE.name()
            );
            
            // Lấy lại company vừa tạo
            company = companyRepository.findByTenantDomainAndDeletedFalse(TAMABEE_TENANT)
                    .orElseThrow(() -> new RuntimeException("Failed to create Tamabee company"));
            
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
            // Fix region cũ dùng timezone thay vì language code
            if (company.getRegion() != null && company.getRegion().contains("/")) {
                company.setRegion("ja");
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
            LocalDateTime freeTrialEndDate = LocalDateTime.now(
                    RegionUtil.getTimezone(RegionContext.getCurrentRegion()))
                    .plusYears(TAMABEE_FREE_TRIAL_YEARS);
            WalletEntity wallet = walletFactory.createForCompany(company.getId(), freeTrialEndDate);
            walletRepository.save(wallet);
            log.info("✅ Created wallet for Tamabee with free trial until: {}", freeTrialEndDate);
        }
    }

    /**
     * Tạo admin user trong tenant DB.
     * QUAN TRỌNG: TenantContext phải được set TRƯỚC khi gọi method này.
     */
    @Override
    @Transactional
    public void createDefaultAdminIfNotExists() {
        String adminEmail = "hiepdeptrai0908@gmail.com";
        LocalDate adminDateOfBirth = LocalDate.of(1997, 9, 8);

        if (!userRepository.existsByEmail(adminEmail)) {
            UserEntity admin = new UserEntity();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("hiep1234"));
            admin.setRole(UserRole.ADMIN_TAMABEE);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setLanguage("vi");
            admin.setTenantDomain(TAMABEE_TENANT);

            // Tạo mã nhân viên duy nhất
            String employeeCode = EmployeeCodeGenerator.generateForAdmin(userRepository);
            admin.setEmployeeCode(employeeCode);

            // Tạo profile với mã giới thiệu
            UserProfileEntity profile = new UserProfileEntity();
            profile.setName("Triệu Quang Hiệp");
            profile.setDateOfBirth(adminDateOfBirth);
            String referralCode;
            do {
                referralCode = ReferralCodeGenerator.generate();
            } while (userRepository.existsByProfileReferralCodeAndDeletedFalse(referralCode));
            profile.setReferralCode(referralCode);
            profile.setUser(admin);
            admin.setProfile(profile);

            admin.calculateProfileCompleteness();

            userRepository.save(admin);
            log.info("✅ Created default Tamabee admin: {} with employeeCode: {}", adminEmail, employeeCode);
        } else {
            log.info("ℹ️ Tamabee admin already exists: {}", adminEmail);
        }
    }
}
