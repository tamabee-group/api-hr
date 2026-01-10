package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.dto.response.wallet.CommissionSummaryResponse;
import com.tamabee.api_hr.dto.response.wallet.ReferredCompanyResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.wallet.EmployeeCommissionEntity;
import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.entity.wallet.WalletEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.company.CompanyRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.repository.wallet.EmployeeCommissionRepository;
import com.tamabee.api_hr.repository.wallet.PlanRepository;
import com.tamabee.api_hr.repository.wallet.WalletRepository;
import com.tamabee.api_hr.repository.wallet.WalletTransactionRepository;
import com.tamabee.api_hr.service.admin.interfaces.IEmployeeReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service cho Employee Tamabee xem và theo dõi company đã giới thiệu
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeReferralServiceImpl implements IEmployeeReferralService {

        private final CompanyRepository companyRepository;
        private final UserRepository userRepository;
        private final WalletRepository walletRepository;
        private final WalletTransactionRepository walletTransactionRepository;
        private final EmployeeCommissionRepository commissionRepository;
        private final PlanRepository planRepository;

        @Override
        @Transactional(readOnly = true)
        public Page<ReferredCompanyResponse> getReferredCompanies(String employeeCode, Pageable pageable) {
                // Lấy thông tin employee
                UserEntity employee = userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode)
                                .orElseThrow(() -> NotFoundException.user(employeeCode));

                // Query companies được giới thiệu bởi employee
                Page<CompanyEntity> companies = companyRepository.findByReferredByEmployeeId(employee.getId(),
                                pageable);

                return companies.map(this::toReferredCompanyResponse);
        }

        @Override
        @Transactional(readOnly = true)
        public CommissionSummaryResponse getCommissionSummary(String employeeCode) {
                // Lấy thông tin employee
                UserEntity employee = userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode)
                                .orElseThrow(() -> NotFoundException.user(employeeCode));

                String employeeName = getEmployeeName(employee);

                // Đếm số referrals
                int totalReferrals = companyRepository.countByReferredByEmployeeId(employee.getId());

                // Tổng số commission
                long totalCommissions = commissionRepository.countByEmployeeCode(employeeCode);

                // Tổng số tiền
                BigDecimal totalAmount = commissionRepository.sumAmountByEmployeeCode(employeeCode);

                // Số commission PENDING
                long pendingCommissions = commissionRepository.countByEmployeeCodeAndStatus(
                                employeeCode, CommissionStatus.PENDING);
                BigDecimal pendingAmount = commissionRepository.sumAmountByEmployeeCodeAndStatus(
                                employeeCode, CommissionStatus.PENDING);

                // Số commission ELIGIBLE
                long eligibleCommissions = commissionRepository.countByEmployeeCodeAndStatus(
                                employeeCode, CommissionStatus.ELIGIBLE);
                BigDecimal eligibleAmount = commissionRepository.sumAmountByEmployeeCodeAndStatus(
                                employeeCode, CommissionStatus.ELIGIBLE);

                // Số commission PAID
                long paidCommissions = commissionRepository.countByEmployeeCodeAndStatus(
                                employeeCode, CommissionStatus.PAID);
                BigDecimal paidAmount = commissionRepository.sumAmountByEmployeeCodeAndStatus(
                                employeeCode, CommissionStatus.PAID);

                return CommissionSummaryResponse.builder()
                                .employeeCode(employeeCode)
                                .employeeName(employeeName)
                                .totalReferrals(totalReferrals)
                                .totalCommissions(totalCommissions)
                                .totalAmount(totalAmount)
                                .pendingCommissions(pendingCommissions)
                                .pendingAmount(pendingAmount)
                                .eligibleCommissions(eligibleCommissions)
                                .eligibleAmount(eligibleAmount)
                                .paidCommissions(paidCommissions)
                                .paidAmount(paidAmount)
                                .build();
        }

        /**
         * Chuyển đổi CompanyEntity sang ReferredCompanyResponse
         */
        private ReferredCompanyResponse toReferredCompanyResponse(CompanyEntity company) {
                // Lấy wallet info
                WalletEntity wallet = walletRepository.findByCompanyId(company.getId()).orElse(null);

                BigDecimal currentBalance = BigDecimal.ZERO;
                BigDecimal totalBilling = BigDecimal.ZERO;
                if (wallet != null) {
                        currentBalance = wallet.getBalance();
                        totalBilling = wallet.getTotalBilling();
                }

                // Tính total deposits từ transactions
                BigDecimal totalDeposits = walletTransactionRepository.sumDepositsByCompanyId(company.getId());

                // Lấy plan name
                String planName = null;
                if (company.getPlanId() != null) {
                        planName = planRepository.findByIdAndDeletedFalse(company.getPlanId())
                                        .map(PlanEntity::getNameVi)
                                        .orElse(null);
                }

                // Lấy commission info
                EmployeeCommissionEntity commission = commissionRepository
                                .findByCompanyId(company.getId())
                                .orElse(null);

                Long commissionId = null;
                BigDecimal commissionAmount = null;
                CommissionStatus commissionStatus = null;
                java.time.LocalDateTime commissionPaidAt = null;

                if (commission != null) {
                        commissionId = commission.getId();
                        commissionAmount = commission.getAmount();
                        commissionStatus = commission.getStatus();
                        commissionPaidAt = commission.getPaidAt();
                }

                return ReferredCompanyResponse.builder()
                                .companyId(company.getId())
                                .companyName(company.getName())
                                .ownerName(company.getOwnerName())
                                .planName(planName)
                                .status(company.getStatus())
                                .currentBalance(currentBalance)
                                .totalDeposits(totalDeposits)
                                .totalBilling(totalBilling)
                                .commissionId(commissionId)
                                .commissionAmount(commissionAmount)
                                .commissionStatus(commissionStatus)
                                .commissionPaidAt(commissionPaidAt)
                                .companyCreatedAt(company.getCreatedAt())
                                .build();
        }

        /**
         * Lấy tên nhân viên từ profile
         */
        private String getEmployeeName(UserEntity user) {
                if (user.getProfile() != null && user.getProfile().getName() != null) {
                        return user.getProfile().getName();
                }
                return user.getEmail();
        }
}
