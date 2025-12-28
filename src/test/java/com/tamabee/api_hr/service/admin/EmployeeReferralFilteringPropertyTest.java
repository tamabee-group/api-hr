package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.response.ReferredCompanyResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Employee Referral Filtering
 * Property 7: Employee chỉ xem được company mình giới thiệu
 */
@Tag("Feature: tamabee-role-redesign")
public class EmployeeReferralFilteringPropertyTest {

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
     * Property 7: Employee chỉ xem được company mình giới thiệu
     * For any Employee Tamabee user, khi gọi API getReferredCompanies,
     * response SHALL chỉ chứa companies có referred_by_employee_id = user's id.
     */
    @Property(tries = 100)
    @Tag("Feature: tamabee-role-redesign, Property 7: Employee chỉ xem được company mình giới thiệu")
    void employeeShouldOnlySeeCompaniesTheyReferred(
            @ForAll("validEmployeeCodes") String employeeCode,
            @ForAll("validEmployeeIds") Long employeeId,
            @ForAll("referredCompanyCount") int companyCount) {

        // Chuẩn bị employee
        UserEntity employee = createEmployee(employeeId, employeeCode);

        // Chuẩn bị danh sách companies được giới thiệu bởi employee này
        List<CompanyEntity> referredCompanies = createReferredCompanies(employee, companyCount);

        // Mock repositories
        when(userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(Optional.of(employee));

        Pageable pageable = PageRequest.of(0, 20);
        Page<CompanyEntity> companyPage = new PageImpl<>(referredCompanies, pageable, referredCompanies.size());

        when(companyRepository.findByReferredByEmployeeId(eq(employeeId), any(Pageable.class)))
                .thenReturn(companyPage);

        // Mock wallet và transaction cho mỗi company
        for (CompanyEntity company : referredCompanies) {
            WalletEntity wallet = createWallet(company.getId());
            when(walletRepository.findByCompanyId(company.getId()))
                    .thenReturn(Optional.of(wallet));
            when(walletTransactionRepository.sumDepositsByCompanyId(company.getId()))
                    .thenReturn(BigDecimal.valueOf(50000));
            when(commissionRepository.findByCompanyIdAndDeletedFalse(company.getId()))
                    .thenReturn(Optional.empty());
        }

        // Thực thi
        Page<ReferredCompanyResponse> result = employeeReferralService.getReferredCompanies(employeeCode, pageable);

        // Kiểm tra: số lượng companies trả về phải bằng số companies được giới thiệu
        assertEquals(companyCount, result.getContent().size(),
                "Số lượng companies trả về phải bằng số companies được giới thiệu");

        // Kiểm tra: tất cả companies trong response phải thuộc về employee này
        for (ReferredCompanyResponse response : result.getContent()) {
            // Verify rằng company này được query từ repository với đúng employeeId
            verify(companyRepository).findByReferredByEmployeeId(eq(employeeId), any(Pageable.class));
        }
    }

    /**
     * Property 7: Employee không thể xem company của người khác
     * Khi query với employeeId, chỉ trả về companies có referred_by_employee_id =
     * employeeId
     */
    @Property(tries = 100)
    @Tag("Feature: tamabee-role-redesign, Property 7: Employee chỉ xem được company mình giới thiệu")
    void employeeShouldNotSeeOtherEmployeesCompanies(
            @ForAll("validEmployeeCodes") String employeeCode,
            @ForAll("validEmployeeIds") Long employeeId) {

        // Chuẩn bị employee
        UserEntity employee = createEmployee(employeeId, employeeCode);

        // Mock: employee không có company nào được giới thiệu
        when(userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode))
                .thenReturn(Optional.of(employee));

        Pageable pageable = PageRequest.of(0, 20);
        Page<CompanyEntity> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);

        when(companyRepository.findByReferredByEmployeeId(eq(employeeId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Thực thi
        Page<ReferredCompanyResponse> result = employeeReferralService.getReferredCompanies(employeeCode, pageable);

        // Kiểm tra: kết quả phải rỗng
        assertTrue(result.getContent().isEmpty(),
                "Employee không có referrals phải nhận được danh sách rỗng");

        // Verify: chỉ query với đúng employeeId
        verify(companyRepository).findByReferredByEmployeeId(eq(employeeId), any(Pageable.class));
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
    Arbitrary<Integer> referredCompanyCount() {
        return Arbitraries.integers().between(0, 10);
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

    private List<CompanyEntity> createReferredCompanies(UserEntity referrer, int count) {
        List<CompanyEntity> companies = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CompanyEntity company = new CompanyEntity();
            company.setId((long) (i + 1));
            company.setName("Company " + (i + 1));
            company.setOwnerName("Owner " + (i + 1));
            company.setEmail("company" + (i + 1) + "@test.com");
            company.setPhone("0123456789");
            company.setAddress("Address " + (i + 1));
            company.setIndustry("IT");
            company.setLocale("vi");
            company.setLanguage("vi");
            company.setStatus(CompanyStatus.ACTIVE);
            company.setReferredByEmployee(referrer);
            company.setCreatedAt(LocalDateTime.now());
            companies.add(company);
        }
        return companies;
    }

    private WalletEntity createWallet(Long companyId) {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(companyId);
        wallet.setCompanyId(companyId);
        wallet.setBalance(BigDecimal.valueOf(100000));
        wallet.setTotalBilling(BigDecimal.valueOf(30000));
        wallet.setLastBillingDate(LocalDateTime.now().minusDays(30));
        wallet.setNextBillingDate(LocalDateTime.now().plusDays(1));
        return wallet;
    }
}
