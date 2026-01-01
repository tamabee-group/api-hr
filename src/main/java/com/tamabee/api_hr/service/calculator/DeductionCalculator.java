package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.DeductionConfig;
import com.tamabee.api_hr.dto.config.DeductionRule;
import com.tamabee.api_hr.dto.result.AttendanceSummary;
import com.tamabee.api_hr.dto.result.DeductionItem;
import com.tamabee.api_hr.dto.result.DeductionResult;
import com.tamabee.api_hr.enums.DeductionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calculator tính toán khấu trừ
 * Hỗ trợ: FIXED, PERCENTAGE deductions
 * Áp dụng late/early penalties theo cấu hình
 * Áp dụng theo thứ tự (order) đã cấu hình
 */
@Component
public class DeductionCalculator implements IDeductionCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public DeductionResult calculateDeductions(
            DeductionConfig config,
            AttendanceSummary attendance,
            BigDecimal grossSalary) {

        if (config == null) {
            return DeductionResult.builder().build();
        }

        List<DeductionItem> items = new ArrayList<>();
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal latePenalty = BigDecimal.ZERO;
        BigDecimal earlyLeavePenalty = BigDecimal.ZERO;

        // Tính phạt đi muộn
        if (Boolean.TRUE.equals(config.getEnableLatePenalty()) && attendance != null) {
            latePenalty = calculateLatePenalty(config, attendance);
            if (latePenalty.compareTo(BigDecimal.ZERO) > 0) {
                items.add(DeductionItem.builder()
                        .code("LATE_PENALTY")
                        .name("Phạt đi muộn")
                        .type(DeductionType.FIXED)
                        .amount(latePenalty)
                        .order(0)
                        .build());
                totalDeductions = totalDeductions.add(latePenalty);
            }
        }

        // Tính phạt về sớm
        if (Boolean.TRUE.equals(config.getEnableEarlyLeavePenalty()) && attendance != null) {
            earlyLeavePenalty = calculateEarlyLeavePenalty(config, attendance);
            if (earlyLeavePenalty.compareTo(BigDecimal.ZERO) > 0) {
                items.add(DeductionItem.builder()
                        .code("EARLY_LEAVE_PENALTY")
                        .name("Phạt về sớm")
                        .type(DeductionType.FIXED)
                        .amount(earlyLeavePenalty)
                        .order(0)
                        .build());
                totalDeductions = totalDeductions.add(earlyLeavePenalty);
            }
        }

        // Xử lý các deduction rules theo thứ tự
        if (config.getDeductions() != null && !config.getDeductions().isEmpty()) {
            List<DeductionRule> sortedRules = config.getDeductions().stream()
                    .filter(rule -> rule != null)
                    .sorted(Comparator
                            .comparingInt(rule -> rule.getOrder() != null ? rule.getOrder() : Integer.MAX_VALUE))
                    .toList();

            for (DeductionRule rule : sortedRules) {
                DeductionItem item = processDeductionRule(rule, grossSalary);
                if (item != null && item.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    items.add(item);
                    totalDeductions = totalDeductions.add(item.getAmount());
                }
            }
        }

        return DeductionResult.builder()
                .items(items)
                .totalDeductions(totalDeductions)
                .latePenalty(latePenalty)
                .earlyLeavePenalty(earlyLeavePenalty)
                .build();
    }

    /**
     * Tính phạt đi muộn = số phút đi muộn × rate/phút
     */
    private BigDecimal calculateLatePenalty(DeductionConfig config, AttendanceSummary attendance) {
        if (config.getLatePenaltyPerMinute() == null || attendance.getTotalLateMinutes() == null) {
            return BigDecimal.ZERO;
        }

        return config.getLatePenaltyPerMinute()
                .multiply(BigDecimal.valueOf(attendance.getTotalLateMinutes()))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính phạt về sớm = số phút về sớm × rate/phút
     */
    private BigDecimal calculateEarlyLeavePenalty(DeductionConfig config, AttendanceSummary attendance) {
        if (config.getEarlyLeavePenaltyPerMinute() == null || attendance.getTotalEarlyLeaveMinutes() == null) {
            return BigDecimal.ZERO;
        }

        return config.getEarlyLeavePenaltyPerMinute()
                .multiply(BigDecimal.valueOf(attendance.getTotalEarlyLeaveMinutes()))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Xử lý từng rule khấu trừ
     */
    private DeductionItem processDeductionRule(DeductionRule rule, BigDecimal grossSalary) {
        BigDecimal amount = BigDecimal.ZERO;

        if (rule.getType() == DeductionType.FIXED) {
            amount = rule.getAmount() != null ? rule.getAmount() : BigDecimal.ZERO;
        } else if (rule.getType() == DeductionType.PERCENTAGE) {
            if (rule.getPercentage() != null && grossSalary != null) {
                amount = grossSalary
                        .multiply(rule.getPercentage())
                        .divide(HUNDRED, 0, RoundingMode.HALF_UP);
            }
        }

        return DeductionItem.builder()
                .code(rule.getCode())
                .name(rule.getName())
                .type(rule.getType())
                .amount(amount)
                .order(rule.getOrder())
                .build();
    }
}
