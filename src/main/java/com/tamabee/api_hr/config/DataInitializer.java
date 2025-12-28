package com.tamabee.api_hr.config;

import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.util.EmployeeCodeGenerator;
import com.tamabee.api_hr.util.ReferralCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createDefaultAdminIfNotExists();
    }

    private void createDefaultAdminIfNotExists() {
        String adminEmail = "hiepdeptrai0908@gmail.com";
        String adminDateOfBirth = "1997-09-08"; // Format yyyy-MM-dd

        if (!userRepository.existsByEmail(adminEmail)) {
            UserEntity admin = new UserEntity();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("hiep1234"));
            admin.setRole(UserRole.ADMIN_TAMABEE);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setLocale("vi");
            admin.setLanguage("vi");
            admin.setCompanyId(0L);

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
