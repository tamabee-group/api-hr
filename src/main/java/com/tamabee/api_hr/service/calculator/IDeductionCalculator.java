package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.DeductionConfig;
import com.tamabee.api_hr.dto.result.AttendanceSummary;
import com.tamabee.api_hr.dto.result.DeductionResult;

import java.math.BigDecimal;

/**
 * Interface cho việc tính toán khấu trừ
 */
public interface IDeductionCalculator {

    /**
     * Tính toán khấu trừ dựa trên cấu hình và tổng hợp chấm công
     *
     * @param config      Cấu hình khấu trừ của công ty
     * @param attendance  Tổng hợp chấm công trong kỳ
     * @param grossSalary Lương gộp (để tính khấu trừ theo phần trăm)
     * @return Kết quả tính toán khấu trừ
     */
    DeductionResult calculateDeductions(
            DeductionConfig config,
            AttendanceSummary attendance,
            BigDecimal grossSalary);
}
