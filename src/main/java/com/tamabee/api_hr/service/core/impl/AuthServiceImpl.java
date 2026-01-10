package com.tamabee.api_hr.service.core.impl;

import java.time.LocalDateTime;

import com.tamabee.api_hr.datasource.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.TenantProvisioningService;
import com.tamabee.api_hr.dto.response.company.DomainAvailabilityResponse;
import com.tamabee.api_hr.dto.response.user.UserResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.CompanyStatus;
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
import com.tamabee.api_hr.util.EmployeeCodeGenerator;
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
    private final JdbcTemplate masterJdbcTemplate;

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
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate) {
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
        this.masterJdbcTemplate = masterJdbcTemplate;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        validateEmailNotExists(request.getEmail());
        validateEmailVerified(request.getEmail());
        validateCompanyNameNotExists(request.getCompanyName());
        validateTenantDomain(request.getTenantDomain());
        validateReferralCode(request.getReferralCode());

        CompanyEntity company = createCompany(request);

        // Tính toán freeTrialEndDate dựa trên referral code
        LocalDateTime freeTrialEndDate = calculateFreeTrialEndDate(request.getReferralCode());
        createWallet(company.getId(), freeTrialEndDate);

        UserEntity user = createUser(request, company.getId());

        // Cập nhật owner cho company
        company.setOwner(user);
        companyRepository.save(company);

        // Provision tenant database sau khi tạo company thành công
        provisionTenantDatabase(company);
    }

    /**
     * Provision tenant database cho company mới.
     * Nếu thất bại, set company status = FAILED và log error.
     */
    private void provisionTenantDatabase(CompanyEntity company) {
        try {
            tenantProvisioningService.provisionTenant(company.getTenantDomain());
            log.info("Successfully provisioned tenant database for company: {}", company.getId());
        } catch (Exception e) {
            log.error("Failed to provision tenant database for company: {}", company.getId(), e);
            company.setStatus(CompanyStatus.FAILED);
            companyRepository.save(company);
            throw new BadRequestException(ErrorCode.TENANT_PROVISIONING_FAILED);
        }
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
     * Nếu có nhập mã giới thiệu thì phải tồn tại trong hệ thống
     */
    private void validateReferralCode(String referralCode) {
        if (referralCode != null && !referralCode.isEmpty()) {
            boolean exists = userRepository.existsByProfileReferralCodeAndDeletedFalse(referralCode);
            if (!exists) {
                throw new BadRequestException(ErrorCode.INVALID_REFERRAL_CODE);
            }
        }
    }

    private CompanyEntity createCompany(RegisterRequest request) {
        CompanyEntity company = companyMapper.toEntity(request);
        return companyRepository.save(company);
    }

    private void createWallet(Long companyId, LocalDateTime freeTrialEndDate) {
        WalletEntity wallet = walletFactory.createForCompany(companyId, freeTrialEndDate);
        walletRepository.save(wallet);
    }

    private UserEntity createUser(RegisterRequest request, Long companyId) {
        UserEntity user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.ADMIN_COMPANY);
        user.setStatus(UserStatus.ACTIVE);
        user.setTenantDomain(request.getTenantDomain());

        // Tạo mã nhân viên duy nhất từ companyId và ngày sinh
        String employeeCode = EmployeeCodeGenerator.generateUnique(companyId, null, userRepository);
        user.setEmployeeCode(employeeCode);

        // Tạo profile với mã giới thiệu
        UserProfileEntity profile = new UserProfileEntity();
        String referralCode;
        do {
            referralCode = ReferralCodeGenerator.generate();
        } while (userRepository.existsByProfileReferralCodeAndDeletedFalse(referralCode));
        profile.setReferralCode(referralCode);
        profile.setUser(user);
        user.setProfile(profile);

        user.calculateProfileCompleteness();
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}, current tenant: {}",
                request.getEmail(),
                TenantContext.getCurrentTenant());

        // Tìm user bằng email hoặc mã nhân viên
        UserEntity user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
                .or(() -> userRepository.findByEmployeeCodeAndDeletedFalse(request.getEmail()))
                .orElseThrow(UnauthorizedException::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw UnauthorizedException.invalidCredentials();
        }

        // Lấy thông tin tenant và company từ tenantDomain
        String tenantDomain = user.getTenantDomain();
        Long companyId = getCompanyIdFromTenant(tenantDomain);
        Long planId = getPlanId(companyId);

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

        // Lấy tên và logo công ty
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
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            throw ConflictException.emailExists(email);
        }
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
     * Lấy companyId từ tenantDomain.
     * Tamabee tenant: companyId = 0
     * Company tenant: companyId từ company table
     */
    private Long getCompanyIdFromTenant(String tenantDomain) {
        if (tenantDomain == null || "tamabee".equals(tenantDomain)) {
            return 0L;
        }
        return companyRepository.findByTenantDomainAndDeletedFalse(tenantDomain)
                .map(CompanyEntity::getId)
                .orElse(0L);
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
