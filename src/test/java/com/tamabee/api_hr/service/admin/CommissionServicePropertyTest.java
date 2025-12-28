package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.EmployeeCommissionEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.mapper.admin.EmployeeCommissionMapper;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.EmployeeCommissionRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WalletRepository;
import com.tamabee.api_hr.service.admin.impl.CommissionServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho CommissionService
 * Feature: wallet-management, Property 6: Commission First Payment Only
 */
public class CommissionServicePropertyTest {

    @Mock
    private EmployeeCommissionRepository commissionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private ISettingService settingService;

    private EmployeeCommissionMapper commissionMapper;
    private CommissionServiceImpl commissionService;

    @BeforeTry
    void setUp() {
        MockitoAnnotations.openMocks(this);
        commissionMapper = new EmployeeCommissionMapper();
        commissionService = new CommissionServiceImpl(
                commissionRepository,
                companyRepository,
                userRepository,
                walletRepository,
                settingService,
                commissionMapper);
    }

    /**
     * Property 6: Commission First Payment Only - Lần thanh toán đầu tiên
     * Với bất kỳ company nào được giới thiệu bởi nhân viên Tamabee,
     * khi thanh toán lần đầu, commission PHẢI được tạo
     */
    @Property(tries = 100)
    void firstPaymentShouldCreateCommission(
            @ForAll("validCompanyId") Long companyId,
            @ForAll("validEmployeeCode") String employeeCode,
            @ForAll("validCommissionAmount") BigDecimal commissionAmount) {

        // Chuẩn bị mock - company chưa có commission
        when(commissionRepository.existsByCompanyIdAndDeletedFalse(companyId)).thenReturn(false);

        // Tạo referrer là nhân viên Tamabee
        UserEntity referrer = createTamabeeEmployee(employeeCode);

        // Tạo company với referrer
        CompanyEntity company = createCompanyWithReferrer(companyId, referrer);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        // Mock commission rate
        when(settingService.getCommissionRate()).thenReturn(commissionAmount);

        // Mock save để trả về entity đã lưu
        when(commissionRepository.save(any(EmployeeCommissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Thực thi
        commissionService.processCommission(companyId);

        // Kiểm tra: commission PHẢI được tạo
        ArgumentCaptor<EmployeeCommissionEntity> captor = ArgumentCaptor.forClass(EmployeeCommissionEntity.class);
        verify(commissionRepository, times(1)).save(captor.capture());

        EmployeeCommissionEntity savedCommission = captor.getValue();
        assertEquals(employeeCode, savedCommission.getEmployeeCode(),
                "Commission phải được gán cho đúng nhân viên");
        assertEquals(companyId, savedCommission.getCompanyId(),
                "Commission phải được gán cho đúng company");
        assertEquals(commissionAmount, savedCommission.getAmount(),
                "Commission amount phải đúng");
        assertEquals(CommissionStatus.PENDING, savedCommission.getStatus(),
                "Commission status phải là PENDING");
    }

    /**
     * Property 6: Commission First Payment Only - Lần thanh toán thứ hai trở đi
     * Với bất kỳ company nào đã có commission,
     * khi thanh toán lần tiếp theo, commission KHÔNG được tạo thêm
     */
    @Property(tries = 100)
    void subsequentPaymentsShouldNotCreateCommission(
            @ForAll("validCompanyId") Long companyId) {

        // Chuẩn bị mock - company ĐÃ có commission
        when(commissionRepository.existsByCompanyIdAndDeletedFalse(companyId)).thenReturn(true);

        // Thực thi
        commissionService.processCommission(companyId);

        // Kiểm tra: KHÔNG được tạo commission mới
        verify(commissionRepository, never()).save(any(EmployeeCommissionEntity.class));
        verify(companyRepository, never()).findById(any());
    }

    /**
     * Property: Company không có người giới thiệu không tạo commission
     */
    @Property(tries = 100)
    void companyWithoutReferrerShouldNotCreateCommission(
            @ForAll("validCompanyId") Long companyId) {

        // Chuẩn bị mock - company chưa có commission
        when(commissionRepository.existsByCompanyIdAndDeletedFalse(companyId)).thenReturn(false);

        // Tạo company KHÔNG có referrer
        CompanyEntity company = createCompanyWithoutReferrer(companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        // Thực thi
        commissionService.processCommission(companyId);

        // Kiểm tra: KHÔNG được tạo commission
        verify(commissionRepository, never()).save(any(EmployeeCommissionEntity.class));
    }

    /**
     * Property: Người giới thiệu không phải nhân viên Tamabee không tạo commission
     */
    @Property(tries = 100)
    void nonTamabeeReferrerShouldNotCreateCommission(
            @ForAll("validCompanyId") Long companyId,
            @ForAll("validEmployeeCode") String employeeCode,
            @ForAll("nonTamabeeRole") UserRole role) {

        // Chuẩn bị mock - company chưa có commission
        when(commissionRepository.existsByCompanyIdAndDeletedFalse(companyId)).thenReturn(false);

        // Tạo referrer KHÔNG phải nhân viên Tamabee
        UserEntity referrer = createUserWithRole(employeeCode, role);

        // Tạo company với referrer
        CompanyEntity company = createCompanyWithReferrer(companyId, referrer);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        // Thực thi
        commissionService.processCommission(companyId);

        // Kiểm tra: KHÔNG được tạo commission
        verify(commissionRepository, never()).save(any(EmployeeCommissionEntity.class));
    }

    // === Helper Methods ===

    private UserEntity createTamabeeEmployee(String employeeCode) {
        UserEntity user = new UserEntity();
        user.setEmployeeCode(employeeCode);
        user.setRole(UserRole.EMPLOYEE_TAMABEE);
        user.setEmail(employeeCode + "@tamabee.com");
        return user;
    }

    private UserEntity createUserWithRole(String employeeCode, UserRole role) {
        UserEntity user = new UserEntity();
        user.setEmployeeCode(employeeCode);
        user.setRole(role);
        user.setEmail(employeeCode + "@example.com");
        return user;
    }

    private CompanyEntity createCompanyWithReferrer(Long companyId, UserEntity referrer) {
        CompanyEntity company = new CompanyEntity();
        company.setId(companyId);
        company.setName("Company " + companyId);
        company.setReferredByEmployee(referrer);
        return company;
    }

    private CompanyEntity createCompanyWithoutReferrer(Long companyId) {
        CompanyEntity company = new CompanyEntity();
        company.setId(companyId);
        company.setName("Company " + companyId);
        company.setReferredByEmployee(null);
        return company;
    }

    // === Generators ===

    @Provide
    Arbitrary<Long> validCompanyId() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<String> validEmployeeCode() {
        return Arbitraries.strings()
                .alpha()
                .ofLength(6)
                .map(String::toUpperCase);
    }

    @Provide
    Arbitrary<BigDecimal> validCommissionAmount() {
        // Số tiền hoa hồng từ 1000 đến 100000 JPY
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("1000"), new BigDecimal("100000"))
                .ofScale(0);
    }

    @Provide
    Arbitrary<UserRole> nonTamabeeRole() {
        // Các role không phải Tamabee
        return Arbitraries.of(
                UserRole.ADMIN_COMPANY,
                UserRole.MANAGER_COMPANY,
                UserRole.EMPLOYEE_COMPANY);
    }
}
