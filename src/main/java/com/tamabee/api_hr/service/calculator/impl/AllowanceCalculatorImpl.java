package com.tamabee.api_hr.service.calculator.impl;

import com.tamabee.api_hr.dto.config.AllowanceCondition;
import com.tamabee.api_hr.dto.config.AllowanceConfig;
import com.tamabee.api_hr.dto.config.AllowanceRule;
import com.tamabee.api_hr.dto.result.AllowanceItem;
import com.tamabee.api_hr.dto.result.AllowanceResult;
import com.tamabee.api_hr.dto.result.AttendanceSummary;
import com.tamabee.api_hr.enums.AllowanceType;
import com.tamabee.api_hr.service.calculator.interfaces.IAllowanceCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculator tính toán phụ cấp
 * Hỗ trợ: FIXED, CONDITIONAL, ONE_TIME allowances
 * Áp dụng eligibility rules theo điều kiện
 */
@Component
public class AllowanceCalculatorImpl implements IAllowanceCalculator {

    @Override
    public AllowanceResult calculateAllowances(AllowanceConfig config, AttendanceSummary attendance) {
        if (config == null || config.getAllowances() == null || config.getAllowances().isEmpty()) {
            return AllowanceResult.builder().build();
        }

        List<AllowanceItem> items = new ArrayList<>();
        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal taxableAllowances = BigDecimal.ZERO;
        BigDecimal nonTaxableAllowances = BigDecimal.ZERO;

        for (AllowanceRule rule : config.getAllowances()) {
            if (rule == null || rule.getAmount() == null)
                continue;

            AllowanceItem item = processAllowanceRule(rule, attendance);
            items.add(item);

            // Chỉ cộng vào tổng nếu đủ điều kiện
            if (item.getIneligibleReason() == null) {
                BigDecimal amount = item.getAmount();
                totalAllowances = totalAllowances.add(amount);

                if (Boolean.TRUE.equals(item.getTaxable())) {
                    taxableAllowances = taxableAllowances.add(amount);
                } else {
                    nonTaxableAllowances = nonTaxableAllowances.add(amount);
                }
            }
        }

        return AllowanceResult.builder()
                .items(items)
                .totalAllowances(totalAllowances)
                .taxableAllowances(taxableAllowances)
                .nonTaxableAllowances(nonTaxableAllowances)
                .build();
    }

    /**
     * Xử lý từng rule phụ cấp
     */
    private AllowanceItem processAllowanceRule(AllowanceRule rule, AttendanceSummary attendance) {
        AllowanceItem.AllowanceItemBuilder builder = AllowanceItem.builder()
                .code(rule.getCode())
                .name(rule.getName())
                .type(rule.getType())
                .amount(rule.getAmount())
                .taxable(rule.getTaxable());

        // Kiểm tra điều kiện cho CONDITIONAL type
        if (rule.getType() == AllowanceType.CONDITIONAL && rule.getCondition() != null) {
            String ineligibleReason = checkEligibility(rule.getCondition(), attendance);
            if (ineligibleReason != null) {
                builder.ineligibleReason(ineligibleReason);
                builder.amount(BigDecimal.ZERO);
            }
        }

        return builder.build();
    }

    /**
     * Kiểm tra điều kiện đủ để nhận phụ cấp
     * 
     * @return null nếu đủ điều kiện, ngược lại trả về lý do không đủ
     */
    private String checkEligibility(AllowanceCondition condition, AttendanceSummary attendance) {
        if (attendance == null) {
            return "Không có dữ liệu chấm công";
        }

        // Kiểm tra số ngày làm việc tối thiểu
        if (condition.getMinWorkingDays() != null) {
            int workingDays = attendance.getWorkingDays() != null ? attendance.getWorkingDays() : 0;
            if (workingDays < condition.getMinWorkingDays()) {
                return String.format("Số ngày làm việc (%d) chưa đạt tối thiểu (%d)",
                        workingDays, condition.getMinWorkingDays());
            }
        }

        // Kiểm tra số giờ làm việc tối thiểu
        if (condition.getMinWorkingHours() != null) {
            int workingHours = attendance.getWorkingHours() != null ? attendance.getWorkingHours() : 0;
            if (workingHours < condition.getMinWorkingHours()) {
                return String.format("Số giờ làm việc (%d) chưa đạt tối thiểu (%d)",
                        workingHours, condition.getMinWorkingHours());
            }
        }

        // Kiểm tra không vắng mặt
        if (Boolean.TRUE.equals(condition.getNoAbsence())) {
            int absenceDays = attendance.getAbsenceDays() != null ? attendance.getAbsenceDays() : 0;
            if (absenceDays > 0) {
                return String.format("Có %d ngày vắng mặt", absenceDays);
            }
        }

        // Kiểm tra không đi muộn
        if (Boolean.TRUE.equals(condition.getNoLateArrival())) {
            int lateCount = attendance.getLateCount() != null ? attendance.getLateCount() : 0;
            if (lateCount > 0) {
                return String.format("Có %d lần đi muộn", lateCount);
            }
        }

        // Kiểm tra không về sớm
        if (Boolean.TRUE.equals(condition.getNoEarlyLeave())) {
            int earlyLeaveCount = attendance.getEarlyLeaveCount() != null ? attendance.getEarlyLeaveCount() : 0;
            if (earlyLeaveCount > 0) {
                return String.format("Có %d lần về sớm", earlyLeaveCount);
            }
        }

        return null; // Đủ điều kiện
    }
}
