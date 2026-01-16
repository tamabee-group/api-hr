package com.tamabee.api_hr.service.admin.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.response.wallet.CommissionSummaryResponse;
import com.tamabee.api_hr.dto.response.wallet.ReferredCompanyResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.admin.interfaces.IEmployeeReferralService;

import lombok.extern.slf4j.Slf4j;

/**
 * Service cho Employee Tamabee xem và theo dõi company đã giới thiệu
 * Sử dụng masterJdbcTemplate vì companies, wallets, plans, employee_commissions nằm trong master database
 * UserRepository dùng tenant DB (tamabee) để query users
 */
@Service
@Slf4j
public class EmployeeReferralServiceImpl implements IEmployeeReferralService {

        private final UserRepository userRepository;
        private final JdbcTemplate masterJdbcTemplate;

        public EmployeeReferralServiceImpl(
                        UserRepository userRepository,
                        @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate) {
                this.userRepository = userRepository;
                this.masterJdbcTemplate = masterJdbcTemplate;
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ReferredCompanyResponse> getReferredCompanies(String employeeCode, Pageable pageable) {
                UserEntity employee = userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode)
                                .orElseThrow(() -> NotFoundException.user(employeeCode));

                return getReferredCompaniesByEmployeeId(employee.getId(), pageable);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ReferredCompanyResponse> getReferredCompaniesByEmployeeId(Long employeeId, Pageable pageable) {
                log.info("getReferredCompaniesByEmployeeId: employeeId={}", employeeId);
                
                // Kiểm tra employee tồn tại
                if (!userRepository.existsByIdAndDeletedFalse(employeeId)) {
                        log.warn("Employee not found: {}", employeeId);
                        throw NotFoundException.user(employeeId);
                }

                // Query companies từ master database
                String countSql = "SELECT COUNT(*) FROM companies WHERE deleted = false AND referred_by_employee_id = ?";
                Integer totalCount = masterJdbcTemplate.queryForObject(countSql, Integer.class, employeeId);
                int total = totalCount != null ? totalCount : 0;
                log.info("Total referred companies for employee {}: {}", employeeId, total);

                if (total == 0) {
                        return new PageImpl<>(new ArrayList<>(), pageable, 0);
                }

                String sql = """
                                SELECT c.id, c.name, c.owner_name, c.email, c.phone, c.status, c.plan_id, c.created_at,
                                       w.balance, w.total_billing, w.next_billing_date,
                                       p.name_vi as plan_name, p.monthly_price as plan_price,
                                       COALESCE((SELECT SUM(amount) FROM wallet_transactions 
                                                 WHERE wallet_id = w.id AND transaction_type = 'DEPOSIT'), 0) as total_deposits,
                                       ec.id as commission_id, ec.amount as commission_amount, 
                                       ec.status as commission_status, ec.paid_at as commission_paid_at
                                FROM companies c
                                LEFT JOIN wallets w ON w.company_id = c.id
                                LEFT JOIN plans p ON p.id = c.plan_id AND p.deleted = false
                                LEFT JOIN employee_commissions ec ON ec.company_id = c.id
                                WHERE c.deleted = false AND c.referred_by_employee_id = ?
                                ORDER BY c.created_at DESC
                                LIMIT ? OFFSET ?
                                """;

                List<ReferredCompanyResponse> companies = masterJdbcTemplate.query(
                                sql,
                                (rs, rowNum) -> {
                                        Long commissionId = rs.getObject("commission_id", Long.class);
                                        
                                        return ReferredCompanyResponse.builder()
                                                        .companyId(rs.getLong("id"))
                                                        .companyName(rs.getString("name"))
                                                        .ownerName(rs.getString("owner_name"))
                                                        .email(rs.getString("email"))
                                                        .phone(rs.getString("phone"))
                                                        .planName(rs.getString("plan_name"))
                                                        .planPrice(rs.getBigDecimal("plan_price"))
                                                        .planExpiryDate(rs.getTimestamp("next_billing_date") != null
                                                                        ? rs.getTimestamp("next_billing_date").toLocalDateTime()
                                                                        : null)
                                                        .status(CompanyStatus.valueOf(rs.getString("status")))
                                                        .currentBalance(rs.getBigDecimal("balance") != null
                                                                        ? rs.getBigDecimal("balance")
                                                                        : BigDecimal.ZERO)
                                                        .totalDeposits(rs.getBigDecimal("total_deposits") != null
                                                                        ? rs.getBigDecimal("total_deposits")
                                                                        : BigDecimal.ZERO)
                                                        .totalBilling(rs.getBigDecimal("total_billing") != null
                                                                        ? rs.getBigDecimal("total_billing")
                                                                        : BigDecimal.ZERO)
                                                        .commissionId(commissionId)
                                                        .commissionAmount(rs.getBigDecimal("commission_amount"))
                                                        .commissionStatus(rs.getString("commission_status") != null
                                                                        ? CommissionStatus.valueOf(rs.getString("commission_status"))
                                                                        : null)
                                                        .commissionPaidAt(rs.getTimestamp("commission_paid_at") != null
                                                                        ? rs.getTimestamp("commission_paid_at").toLocalDateTime()
                                                                        : null)
                                                        .companyCreatedAt(
                                                                        rs.getTimestamp("created_at").toLocalDateTime())
                                                        .build();
                                },
                                employeeId, pageable.getPageSize(), pageable.getOffset());

                return new PageImpl<>(companies, pageable, total);
        }

        @Override
        @Transactional(readOnly = true)
        public CommissionSummaryResponse getCommissionSummary(String employeeCode) {
                UserEntity employee = userRepository.findByEmployeeCodeAndDeletedFalse(employeeCode)
                                .orElseThrow(() -> NotFoundException.user(employeeCode));

                String employeeName = getEmployeeName(employee);

                // Đếm số referrals từ master database
                String countSql = "SELECT COUNT(*) FROM companies WHERE deleted = false AND referred_by_employee_id = ?";
                Integer totalCount = masterJdbcTemplate.queryForObject(countSql, Integer.class, employee.getId());
                int totalReferrals = totalCount != null ? totalCount : 0;

                // Query commission stats từ master database
                String commissionStatsSql = """
                                SELECT 
                                        COUNT(*) as total_commissions,
                                        COALESCE(SUM(amount), 0) as total_amount,
                                        COUNT(*) FILTER (WHERE status = 'PENDING') as pending_commissions,
                                        COALESCE(SUM(amount) FILTER (WHERE status = 'PENDING'), 0) as pending_amount,
                                        COUNT(*) FILTER (WHERE status = 'ELIGIBLE') as eligible_commissions,
                                        COALESCE(SUM(amount) FILTER (WHERE status = 'ELIGIBLE'), 0) as eligible_amount,
                                        COUNT(*) FILTER (WHERE status = 'PAID') as paid_commissions,
                                        COALESCE(SUM(amount) FILTER (WHERE status = 'PAID'), 0) as paid_amount
                                FROM employee_commissions
                                WHERE employee_code = ?
                                """;

                return masterJdbcTemplate.queryForObject(commissionStatsSql, (rs, rowNum) -> CommissionSummaryResponse.builder()
                                .employeeCode(employeeCode)
                                .employeeName(employeeName)
                                .totalReferrals(totalReferrals)
                                .totalCommissions(rs.getLong("total_commissions"))
                                .totalAmount(rs.getBigDecimal("total_amount"))
                                .pendingCommissions(rs.getLong("pending_commissions"))
                                .pendingAmount(rs.getBigDecimal("pending_amount"))
                                .eligibleCommissions(rs.getLong("eligible_commissions"))
                                .eligibleAmount(rs.getBigDecimal("eligible_amount"))
                                .paidCommissions(rs.getLong("paid_commissions"))
                                .paidAmount(rs.getBigDecimal("paid_amount"))
                                .build(), employeeCode);
        }

        @Override
        @Transactional(readOnly = true)
        public CommissionSummaryResponse getCommissionSummaryByEmployeeId(Long employeeId) {
                UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId)
                                .orElseThrow(() -> NotFoundException.user(employeeId));

                return getCommissionSummary(employee.getEmployeeCode());
        }

        private String getEmployeeName(UserEntity user) {
                if (user.getProfile() != null && user.getProfile().getName() != null) {
                        return user.getProfile().getName();
                }
                return user.getEmail();
        }
}
