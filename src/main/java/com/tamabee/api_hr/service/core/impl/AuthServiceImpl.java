package com.tamabee.api_hr.service.core.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.datasource.TenantDataSourceManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.tamabee.api_hr.datasource.TenantProvisioningService;
import com.tamabee.api_hr.dto.response.company.DomainAvailabilityResponse;
import com.tamabee.api_hr.dto.response.user.UserResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.exception.UnauthorizedException;
import com.tamabee.api_hr.mapper.core.CompanyMapper;
import com.tamabee.api_hr.mapper.core.UserMapper;
import com.tamabee.api_hr.mapper.core.WalletFactory;
import com.tamabee.api_hr.dto.auth.LoginRequest;
import com.tamabee.api_hr.dto.auth.RegisterRequest;
import com.tamabee.api_hr.dto.auth.LoginResponse;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.core.EmailVerificationRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.repository.wallet.WalletRepository;
import com.tamabee.api_hr.service.admin.interfaces.ISettingService;
import com.tamabee.api_hr.service.core.interfaces.IAuthService;
import com.tamabee.api_hr.util.JwtUtil;
import com.tamabee.api_hr.util.ReferralCodeGenerator;
import com.tamabee.api_hr.util.TenantDomainValidator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final WalletRepository walletRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final WalletFactory walletFactory;
    private final JwtUtil jwtUtil;
    private final ISettingService settingService;
    private final TenantProvisioningService tenantProvisioningService;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final JdbcTemplate masterJdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public AuthServiceImpl(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            WalletRepository walletRepository,
            EmailVerificationRepository emailVerificationRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            CompanyMapper companyMapper,
            WalletFactory walletFactory,
            JwtUtil jwtUtil,
            ISettingService settingService,
            TenantProvisioningService tenantProvisioningService,
            TenantDataSourceManager tenantDataSourceManager,
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
            TransactionTemplate transactionTemplate) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.walletRepository = walletRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.companyMapper = companyMapper;
        this.walletFactory = walletFactory;
        this.jwtUtil = jwtUtil;
        this.settingService = settingService;
        this.tenantProvisioningService = tenantProvisioningService;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void register(RegisterRequest request) {
        // Validation - không cần transaction
        validateEmailNotExists(request.getEmail());
        validateEmailVerified(request.getEmail());
        validateCompanyNameNotExists(request.getCompanyName());
        validateTenantDomain(request.getTenantDomain());
        validateReferralCode(request.getReferralCode());

        // 1. Tạo company và wallet trong master DB (dùng transaction riêng)
        CompanyEntity company = createCompanyWithTransaction(request);
        LocalDateTime freeTrialEndDate = calculateFreeTrialEndDate(request.getReferralCode());
        WalletEntity wallet = createWalletWithTransaction(company.getId(), freeTrialEndDate);

        // 2. Provision tenant database - nếu thất bại sẽ rollback company và wallet
        try {
            tenantProvisioningService.provisionTenant(company.getTenantDomain());
            log.info("Successfully provisioned tenant database for company: {}", company.getId());
        } catch (Exception e) {
            log.error("Failed to provision tenant database for company: {}, rolling back...", company.getId(), e);
            // Rollback: xóa wallet và company
            deleteWalletWithTransaction(wallet.getId());
            deleteCompanyWithTransaction(company.getId());
            // Cố gắng xóa database nếu đã tạo một phần
            tenantProvisioningService.dropTenant(company.getTenantDomain());
            throw new BadRequestException(ErrorCode.TENANT_PROVISIONING_FAILED);
        }

        // 3. Tạo user admin trong tenant DB bằng JDBC trực tiếp
        String tenantDomain = request.getTenantDomain();
        try {
            Long userId = createUserWithJdbc(request, company.getId(), tenantDomain);
            
            // Cập nhật owner cho company (trong master DB)
            updateCompanyOwner(company.getId(), userId);
            
            log.info("Successfully created admin user for tenant: {}", tenantDomain);
        } catch (Exception e) {
            log.error("Failed to create admin user for tenant: {}, rolling back...", tenantDomain, e);
            // Rollback: xóa wallet, company và drop tenant database
            deleteWalletWithTransaction(wallet.getId());
            deleteCompanyWithTransaction(company.getId());
            tenantProvisioningService.dropTenant(tenantDomain);
            throw new BadRequestException(ErrorCode.USER_CREATION_FAILED);
        }
    }

    /**
     * Tạo company với transaction riêng (dùng TransactionTemplate)
     */
    private CompanyEntity createCompanyWithTransaction(RegisterRequest request) {
        return transactionTemplate.execute(status -> {
            CompanyEntity company = companyMapper.toEntity(request);
            return companyRepository.save(company);
        });
    }

    /**
     * Tạo wallet với transaction riêng (dùng TransactionTemplate)
     */
    private WalletEntity createWalletWithTransaction(Long companyId, LocalDateTime freeTrialEndDate) {
        return transactionTemplate.execute(status -> {
            WalletEntity wallet = walletFactory.createForCompany(companyId, freeTrialEndDate);
            return walletRepository.save(wallet);
        });
    }

    /**
     * Xóa company với transaction riêng (rollback)
     */
    private void deleteCompanyWithTransaction(Long companyId) {
        transactionTemplate.executeWithoutResult(status -> {
            companyRepository.deleteById(companyId);
        });
    }

    /**
     * Xóa wallet với transaction riêng (rollback)
     */
    private void deleteWalletWithTransaction(Long walletId) {
        transactionTemplate.executeWithoutResult(status -> {
            walletRepository.deleteById(walletId);
        });
    }

    /**
     * Tạo user và profile trong tenant DB bằng JDBC trực tiếp
     * Không dùng JPA vì cần switch DataSource
     */
    private Long createUserWithJdbc(RegisterRequest request, Long companyId, String tenantDomain) throws Exception {
        log.info("Creating admin user in tenant DB: {}", tenantDomain);
        
        DataSource tenantDs = tenantDataSourceManager.getDataSource(tenantDomain);
        if (tenantDs == null) {
            log.error("Tenant DataSource not found: {}", tenantDomain);
            throw new RuntimeException("Tenant DataSource not found: " + tenantDomain);
        }
        
        log.info("Got tenant DataSource for: {}", tenantDomain);

        try (Connection conn = tenantDs.getConnection()) {
            log.info("Got connection to tenant DB: {}", tenantDomain);
            conn.setAutoCommit(false);
            try {
                // Tạo employee code cho admin: yyyymmdd (ngày đăng ký)
                String employeeCode = generateAdminEmployeeCode(conn);
                log.info("Generated employee code: {}", employeeCode);
                
                // Tạo referral code
                String referralCode = ReferralCodeGenerator.generate();
                log.info("Generated referral code: {}", referralCode);
                
                // Insert user
                String userSql = """
                    INSERT INTO users (employee_code, email, password, role, status, locale, language, 
                                      tenant_domain, profile_completeness, deleted, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false, NOW(), NOW())
                    RETURNING id
                    """;
                
                Long userId;
                try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                    ps.setString(1, employeeCode);
                    ps.setString(2, request.getEmail());
                    ps.setString(3, passwordEncoder.encode(request.getPassword()));
                    ps.setString(4, UserRole.ADMIN_COMPANY.name());
                    ps.setString(5, UserStatus.ACTIVE.name());
                    ps.setString(6, request.getLocale());
                    ps.setString(7, request.getLanguage());
                    ps.setString(8, tenantDomain);
                    ps.setInt(9, 8); // profileCompleteness = 8% (1/12 fields filled - name)
                    
                    log.info("Executing INSERT user SQL for email: {}", request.getEmail());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getLong(1);
                            log.info("Inserted user with ID: {}", userId);
                        } else {
                            log.error("Failed to get user ID after INSERT");
                            throw new RuntimeException("Failed to get user ID");
                        }
                    }
                }
                
                // Insert user profile
                String profileSql = """
                    INSERT INTO user_profiles (user_id, name, referral_code, deleted, created_at, updated_at)
                    VALUES (?, ?, ?, false, NOW(), NOW())
                    """;
                
                try (PreparedStatement ps = conn.prepareStatement(profileSql)) {
                    ps.setLong(1, userId);
                    ps.setString(2, request.getOwnerName());
                    ps.setString(3, referralCode);
                    log.info("Executing INSERT user_profile SQL for userId: {}", userId);
                    ps.executeUpdate();
                    log.info("Inserted user_profile for userId: {}", userId);
                }
                
                conn.commit();
                log.info("COMMITTED: Created user {} with employee code {} in tenant {}", userId, employeeCode, tenantDomain);
                return userId;
                
            } catch (Exception e) {
                log.error("Error creating user in tenant {}, rolling back: {}", tenantDomain, e.getMessage(), e);
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error("Failed to get connection or create user in tenant {}: {}", tenantDomain, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Tạo employee code cho admin: yyyymmdd (ngày đăng ký)
     * Nếu trùng thì thử năm tiếp theo
     */
    private String generateAdminEmployeeCode(Connection conn) throws Exception {
        java.time.LocalDate today = java.time.LocalDate.now();
        int year = today.getYear();
        String monthDay = String.format("%02d%02d", today.getMonthValue(), today.getDayOfMonth());
        
        // Thử từ năm hiện tại, nếu trùng thì tăng năm
        for (int i = 0; i < 100; i++) {
            String employeeCode = String.valueOf(year + i) + monthDay;
            if (!employeeCodeExists(conn, employeeCode)) {
                return employeeCode;
            }
        }
        
        // Fallback: thêm random suffix
        return String.valueOf(year) + monthDay;
    }

    /**
     * Tạo employee code cho user thường: năm đăng ký + tháng sinh + ngày sinh
     * Nếu trùng thì năm + 1
     * @param dateOfBirth format yyyy-MM-dd hoặc dd/MM/yyyy
     */
    public static String generateUserEmployeeCode(Connection conn, String dateOfBirth) throws Exception {
        java.time.LocalDate today = java.time.LocalDate.now();
        int year = today.getYear();
        
        // Parse ngày sinh
        String month = "01";
        String day = "01";
        
        if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
            try {
                if (dateOfBirth.contains("-")) {
                    // Format yyyy-MM-dd
                    String[] parts = dateOfBirth.split("-");
                    if (parts.length >= 3) {
                        month = String.format("%02d", Integer.parseInt(parts[1]));
                        day = String.format("%02d", Integer.parseInt(parts[2]));
                    }
                } else if (dateOfBirth.contains("/")) {
                    // Format dd/MM/yyyy
                    String[] parts = dateOfBirth.split("/");
                    if (parts.length >= 2) {
                        day = String.format("%02d", Integer.parseInt(parts[0]));
                        month = String.format("%02d", Integer.parseInt(parts[1]));
                    }
                }
            } catch (NumberFormatException e) {
                // Giữ giá trị mặc định
            }
        }
        
        // Thử từ năm hiện tại, nếu trùng thì tăng năm
        for (int i = 0; i < 100; i++) {
            String employeeCode = String.valueOf(year + i) + month + day;
            if (!employeeCodeExists(conn, employeeCode)) {
                return employeeCode;
            }
        }
        
        // Fallback
        return String.valueOf(year) + month + day;
    }

    /**
     * Kiểm tra employee code đã tồn tại trong tenant DB chưa
     */
    private static boolean employeeCodeExists(Connection conn, String employeeCode) throws Exception {
        String sql = "SELECT EXISTS(SELECT 1 FROM users WHERE employee_code = ? AND deleted = false)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employeeCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    /**
     * Cập nhật owner_id cho company trong master DB bằng JDBC
     */
    private void updateCompanyOwner(Long companyId, Long ownerId) {
        String sql = "UPDATE companies SET owner_id = ? WHERE id = ?";
        masterJdbcTemplate.update(sql, ownerId, companyId);
    }

    /**
     * Tính toán ngày hết hạn free trial
     * - Không có referral code: freeTrialMonths (mặc định 2 tháng)
     * - Có referral code: freeTrialMonths + referralBonusMonths (mặc định 3 tháng)
     */
    private LocalDateTime calculateFreeTrialEndDate(String referralCode) {
        int freeTrialMonths = settingService.getFreeTrialMonths();
        int totalMonths = freeTrialMonths;

        // Nếu có mã giới thiệu hợp lệ, cộng thêm bonus months
        if (referralCode != null && !referralCode.isEmpty()) {
            totalMonths += settingService.getReferralBonusMonths();
        }

        return LocalDateTime.now().plusMonths(totalMonths);
    }

    private void validateEmailVerified(String email) {
        boolean isVerified = emailVerificationRepository.existsByEmailAndUsedTrue(email);
        if (!isVerified) {
            throw new BadRequestException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    /**
     * Validate tenant domain: format, reserved words, và existence
     */
    private void validateTenantDomain(String tenantDomain) {
        TenantDomainValidator.ValidationResult result = TenantDomainValidator.validate(tenantDomain);

        if (!result.isValid()) {
            if (result.getError() == TenantDomainValidator.ValidationError.RESERVED) {
                throw new BadRequestException(ErrorCode.TENANT_DOMAIN_RESERVED);
            }
            throw new BadRequestException(ErrorCode.INVALID_TENANT_DOMAIN);
        }

        // Check existence trong database
        if (companyRepository.existsByTenantDomainAndDeletedFalse(tenantDomain)) {
            throw new ConflictException(ErrorCode.TENANT_DOMAIN_EXISTS);
        }
    }

    /**
     * Kiểm tra mã giới thiệu có hợp lệ không
     * Nếu có nhập mã giới thiệu thì phải tồn tại trong tamabee_tamabee.user_profiles
     */
    private void validateReferralCode(String referralCode) {
        if (referralCode == null || referralCode.isEmpty()) {
            return;
        }

        // Trim và uppercase để so sánh
        String code = referralCode.trim().toUpperCase();
        log.info("Validating referral code: '{}' (original: '{}')", code, referralCode);

        // Query tamabee_tamabee database để check referral code
        try {
            DataSource tamabeeDs = tenantDataSourceManager.getDataSource("tamabee");
            log.info("Tamabee DataSource: {}, hasTenant: {}", tamabeeDs, tenantDataSourceManager.hasTenant("tamabee"));
            
            if (tamabeeDs == null) {
                log.warn("Tamabee DataSource not found, skipping referral code validation");
                return;
            }

            try (Connection conn = tamabeeDs.getConnection()) {
                log.info("Connected to tamabee database, catalog: {}", conn.getCatalog());
                
                // Query không phân biệt hoa thường
                String sql = "SELECT referral_code FROM user_profiles WHERE UPPER(referral_code) = ? AND deleted = false";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, code);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String foundCode = rs.getString(1);
                            log.info("Referral code '{}' found in database: '{}'", code, foundCode);
                            return;
                        }
                    }
                }
                
                // Debug: list all referral codes
                String debugSql = "SELECT referral_code FROM user_profiles WHERE deleted = false LIMIT 5";
                try (PreparedStatement ps = conn.prepareStatement(debugSql);
                     ResultSet rs = ps.executeQuery()) {
                    StringBuilder sb = new StringBuilder("Available referral codes: ");
                    while (rs.next()) {
                        sb.append(rs.getString(1)).append(", ");
                    }
                    log.info(sb.toString());
                }
            }

            // Mã giới thiệu không tồn tại
            log.warn("Invalid referral code: '{}' - not found in tamabee_tamabee.user_profiles", code);
            throw new BadRequestException(ErrorCode.INVALID_REFERRAL_CODE);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating referral code '{}': {}", code, e.getMessage(), e);
            // Không throw exception, cho phép đăng ký tiếp
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}, current tenant: {}",
                request.getEmail(),
                TenantContext.getCurrentTenant());

        // Tìm user bằng email hoặc mã nhân viên (trong tenant DB)
        UserEntity user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .or(() -> userRepository.findByEmployeeCodeAndDeletedFalse(request.getEmail()))
                .orElseThrow(UnauthorizedException::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw UnauthorizedException.invalidCredentials();
        }

        // Lấy tenantDomain từ user
        String tenantDomain = user.getTenantDomain();
        
        // Lấy companyId và planId từ master DB bằng JDBC
        Long companyId = getCompanyIdFromTenant(tenantDomain);
        Long planId = getPlanId(companyId);

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                companyId,
                tenantDomain,
                planId);

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(),
                user.getEmail());

        // Lấy company name và logo từ master DB
        String companyName = getCompanyName(companyId, tenantDomain);
        String companyLogo = getCompanyLogo(companyId, tenantDomain);
        UserResponse userResponse = userMapper.toResponse(user, companyName, companyLogo, tenantDomain, planId);

        return new LoginResponse(accessToken, refreshToken, userResponse);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateEmailExists(String email) {
        if (!userRepository.existsByEmailAndDeletedFalse(email)) {
            throw NotFoundException.email(email);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateEmailNotExists(String email) {
        // Chỉ check trong master DB (companies table)
        // Không check users vì đây là đăng ký công ty mới, chưa có tenant
        if (companyRepository.existsByEmail(email)) {
            throw ConflictException.emailExists(email);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCompanyNameNotExists(String companyName) {
        if (companyRepository.existsByName(companyName)) {
            throw ConflictException.companyNameExists(companyName);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String refreshToken) {
        var claims = jwtUtil.validateToken(refreshToken);
        if (claims == null) {
            throw UnauthorizedException.invalidRefreshToken();
        }

        String email = (String) claims.get("sub");
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));

        // Lấy thông tin tenant và company từ tenantDomain
        String tenantDomain = user.getTenantDomain();
        Long companyId = getCompanyIdFromTenant(tenantDomain);
        Long planId = getPlanId(companyId);

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                companyId,
                tenantDomain,
                planId);

        // Lấy tên và logo công ty
        String companyName = getCompanyName(companyId, tenantDomain);
        String companyLogo = getCompanyLogo(companyId, tenantDomain);
        UserResponse userResponse = userMapper.toResponse(user, companyName, companyLogo, tenantDomain, planId);

        return new LoginResponse(newAccessToken, refreshToken, userResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw UnauthorizedException.notAuthenticated();
        }

        String email = authentication.getName();
        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> NotFoundException.user(email));

        // Lấy thông tin tenant và company từ tenantDomain
        String tenantDomain = user.getTenantDomain();
        Long companyId = getCompanyIdFromTenant(tenantDomain);
        Long planId = getPlanId(companyId);

        // Lấy tên và logo công ty
        String companyName = getCompanyName(companyId, tenantDomain);
        String companyLogo = getCompanyLogo(companyId, tenantDomain);
        return userMapper.toResponse(user, companyName, companyLogo, tenantDomain, planId);
    }

    /**
     * Lấy tên công ty từ companyId hoặc tenantDomain (query master DB)
     */
    private String getCompanyName(Long companyId, String tenantDomain) {
        String sql;
        Object param;
        
        if (companyId != null && companyId > 0) {
            sql = "SELECT name FROM companies WHERE id = ? AND deleted = false";
            param = companyId;
        } else if (tenantDomain != null) {
            sql = "SELECT name FROM companies WHERE tenant_domain = ? AND deleted = false";
            param = tenantDomain;
        } else {
            return null;
        }
        
        try {
            return masterJdbcTemplate.queryForObject(sql, String.class, param);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lấy logo công ty từ companyId hoặc tenantDomain (query master DB)
     */
    private String getCompanyLogo(Long companyId, String tenantDomain) {
        String sql;
        Object param;
        
        if (companyId != null && companyId > 0) {
            sql = "SELECT logo FROM companies WHERE id = ? AND deleted = false";
            param = companyId;
        } else if (tenantDomain != null) {
            sql = "SELECT logo FROM companies WHERE tenant_domain = ? AND deleted = false";
            param = tenantDomain;
        } else {
            return null;
        }
        
        try {
            return masterJdbcTemplate.queryForObject(sql, String.class, param);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lấy companyId từ tenantDomain (query master DB).
     * Tamabee tenant: companyId = 0
     * Company tenant: companyId từ company table
     */
    private Long getCompanyIdFromTenant(String tenantDomain) {
        if (tenantDomain == null || "tamabee".equals(tenantDomain)) {
            return 0L;
        }
        String sql = "SELECT id FROM companies WHERE tenant_domain = ? AND deleted = false";
        try {
            return masterJdbcTemplate.queryForObject(sql, Long.class, tenantDomain);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Lấy planId cho company từ master database.
     */
    private Long getPlanId(Long companyId) {
        if (companyId == null) {
            return null;
        }
        String sql = "SELECT plan_id FROM companies WHERE id = ? AND deleted = false";
        try {
            return masterJdbcTemplate.queryForObject(sql, Long.class, companyId);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DomainAvailabilityResponse checkDomainAvailability(String domain) {
        // Validate format và reserved words
        TenantDomainValidator.ValidationResult validationResult = TenantDomainValidator.validate(domain);

        if (!validationResult.isValid()) {
            String reason = validationResult.getError() == TenantDomainValidator.ValidationError.RESERVED
                    ? "RESERVED"
                    : "INVALID_FORMAT";
            return DomainAvailabilityResponse.builder()
                    .domain(domain)
                    .available(false)
                    .reason(reason)
                    .build();
        }

        // Check existence trong database
        boolean exists = companyRepository.existsByTenantDomainAndDeletedFalse(domain);
        if (exists) {
            return DomainAvailabilityResponse.builder()
                    .domain(domain)
                    .available(false)
                    .reason("ALREADY_EXISTS")
                    .build();
        }

        return DomainAvailabilityResponse.builder()
                .domain(domain)
                .available(true)
                .reason(null)
                .build();
    }
}
