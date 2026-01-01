package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.dto.result.*;

import java.util.List;

/**
 * Interface cho việc tính toán lương tổng hợp
 */
public interface IPayrollCalculator {

    /**
     * Tính toán lương tổng hợp cho nhân viên
     *
     * @param salaryInfo      Thông tin lương của nhân viên
     * @param attendance      Tổng hợp chấm công trong kỳ
     * @param dailyDetails    Chi tiết tăng ca theo ngày
     * @param payrollConfig   Cấu hình tính lương
     * @param overtimeConfig  Cấu hình tăng ca
     * @param allowanceConfig Cấu hình phụ cấp
     * @param deductionConfig Cấu hình khấu trừ
     * @return Kết quả tính toán lương
     */
    PayrollResult calculatePayroll(
            EmployeeSalaryInfo salaryInfo,
            AttendanceSummary attendance,
            List<DailyOvertimeDetail> dailyDetails,
            PayrollConfig payrollConfig,
            OvertimeConfig overtimeConfig,
            AllowanceConfig allowanceConfig,
            DeductionConfig deductionConfig);

    /**
     * Tính toán lương tổng hợp cho nhân viên với break config
     *
     * @param salaryInfo      Thông tin lương của nhân viên
     * @param attendance      Tổng hợp chấm công trong kỳ
     * @param dailyDetails    Chi tiết tăng ca theo ngày
     * @param payrollConfig   Cấu hình tính lương
     * @param overtimeConfig  Cấu hình tăng ca
     * @param allowanceConfig Cấu hình phụ cấp
     * @param deductionConfig Cấu hình khấu trừ
     * @param breakConfig     Cấu hình giờ giải lao
     * @return Kết quả tính toán lương
     */
    PayrollResult calculatePayroll(
            EmployeeSalaryInfo salaryInfo,
            AttendanceSummary attendance,
            List<DailyOvertimeDetail> dailyDetails,
            PayrollConfig payrollConfig,
            OvertimeConfig overtimeConfig,
            AllowanceConfig allowanceConfig,
            DeductionConfig deductionConfig,
            BreakConfig breakConfig);
}
