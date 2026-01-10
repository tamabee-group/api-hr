package com.tamabee.api_hr.service.calculator.interfaces;

import com.tamabee.api_hr.dto.config.AllowanceConfig;
import com.tamabee.api_hr.dto.result.AllowanceResult;
import com.tamabee.api_hr.dto.result.AttendanceSummary;

/**
 * Interface cho việc tính toán phụ cấp
 */
public interface IAllowanceCalculator {

    /**
     * Tính toán phụ cấp dựa trên cấu hình và tổng hợp chấm công
     *
     * @param config     Cấu hình phụ cấp của công ty
     * @param attendance Tổng hợp chấm công trong kỳ
     * @return Kết quả tính toán phụ cấp
     */
    AllowanceResult calculateAllowances(AllowanceConfig config, AttendanceSummary attendance);
}
