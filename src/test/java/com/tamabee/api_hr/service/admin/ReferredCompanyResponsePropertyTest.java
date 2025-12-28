package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.response.ReferredCompanyResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.EmployeeCommissionEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.repository.*;
import com.tamabee.api_hr.service.admin.impl.EmployeeReferralServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Referred Company Response
 * Property 8: Referred company response chứa đầy đủ service usage info
 */
@Tag("Feature: tamabee-role-redesign")
public class ReferredCompanyResponsePropertyTest {

        @Mock
        private CompanyRepository companyRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private WalletRepository walletRepository;

        @Mock
        private WalletTransactionRepository walletTransactionRepository;

        @Mock
        private EmployeeCommissionRepository commissionRepository;

        @Mock
        private PlanRepository planRepository;

        private EmployeeReferralServiceImpl employeeReferralService;

        @BeforeTry
        void setUp() {
                MockitoAnnotations.openMocks(this);
                employeeReferralService = new EmployeeReferralServiceImpl(
                                companyRepository,
                                userRepository,
                                walletRepository,
                                walletTransactionRepository,
                                commissionRepository,
                                planRepository);
        }

        /**
         * Property 8: Referred company response chứa đầy đủ service usage info
         * For any referred company trong response, response SHALL chứa:
         * currentBalance, totalDeposits, totalBilling, commissionStatus.
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 8: Referred company response chứa đầy đủ service usage info")
        void referredCompanyResponseShouldContainAllServiceUsageInfo(
                        @ForAll("validEmployeeCodes") String employeeCode,
                        @ForAll("validEmployeeIds") Long employeeId,
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("validBalances") BigDecimal currentBalance,
                        @ForAll("validBalances") BigDecimal totalDeposits,
                        @ForAll("validBalances") BigDecimal totalBilling) {

                // Chuẩn bị employee
                UserEntity employee = createEmployee(employeeId, employeeCode);

                // Chuẩn bị company
                CompanyEntity company = createCompany(companyId, employee);

                // Chuẩn bị wallet với service usage info
                WalletEntity wallet = createWallet(companyId, currentBalance, totalBilling);

                // Mock repositories
                when(userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode))
                                .thenReturn(Optional.of(employee));

                Pageable pageable = PageRequest.of(0, 20);
                Page<CompanyEntity> companyPage = new PageImpl<>(Collections.singletonList(company), pageable, 1);

                when(companyRepository.findByReferredByEmployeeId(eq(employeeId), any(Pageable.class)))
                                .thenReturn(companyPage);

                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));

                when(walletTransactionRepository.sumDepositsByCompanyId(companyId))
                                .thenReturn(totalDeposits);

                when(commissionRepository.findByCompanyIdAndDeletedFalse(companyId))
                                .thenReturn(Optional.empty());

                // Thực thi
                Page<ReferredCompanyResponse> result = employeeReferralService.getReferredCompanies(employeeCode,
                                pageable);

                // Kiểm tra: response phải chứa đầy đủ service usage info
                assertFalse(result.getContent().isEmpty(), "Response không được rỗng");

                ReferredCompanyResponse response = result.getContent().get(0);

                // Kiểm tra currentBalance
                assertNotNull(response.getCurrentBalance(), "currentBalance không được null");
                assertEquals(currentBalance, response.getCurrentBalance(),
                                "currentBalance phải khớp với wallet balance");

                // Kiểm tra totalDeposits
                assertNotNull(response.getTotalDeposits(), "totalDeposits không được null");
                assertEquals(totalDeposits, response.getTotalDeposits(),
                                "totalDeposits phải khớp với sum deposits");

                // Kiểm tra totalBilling
                assertNotNull(response.getTotalBilling(), "totalBilling không được null");
                assertEquals(totalBilling, response.getTotalBilling(),
                                "totalBilling phải khớp với wallet totalBilling");
        }

        /**
         * Property 8: Response phải chứa commission status khi có commission
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 8: Referred company response chứa đầy đủ service usage info")
        void referredCompanyResponseShouldContainCommissionStatus(
                        @ForAll("validEmployeeCodes") String employeeCode,
                        @ForAll("validEmployeeIds") Long employeeId,
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("validBalances") BigDecimal commissionAmount,
                        @ForAll("commissionStatuses") CommissionStatus commissionStatus) {

                // Chuẩn bị employee
                UserEntity employee = createEmployee(employeeId, employeeCode);

                // Chuẩn bị company
                CompanyEntity company = createCompany(companyId, employee);

                // Chuẩn bị wallet
                WalletEntity wallet = createWallet(companyId, BigDecimal.valueOf(100000), BigDecimal.valueOf(50000));

                // Chuẩn bị commission
                EmployeeCommissionEntity commission = createCommission(companyId, employeeCode, commissionAmount,
                                commissionStatus);

                // Mock repositories
                when(userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode))
                                .thenReturn(Optional.of(employee));

                Pageable pageable = PageRequest.of(0, 20);
                Page<CompanyEntity> companyPage = new PageImpl<>(Collections.singletonList(company), pageable, 1);

                when(companyRepository.findByReferredByEmployeeId(eq(employeeId), any(Pageable.class)))
                                .thenReturn(companyPage);

                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));

                when(walletTransactionRepository.sumDepositsByCompanyId(companyId))
                                .thenReturn(BigDecimal.valueOf(80000));

                when(commissionRepository.findByCompanyIdAndDeletedFalse(companyId))
                                .thenReturn(Optional.of(commission));

                // Thực thi
                Page<ReferredCompanyResponse> result = employeeReferralService.getReferredCompanies(employeeCode,
                                pageable);

                // Kiểm tra: response phải chứa commission info
                assertFalse(result.getContent().isEmpty(), "Response không được rỗng");

                ReferredCompanyResponse response = result.getContent().get(0);

                // Kiểm tra commissionAmount
                assertNotNull(response.getCommissionAmount(), "commissionAmount không được null khi có commission");
                assertEquals(commissionAmount, response.getCommissionAmount(),
                                "commissionAmount phải khớp với commission amount");

                // Kiểm tra commissionStatus
                assertNotNull(response.getCommissionStatus(), "commissionStatus không được null khi có commission");
                assertEquals(commissionStatus, response.getCommissionStatus(),
                                "commissionStatus phải khớp với commission status");
        }

        /**
         * Property 8: Response phải chứa company info
         */
        @Property(tries = 100)
        @Tag("Feature: tamabee-role-redesign, Property 8: Referred company response chứa đầy đủ service usage info")
        void referredCompanyResponseShouldContainCompanyInfo(
                        @ForAll("validEmployeeCodes") String employeeCode,
                        @ForAll("validEmployeeIds") Long employeeId,
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("companyNames") String companyName,
                        @ForAll("ownerNames") String ownerName) {

                // Chuẩn bị employee
                UserEntity employee = createEmployee(employeeId, employeeCode);

                // Chuẩn bị company với tên cụ thể
                CompanyEntity company = createCompany(companyId, employee);
                company.setName(companyName);
                company.setOwnerName(ownerName);

                // Chuẩn bị wallet
                WalletEntity wallet = createWallet(companyId, BigDecimal.valueOf(100000), BigDecimal.valueOf(50000));

                // Mock repositories
                when(userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode))
                                .thenReturn(Optional.of(employee));

                Pageable pageable = PageRequest.of(0, 20);
                Page<CompanyEntity> companyPage = new PageImpl<>(Collections.singletonList(company), pageable, 1);

                when(companyRepository.findByReferredByEmployeeId(eq(employeeId), any(Pageable.class)))
                                .thenReturn(companyPage);

                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));

                when(walletTransactionRepository.sumDepositsByCompanyId(companyId))
                                .thenReturn(BigDecimal.valueOf(80000));

                when(commissionRepository.findByCompanyIdAndDeletedFalse(companyId))
                                .thenReturn(Optional.empty());

                // Thực thi
                Page<ReferredCompanyResponse> result = employeeReferralService.getReferredCompanies(employeeCode,
                                pageable);

                // Kiểm tra: response phải chứa company info
                assertFalse(result.getContent().isEmpty(), "Response không được rỗng");

                ReferredCompanyResponse response = result.getContent().get(0);

                // Kiểm tra companyId
                assertNotNull(response.getCompanyId(), "companyId không được null");
                assertEquals(companyId, response.getCompanyId(), "companyId phải khớp");

                // Kiểm tra companyName
                assertNotNull(response.getCompanyName(), "companyName không được null");
                assertEquals(companyName, response.getCompanyName(), "companyName phải khớp");

                // Kiểm tra ownerName
                assertNotNull(response.getOwnerName(), "ownerName không được null");
                assertEquals(ownerName, response.getOwnerName(), "ownerName phải khớp");

                // Kiểm tra status
                assertNotNull(response.getStatus(), "status không được null");
        }

        // === Generators ===

        @Provide
        Arbitrary<String> validEmployeeCodes() {
                return Arbitraries.strings()
                                .withCharRange('A', 'Z')
                                .ofLength(6)
                                .map(s -> "EMP" + s.substring(0, 3));
        }

        @Provide
        Arbitrary<Long> validEmployeeIds() {
                return Arbitraries.longs().between(1L, 10000L);
        }

        @Provide
        Arbitrary<Long> validCompanyIds() {
                return Arbitraries.longs().between(1L, 10000L);
        }

        @Provide
        Arbitrary<BigDecimal> validBalances() {
                return Arbitraries.bigDecimals()
                                .between(BigDecimal.ZERO, BigDecimal.valueOf(1000000))
                                .ofScale(0);
        }

        @Provide
        Arbitrary<CommissionStatus> commissionStatuses() {
                return Arbitraries.of(CommissionStatus.PENDING, CommissionStatus.ELIGIBLE, CommissionStatus.PAID);
        }

        @Provide
        Arbitrary<String> companyNames() {
                return Arbitraries.strings()
                                .withCharRange('A', 'Z')
                                .ofMinLength(3)
                                .ofMaxLength(20)
                                .map(s -> "Company " + s);
        }

        @Provide
        Arbitrary<String> ownerNames() {
                return Arbitraries.strings()
                                .withCharRange('A', 'Z')
                                .ofMinLength(3)
                                .ofMaxLength(15)
                                .map(s -> "Owner " + s);
        }

        // === Helper methods ===

        private UserEntity createEmployee(Long id, String employeeCode) {
                UserEntity employee = new UserEntity();
                employee.setId(id);
                employee.setEmployeeCode(employeeCode);
                employee.setEmail(employeeCode.toLowerCase() + "@tamabee.com");
                employee.setRole(UserRole.EMPLOYEE_TAMABEE);
                employee.setCompanyId(0L);
                return employee;
        }

        private CompanyEntity createCompany(Long id, UserEntity referrer) {
                CompanyEntity company = new CompanyEntity();
                company.setId(id);
                company.setName("Test Company " + id);
                company.setOwnerName("Test Owner " + id);
                company.setEmail("company" + id + "@test.com");
                company.setPhone("0123456789");
                company.setAddress("Test Address");
                company.setIndustry("IT");
                company.setLocale("vi");
                company.setLanguage("vi");
                company.setStatus(CompanyStatus.ACTIVE);
                company.setReferredByEmployee(referrer);
                company.setCreatedAt(LocalDateTime.now());
                return company;
        }

        private WalletEntity createWallet(Long companyId, BigDecimal balance, BigDecimal totalBilling) {
                WalletEntity wallet = new WalletEntity();
                wallet.setId(companyId);
                wallet.setCompanyId(companyId);
                wallet.setBalance(balance);
                wallet.setTotalBilling(totalBilling);
                wallet.setLastBillingDate(LocalDateTime.now().minusDays(30));
                wallet.setNextBillingDate(LocalDateTime.now().plusDays(1));
                return wallet;
        }

        private EmployeeCommissionEntity createCommission(Long companyId, String employeeCode,
                        BigDecimal amount, CommissionStatus status) {
                EmployeeCommissionEntity commission = new EmployeeCommissionEntity();
                commission.setId(companyId);
                commission.setCompanyId(companyId);
                commission.setEmployeeCode(employeeCode);
                commission.setAmount(amount);
                commission.setStatus(status);
                commission.setCompanyBillingAtCreation(BigDecimal.ZERO);
                return commission;
        }
}
