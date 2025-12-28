package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.response.DepositRequestResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.entity.wallet.DepositRequestEntity;
import com.tamabee.api_hr.enums.DepositStatus;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.mapper.admin.DepositRequestMapper;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.DepositRequestRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.admin.impl.DepositRequestServiceImpl;
import com.tamabee.api_hr.service.core.IEmailService;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Deposit Request Requester Info
 * Feature: tamabee-role-redesign, Property 6: Deposit request response chứa
 * requester info
 */
public class DepositRequestRequesterInfoPropertyTest {

    @Mock
    private DepositRequestRepository depositRequestRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IWalletService walletService;

    @Mock
    private IEmailService emailService;

    private DepositRequestMapper depositRequestMapper;
    private DepositRequestServiceImpl depositRequestService;

    @BeforeTry
    void setUp() {
        MockitoAnnotations.openMocks(this);
        depositRequestMapper = new DepositRequestMapper();
        depositRequestService = new DepositRequestServiceImpl(
                depositRequestRepository,
                companyRepository,
                userRepository,
                depositRequestMapper,
                walletService,
                emailService);
    }

    /**
     * Property 6: Deposit request response chứa requester info
     * Với bất kỳ deposit request nào, response PHẢI chứa requesterName.
     * Nếu user profile có name, requesterName = profile.name
     * Nếu user profile không có name, requesterName = employee code (fallback)
     */
    @Property(tries = 100)
    void depositRequestResponseShouldContainRequesterNameFromProfile(
            @ForAll("validDepositIds") Long depositId,
            @ForAll("validEmployeeCodes") String employeeCode,
            @ForAll("validProfileNames") String profileName,
            @ForAll("validEmails") String email) {

        // Setup deposit request entity
        DepositRequestEntity entity = createDepositRequestEntity(depositId, 100L, employeeCode);
        when(depositRequestRepository.findByIdAndDeletedFalse(depositId))
                .thenReturn(Optional.of(entity));

        // Setup company
        CompanyEntity company = new CompanyEntity();
        company.setId(100L);
        company.setName("Test Company");
        when(companyRepository.findById(100L)).thenReturn(Optional.of(company));

        // Setup user với profile có name
        UserEntity user = createUserWithProfile(employeeCode, email, profileName);
        when(userRepository.findWithProfileByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(Optional.of(user));

        // Setup security context
        setupSecurityContext("admin@tamabee.com");
        setupCurrentUser(1L, "ADM001", 0L);

        // Thực thi
        DepositRequestResponse response = depositRequestService.getById(depositId);

        // Kiểm tra: requesterName phải là profile name
        assertNotNull(response, "Response không được null");
        assertNotNull(response.getRequesterName(), "RequesterName không được null");
        assertEquals(profileName, response.getRequesterName(),
                "RequesterName phải là profile name khi profile có name");
        assertEquals(email, response.getRequesterEmail(),
                "RequesterEmail phải là email của user");
    }

    /**
     * Property 6: Fallback về employee code khi profile không có name
     * Với bất kỳ deposit request nào mà user profile không có name,
     * requesterName PHẢI fallback về employee code
     */
    @Property(tries = 100)
    void depositRequestResponseShouldFallbackToEmployeeCodeWhenNoProfileName(
            @ForAll("validDepositIds") Long depositId,
            @ForAll("validEmployeeCodes") String employeeCode,
            @ForAll("emptyOrBlankNames") String emptyName,
            @ForAll("validEmails") String email) {

        // Setup deposit request entity
        DepositRequestEntity entity = createDepositRequestEntity(depositId, 100L, employeeCode);
        when(depositRequestRepository.findByIdAndDeletedFalse(depositId))
                .thenReturn(Optional.of(entity));

        // Setup company
        CompanyEntity company = new CompanyEntity();
        company.setId(100L);
        company.setName("Test Company");
        when(companyRepository.findById(100L)).thenReturn(Optional.of(company));

        // Setup user với profile không có name (null hoặc empty)
        UserEntity user = createUserWithProfile(employeeCode, email, emptyName);
        when(userRepository.findWithProfileByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(Optional.of(user));

        // Setup security context
        setupSecurityContext("admin@tamabee.com");
        setupCurrentUser(1L, "ADM001", 0L);

        // Thực thi
        DepositRequestResponse response = depositRequestService.getById(depositId);

        // Kiểm tra: requesterName phải fallback về employee code
        assertNotNull(response, "Response không được null");
        assertNotNull(response.getRequesterName(), "RequesterName không được null");
        assertEquals(employeeCode, response.getRequesterName(),
                "RequesterName phải fallback về employee code khi profile không có name");
        assertEquals(email, response.getRequesterEmail(),
                "RequesterEmail phải là email của user");
    }

    /**
     * Property 6: Fallback về employee code khi user không có profile
     * Với bất kỳ deposit request nào mà user không có profile,
     * requesterName PHẢI fallback về employee code
     */
    @Property(tries = 100)
    void depositRequestResponseShouldFallbackToEmployeeCodeWhenNoProfile(
            @ForAll("validDepositIds") Long depositId,
            @ForAll("validEmployeeCodes") String employeeCode,
            @ForAll("validEmails") String email) {

        // Setup deposit request entity
        DepositRequestEntity entity = createDepositRequestEntity(depositId, 100L, employeeCode);
        when(depositRequestRepository.findByIdAndDeletedFalse(depositId))
                .thenReturn(Optional.of(entity));

        // Setup company
        CompanyEntity company = new CompanyEntity();
        company.setId(100L);
        company.setName("Test Company");
        when(companyRepository.findById(100L)).thenReturn(Optional.of(company));

        // Setup user KHÔNG có profile
        UserEntity user = createUserWithoutProfile(employeeCode, email);
        when(userRepository.findWithProfileByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(Optional.of(user));

        // Setup security context
        setupSecurityContext("admin@tamabee.com");
        setupCurrentUser(1L, "ADM001", 0L);

        // Thực thi
        DepositRequestResponse response = depositRequestService.getById(depositId);

        // Kiểm tra: requesterName phải fallback về employee code
        assertNotNull(response, "Response không được null");
        assertNotNull(response.getRequesterName(), "RequesterName không được null");
        assertEquals(employeeCode, response.getRequesterName(),
                "RequesterName phải fallback về employee code khi user không có profile");
        assertEquals(email, response.getRequesterEmail(),
                "RequesterEmail phải là email của user");
    }

    /**
     * Property 6: Fallback về employee code khi user không tồn tại
     * Với bất kỳ deposit request nào mà user không tồn tại trong database,
     * requesterName PHẢI fallback về employee code
     */
    @Property(tries = 100)
    void depositRequestResponseShouldFallbackToEmployeeCodeWhenUserNotFound(
            @ForAll("validDepositIds") Long depositId,
            @ForAll("validEmployeeCodes") String employeeCode) {

        // Setup deposit request entity
        DepositRequestEntity entity = createDepositRequestEntity(depositId, 100L, employeeCode);
        when(depositRequestRepository.findByIdAndDeletedFalse(depositId))
                .thenReturn(Optional.of(entity));

        // Setup company
        CompanyEntity company = new CompanyEntity();
        company.setId(100L);
        company.setName("Test Company");
        when(companyRepository.findById(100L)).thenReturn(Optional.of(company));

        // User KHÔNG tồn tại
        when(userRepository.findWithProfileByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(Optional.empty());

        // Setup security context
        setupSecurityContext("admin@tamabee.com");
        setupCurrentUser(1L, "ADM001", 0L);

        // Thực thi
        DepositRequestResponse response = depositRequestService.getById(depositId);

        // Kiểm tra: requesterName phải fallback về employee code
        assertNotNull(response, "Response không được null");
        assertNotNull(response.getRequesterName(), "RequesterName không được null");
        assertEquals(employeeCode, response.getRequesterName(),
                "RequesterName phải fallback về employee code khi user không tồn tại");
        assertNull(response.getRequesterEmail(),
                "RequesterEmail phải là null khi user không tồn tại");
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<Long> validDepositIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<String> validEmployeeCodes() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofLength(3)
                .flatMap(prefix -> Arbitraries.integers().between(100, 999)
                        .map(num -> prefix + num));
    }

    @Provide
    Arbitrary<String> validProfileNames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(2)
                .ofMaxLength(50)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1));
    }

    @Provide
    Arbitrary<String> validEmails() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(s -> s + "@example.com");
    }

    @Provide
    Arbitrary<String> emptyOrBlankNames() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("\t"),
                Arbitraries.just("\n"));
    }

    // ==================== Helper Methods ====================

    private void setupSecurityContext(String email) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_TAMABEE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private UserEntity setupCurrentUser(Long userId, String employeeCode, Long companyId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        user.setEmployeeCode(employeeCode);
        user.setCompanyId(companyId);
        user.setRole(UserRole.ADMIN_TAMABEE);

        when(userRepository.findByEmailAndDeletedFalse(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.findWithProfileByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(Optional.of(user));

        return user;
    }

    private DepositRequestEntity createDepositRequestEntity(Long id, Long companyId, String requestedBy) {
        DepositRequestEntity entity = new DepositRequestEntity();
        entity.setId(id);
        entity.setCompanyId(companyId);
        entity.setAmount(BigDecimal.valueOf(100000));
        entity.setTransferProofUrl("https://example.com/proof.jpg");
        entity.setStatus(DepositStatus.PENDING);
        entity.setRequestedBy(requestedBy);
        entity.setDeleted(false);
        return entity;
    }

    private UserEntity createUserWithProfile(String employeeCode, String email, String profileName) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmployeeCode(employeeCode);
        user.setEmail(email);
        user.setRole(UserRole.ADMIN_COMPANY);

        UserProfileEntity profile = new UserProfileEntity();
        profile.setName(profileName);
        profile.setUser(user);
        user.setProfile(profile);

        return user;
    }

    private UserEntity createUserWithoutProfile(String employeeCode, String email) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmployeeCode(employeeCode);
        user.setEmail(email);
        user.setRole(UserRole.ADMIN_COMPANY);
        user.setProfile(null);

        return user;
    }
}
