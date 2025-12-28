package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.dto.request.DepositRequestCreateRequest;
import com.tamabee.api_hr.dto.request.RefundRequest;
import com.tamabee.api_hr.dto.request.RejectRequest;
import com.tamabee.api_hr.dto.response.DepositRequestResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.DepositRequestEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.entity.wallet.WalletTransactionEntity;
import com.tamabee.api_hr.enums.DepositStatus;
import com.tamabee.api_hr.enums.TransactionType;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.mapper.admin.DepositRequestMapper;
import com.tamabee.api_hr.mapper.admin.WalletMapper;
import com.tamabee.api_hr.mapper.admin.WalletTransactionMapper;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.DepositRequestRepository;
import com.tamabee.api_hr.repository.PlanRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.repository.WalletRepository;
import com.tamabee.api_hr.repository.WalletTransactionRepository;
import com.tamabee.api_hr.service.admin.impl.DepositRequestServiceImpl;
import com.tamabee.api_hr.service.admin.impl.WalletServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho DepositRequestService và Input Validation
 * Feature: wallet-management, Property 3: Input Validation
 * Validates: Requirements 5.3, 5.4, 8.3, 10.5
 */
public class DepositRequestServicePropertyTest {

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

        // Mocks cho WalletService tests
        @Mock
        private WalletRepository walletRepository;

        @Mock
        private WalletTransactionRepository walletTransactionRepository;

        @Mock
        private PlanRepository planRepository;

        private DepositRequestMapper depositRequestMapper;
        private DepositRequestServiceImpl depositRequestService;
        private WalletMapper walletMapper;
        private WalletTransactionMapper walletTransactionMapper;
        private WalletServiceImpl walletServiceImpl;

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

                // Setup WalletService cho refund validation tests
                walletMapper = new WalletMapper();
                walletTransactionMapper = new WalletTransactionMapper();
                walletServiceImpl = new WalletServiceImpl(
                                walletRepository,
                                walletTransactionRepository,
                                companyRepository,
                                planRepository,
                                userRepository,
                                walletMapper,
                                walletTransactionMapper);
        }

        // ==================== Property 3: Input Validation ====================

        /**
         * Property 3: Input Validation - Invalid Amount
         * Với bất kỳ request nào có amount <= 0, service PHẢI throw BadRequestException
         * 
         * Validates: Requirements 5.3
         */
        @Property(tries = 100)
        void createWithInvalidAmountShouldThrowBadRequest(
                        @ForAll("invalidAmounts") BigDecimal invalidAmount,
                        @ForAll("validTransferProofUrls") String transferProofUrl) {

                // Setup security context
                setupSecurityContext("test@company.com");
                setupCurrentUser(1L, "EMP001", 100L);

                // Tạo request với amount không hợp lệ
                DepositRequestCreateRequest request = new DepositRequestCreateRequest();
                request.setAmount(invalidAmount);
                request.setTransferProofUrl(transferProofUrl);

                // Kiểm tra: phải throw BadRequestException
                assertThrows(BadRequestException.class,
                                () -> depositRequestService.create(request),
                                "Amount <= 0 phải throw BadRequestException");

                // Xác nhận không có deposit request nào được lưu
                verify(depositRequestRepository, never()).save(any(DepositRequestEntity.class));
        }

        /**
         * Property 3: Input Validation - Empty Transfer Proof URL
         * Với bất kỳ request nào có transferProofUrl rỗng, service PHẢI throw
         * BadRequestException
         * 
         * Validates: Requirements 5.4
         */
        @Property(tries = 100)
        void createWithEmptyTransferProofShouldThrowBadRequest(
                        @ForAll("validAmounts") BigDecimal validAmount,
                        @ForAll("emptyOrBlankStrings") String emptyTransferProof) {

                // Setup security context
                setupSecurityContext("test@company.com");
                setupCurrentUser(1L, "EMP001", 100L);

                // Tạo request với transferProofUrl rỗng
                DepositRequestCreateRequest request = new DepositRequestCreateRequest();
                request.setAmount(validAmount);
                request.setTransferProofUrl(emptyTransferProof);

                // Kiểm tra: phải throw BadRequestException
                assertThrows(BadRequestException.class,
                                () -> depositRequestService.create(request),
                                "TransferProofUrl rỗng phải throw BadRequestException");

                // Xác nhận không có deposit request nào được lưu
                verify(depositRequestRepository, never()).save(any(DepositRequestEntity.class));
        }

        /**
         * Property 3: Input Validation - Empty Rejection Reason
         * Với bất kỳ request reject nào có rejectionReason rỗng, service PHẢI throw
         * BadRequestException
         * 
         * Validates: Requirements 8.3
         */
        @Property(tries = 100)
        void rejectWithEmptyReasonShouldThrowBadRequest(
                        @ForAll("validDepositIds") Long depositId,
                        @ForAll("emptyOrBlankStrings") String emptyReason) {

                // Setup security context
                setupSecurityContext("admin@tamabee.com");
                setupCurrentUser(1L, "ADM001", 0L);

                // Tạo deposit request entity với status PENDING
                DepositRequestEntity entity = createDepositRequestEntity(depositId, 100L, DepositStatus.PENDING);
                when(depositRequestRepository.findByIdAndDeletedFalse(depositId))
                                .thenReturn(Optional.of(entity));

                // Tạo reject request với reason rỗng
                RejectRequest request = new RejectRequest();
                request.setRejectionReason(emptyReason);

                // Kiểm tra: phải throw BadRequestException
                assertThrows(BadRequestException.class,
                                () -> depositRequestService.reject(depositId, request),
                                "RejectionReason rỗng phải throw BadRequestException");

                // Xác nhận deposit request không bị thay đổi
                verify(depositRequestRepository, never()).save(any(DepositRequestEntity.class));
        }

        /**
         * Property 3: Input Validation - Valid Input Should Succeed
         * Với bất kỳ request nào có amount > 0 và transferProofUrl không rỗng,
         * service PHẢI tạo deposit request thành công
         * 
         * Validates: Requirements 5.1, 5.2
         */
        @Property(tries = 100)
        void createWithValidInputShouldSucceed(
                        @ForAll("validAmounts") BigDecimal validAmount,
                        @ForAll("validTransferProofUrls") String validTransferProof) {

                // Setup security context
                setupSecurityContext("test@company.com");
                setupCurrentUser(1L, "EMP001", 100L);

                // Setup company
                CompanyEntity company = new CompanyEntity();
                company.setId(100L);
                company.setName("Test Company");
                when(companyRepository.findById(100L)).thenReturn(Optional.of(company));

                // Setup repository save
                when(depositRequestRepository.save(any(DepositRequestEntity.class)))
                                .thenAnswer(invocation -> {
                                        DepositRequestEntity entity = invocation.getArgument(0);
                                        entity.setId(1L);
                                        return entity;
                                });

                // Tạo request hợp lệ
                DepositRequestCreateRequest request = new DepositRequestCreateRequest();
                request.setAmount(validAmount);
                request.setTransferProofUrl(validTransferProof);

                // Thực thi
                DepositRequestResponse response = depositRequestService.create(request);

                // Kiểm tra
                assertNotNull(response, "Response không được null");
                assertEquals(validAmount, response.getAmount(), "Amount phải khớp");
                assertEquals(validTransferProof, response.getTransferProofUrl(), "TransferProofUrl phải khớp");
                assertEquals(DepositStatus.PENDING, response.getStatus(), "Status phải là PENDING");
                assertEquals("EMP001", response.getRequestedBy(), "RequestedBy phải là employeeCode của user hiện tại");

                // Xác nhận deposit request được lưu
                verify(depositRequestRepository, times(1)).save(any(DepositRequestEntity.class));
        }

        // ==================== Property 3: Input Validation - Refund
        // ====================

        /**
         * Property 3: Input Validation - Invalid Refund Amount
         * Với bất kỳ refund request nào có amount <= 0, service PHẢI throw
         * BadRequestException
         * 
         * Validates: Requirements 10.5
         */
        @Property(tries = 100)
        void refundWithInvalidAmountShouldThrowBadRequest(
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("invalidAmounts") BigDecimal invalidAmount,
                        @ForAll("validRefundReasons") String reason) {

                // Setup wallet
                WalletEntity wallet = createWalletEntity(companyId, BigDecimal.valueOf(100000));
                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));

                // Tạo refund request với amount không hợp lệ
                RefundRequest request = new RefundRequest();
                request.setAmount(invalidAmount);
                request.setReason(reason);

                // Kiểm tra: phải throw BadRequestException
                assertThrows(BadRequestException.class,
                                () -> walletServiceImpl.createRefund(companyId, request),
                                "Refund amount <= 0 phải throw BadRequestException");

                // Xác nhận không có transaction nào được lưu
                verify(walletTransactionRepository, never()).save(any(WalletTransactionEntity.class));

                // Xác nhận wallet balance không thay đổi
                assertEquals(BigDecimal.valueOf(100000), wallet.getBalance(),
                                "Wallet balance không được thay đổi khi refund thất bại");
        }

        /**
         * Property 3: Input Validation - Valid Refund Should Succeed
         * Với bất kỳ refund request nào có amount > 0 và reason không rỗng,
         * service PHẢI tạo refund thành công
         * 
         * Validates: Requirements 10.1, 10.2
         */
        @Property(tries = 100)
        void refundWithValidInputShouldSucceed(
                        @ForAll("validCompanyIds") Long companyId,
                        @ForAll("validAmounts") BigDecimal validAmount,
                        @ForAll("validRefundReasons") String validReason) {

                // Setup wallet
                BigDecimal initialBalance = BigDecimal.valueOf(100000);
                WalletEntity wallet = createWalletEntity(companyId, initialBalance);
                when(walletRepository.findByCompanyId(companyId))
                                .thenReturn(Optional.of(wallet));
                when(walletRepository.save(any(WalletEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(walletTransactionRepository.save(any(WalletTransactionEntity.class)))
                                .thenAnswer(invocation -> {
                                        WalletTransactionEntity entity = invocation.getArgument(0);
                                        entity.setId(1L);
                                        return entity;
                                });

                // Tạo refund request hợp lệ
                RefundRequest request = new RefundRequest();
                request.setAmount(validAmount);
                request.setReason(validReason);

                // Thực thi
                var response = walletServiceImpl.createRefund(companyId, request);

                // Kiểm tra
                assertNotNull(response, "Response không được null");
                assertEquals(validAmount, response.getAmount(), "Amount phải khớp");
                assertEquals(TransactionType.REFUND, response.getTransactionType(), "Type phải là REFUND");

                // Kiểm tra balance được cập nhật đúng
                BigDecimal expectedBalance = initialBalance.add(validAmount);
                assertEquals(expectedBalance, wallet.getBalance(), "Balance phải được cộng thêm amount");

                // Xác nhận transaction được lưu
                verify(walletTransactionRepository, times(1)).save(any(WalletTransactionEntity.class));
        }

        // ==================== Property 4: Status Transition Validation
        // ====================

        /**
         * Property 4: Status Transition Validation - Approve Non-Pending
         * Với bất kỳ deposit request nào có status != PENDING, approve PHẢI throw
         * BadRequestException
         * 
         * Validates: Requirements 7.6
         */
        @Property(tries = 100)
        void approveNonPendingDepositShouldThrowBadRequest(
                        @ForAll("validDepositIds") Long depositId,
                        @ForAll("nonPendingStatuses") DepositStatus nonPendingStatus) {

                // Setup security context
                setupSecurityContext("admin@tamabee.com");
                setupCurrentUser(1L, "ADM001", 0L);

                // Tạo deposit request entity với status không phải PENDING
                DepositRequestEntity entity = createDepositRequestEntity(depositId, 100L, nonPendingStatus);
                when(depositRequestRepository.findByIdAndDeletedFalse(depositId))
                                .thenReturn(Optional.of(entity));

                // Kiểm tra: phải throw BadRequestException
                assertThrows(BadRequestException.class,
                                () -> depositRequestService.approve(depositId),
                                "Approve deposit với status " + nonPendingStatus + " phải throw BadRequestException");

                // Xác nhận deposit request không bị thay đổi
                verify(depositRequestRepository, never()).save(any(DepositRequestEntity.class));

                // Xác nhận wallet balance không bị thay đổi
                verify(walletService, never()).addBalance(any(), any(), any(), any(), any());
        }

        /**
         * Property 4: Status Transition Validation - Reject Non-Pending
         * Với bất kỳ deposit request nào có status != PENDING, reject PHẢI throw
         * BadRequestException
         * 
         * Validates: Requirements 8.5
         */
        @Property(tries = 100)
        void rejectNonPendingDepositShouldThrowBadRequest(
                        @ForAll("validDepositIds") Long depositId,
                        @ForAll("nonPendingStatuses") DepositStatus nonPendingStatus,
                        @ForAll("validRejectionReasons") String validReason) {

                // Setup security context
                setupSecurityContext("admin@tamabee.com");
                setupCurrentUser(1L, "ADM001", 0L);

                // Tạo deposit request entity với status không phải PENDING
                DepositRequestEntity entity = createDepositRequestEntity(depositId, 100L, nonPendingStatus);
                when(depositRequestRepository.findByIdAndDeletedFalse(depositId))
                                .thenReturn(Optional.of(entity));

                // Tạo reject request với reason hợp lệ
                RejectRequest request = new RejectRequest();
                request.setRejectionReason(validReason);

                // Kiểm tra: phải throw BadRequestException
                assertThrows(BadRequestException.class,
                                () -> depositRequestService.reject(depositId, request),
                                "Reject deposit với status " + nonPendingStatus + " phải throw BadRequestException");

                // Xác nhận deposit request không bị thay đổi
                verify(depositRequestRepository, never()).save(any(DepositRequestEntity.class));
        }

        /**
         * Property 4: Status Transition Validation - Approve Pending Should Succeed
         * Với bất kỳ deposit request nào có status = PENDING, approve PHẢI thành công
         * và chuyển status sang APPROVED
         * 
         * Validates: Requirements 7.1, 7.2
         */
        @Property(tries = 100)
        void approvePendingDepositShouldSucceed(
                        @ForAll("validDepositIds") Long depositId,
                        @ForAll("validAmounts") BigDecimal amount) {

                // Setup security context
                setupSecurityContext("admin@tamabee.com");
                setupCurrentUser(1L, "ADM001", 0L);

                // Tạo deposit request entity với status PENDING
                DepositRequestEntity entity = createDepositRequestEntity(depositId, 100L, DepositStatus.PENDING);
                entity.setAmount(amount);
                when(depositRequestRepository.findByIdAndDeletedFalse(depositId))
                                .thenReturn(Optional.of(entity));
                when(depositRequestRepository.save(any(DepositRequestEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Setup company cho response
                CompanyEntity company = new CompanyEntity();
                company.setId(100L);
                company.setName("Test Company");
                when(companyRepository.findById(100L)).thenReturn(Optional.of(company));

                // Thực thi
                DepositRequestResponse response = depositRequestService.approve(depositId);

                // Kiểm tra
                assertNotNull(response, "Response không được null");
                assertEquals(DepositStatus.APPROVED, response.getStatus(), "Status phải là APPROVED");
                assertEquals("ADM001", response.getApprovedBy(), "ApprovedBy phải là employeeCode của người duyệt");

                // Xác nhận deposit request được lưu
                verify(depositRequestRepository, times(1)).save(any(DepositRequestEntity.class));

                // Xác nhận wallet balance được cập nhật
                verify(walletService, times(1)).addBalance(eq(100L), eq(amount), any(), eq(TransactionType.DEPOSIT),
                                eq(depositId));
        }

        // ==================== Property 5: Reject Does Not Change Balance
        // ====================

        /**
         * Property 5: Reject Does Not Change Balance
         * Với bất kỳ deposit request nào bị reject, wallet balance PHẢI không thay đổi
         * và không có wallet transaction nào được tạo
         * 
         * Validates: Requirements 8.6
         */
        @Property(tries = 100)
        void rejectDepositShouldNotChangeBalance(
                        @ForAll("validDepositIds") Long depositId,
                        @ForAll("validAmounts") BigDecimal amount,
                        @ForAll("validRejectionReasons") String validReason) {

                // Setup security context
                setupSecurityContext("admin@tamabee.com");
                setupCurrentUser(1L, "ADM001", 0L);

                // Tạo deposit request entity với status PENDING
                DepositRequestEntity entity = createDepositRequestEntity(depositId, 100L, DepositStatus.PENDING);
                entity.setAmount(amount);
                when(depositRequestRepository.findByIdAndDeletedFalse(depositId))
                                .thenReturn(Optional.of(entity));
                when(depositRequestRepository.save(any(DepositRequestEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Setup company cho response
                CompanyEntity company = new CompanyEntity();
                company.setId(100L);
                company.setName("Test Company");
                when(companyRepository.findById(100L)).thenReturn(Optional.of(company));

                // Tạo reject request
                RejectRequest request = new RejectRequest();
                request.setRejectionReason(validReason);

                // Thực thi
                DepositRequestResponse response = depositRequestService.reject(depositId, request);

                // Kiểm tra
                assertNotNull(response, "Response không được null");
                assertEquals(DepositStatus.REJECTED, response.getStatus(), "Status phải là REJECTED");
                assertEquals(validReason, response.getRejectionReason(), "RejectionReason phải khớp");

                // Xác nhận deposit request được lưu
                verify(depositRequestRepository, times(1)).save(any(DepositRequestEntity.class));

                // QUAN TRỌNG: Xác nhận wallet balance KHÔNG bị thay đổi
                verify(walletService, never()).addBalance(any(), any(), any(), any(), any());
                verify(walletService, never()).deductBalance(any(), any(), any(), any(), any());
        }

        /**
         * Property 5: Reject Does Not Change Balance - Multiple Rejects
         * Với bất kỳ số lượng deposit requests nào bị reject, tổng wallet balance
         * PHẢI không thay đổi
         * 
         * Validates: Requirements 8.6
         */
        @Property(tries = 50)
        void multipleRejectsShouldNotChangeBalance(
                        @ForAll("validDepositIds") Long depositId1,
                        @ForAll("validAmounts") BigDecimal amount1,
                        @ForAll("validRejectionReasons") String reason1) {

                // Setup security context
                setupSecurityContext("admin@tamabee.com");
                setupCurrentUser(1L, "ADM001", 0L);

                // Tạo deposit request entity với status PENDING
                DepositRequestEntity entity = createDepositRequestEntity(depositId1, 100L, DepositStatus.PENDING);
                entity.setAmount(amount1);
                when(depositRequestRepository.findByIdAndDeletedFalse(depositId1))
                                .thenReturn(Optional.of(entity));
                when(depositRequestRepository.save(any(DepositRequestEntity.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Setup company cho response
                CompanyEntity company = new CompanyEntity();
                company.setId(100L);
                company.setName("Test Company");
                when(companyRepository.findById(100L)).thenReturn(Optional.of(company));

                // Reject request
                RejectRequest request = new RejectRequest();
                request.setRejectionReason(reason1);

                // Thực thi reject
                depositRequestService.reject(depositId1, request);

                // QUAN TRỌNG: Xác nhận wallet balance KHÔNG bị thay đổi sau reject
                verify(walletService, never()).addBalance(any(), any(), any(), any(), any());
                verify(walletService, never()).deductBalance(any(), any(), any(), any(), any());
        }

        // ==================== Generators ====================

        @Provide
        Arbitrary<BigDecimal> invalidAmounts() {
                return Arbitraries.oneOf(
                                // Số âm
                                Arbitraries.bigDecimals()
                                                .between(BigDecimal.valueOf(-1000000), BigDecimal.valueOf(-0.01))
                                                .ofScale(2),
                                // Zero
                                Arbitraries.just(BigDecimal.ZERO),
                                // Null case sẽ được test riêng
                                Arbitraries.just(null));
        }

        @Provide
        Arbitrary<BigDecimal> validAmounts() {
                return Arbitraries.bigDecimals()
                                .between(BigDecimal.valueOf(1000), BigDecimal.valueOf(100000000))
                                .ofScale(2);
        }

        @Provide
        Arbitrary<String> validTransferProofUrls() {
                return Arbitraries.strings()
                                .withCharRange('a', 'z')
                                .ofMinLength(10)
                                .ofMaxLength(100)
                                .map(s -> "https://storage.example.com/proof/" + s + ".jpg");
        }

        @Provide
        Arbitrary<String> emptyOrBlankStrings() {
                return Arbitraries.oneOf(
                                Arbitraries.just(null),
                                Arbitraries.just(""),
                                Arbitraries.just("   "),
                                Arbitraries.just("\t"),
                                Arbitraries.just("\n"),
                                Arbitraries.just("  \t\n  "));
        }

        @Provide
        Arbitrary<Long> validDepositIds() {
                return Arbitraries.longs().between(1L, 10000L);
        }

        @Provide
        Arbitrary<Long> validCompanyIds() {
                return Arbitraries.longs().between(1L, 10000L);
        }

        @Provide
        Arbitrary<String> validRefundReasons() {
                return Arbitraries.strings()
                                .withCharRange('a', 'z')
                                .ofMinLength(5)
                                .ofMaxLength(100)
                                .map(s -> "Lý do hoàn tiền: " + s);
        }

        @Provide
        Arbitrary<DepositStatus> nonPendingStatuses() {
                return Arbitraries.of(DepositStatus.APPROVED, DepositStatus.REJECTED);
        }

        @Provide
        Arbitrary<String> validRejectionReasons() {
                return Arbitraries.strings()
                                .withCharRange('a', 'z')
                                .ofMinLength(5)
                                .ofMaxLength(200)
                                .map(s -> "Lý do từ chối: " + s);
        }

        // ==================== Helper Methods ====================

        private void setupSecurityContext(String email) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                email, null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN_COMPANY")));
                SecurityContextHolder.getContext().setAuthentication(auth);
        }

        private UserEntity setupCurrentUser(Long userId, String employeeCode, Long companyId) {
                UserEntity user = new UserEntity();
                user.setId(userId);
                user.setEmail(SecurityContextHolder.getContext().getAuthentication().getName());
                user.setEmployeeCode(employeeCode);
                user.setCompanyId(companyId);
                user.setRole(UserRole.ADMIN_COMPANY);

                when(userRepository.findByEmailAndDeletedFalse(user.getEmail())).thenReturn(Optional.of(user));
                when(userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode)).thenReturn(Optional.of(user));

                return user;
        }

        private DepositRequestEntity createDepositRequestEntity(Long id, Long companyId, DepositStatus status) {
                DepositRequestEntity entity = new DepositRequestEntity();
                entity.setId(id);
                entity.setCompanyId(companyId);
                entity.setAmount(BigDecimal.valueOf(100000));
                entity.setTransferProofUrl("https://example.com/proof.jpg");
                entity.setStatus(status);
                entity.setRequestedBy("EMP001");
                entity.setDeleted(false);
                return entity;
        }

        private WalletEntity createWalletEntity(Long companyId, BigDecimal balance) {
                WalletEntity wallet = new WalletEntity();
                wallet.setId(companyId);
                wallet.setCompanyId(companyId);
                wallet.setBalance(balance);
                return wallet;
        }
}
