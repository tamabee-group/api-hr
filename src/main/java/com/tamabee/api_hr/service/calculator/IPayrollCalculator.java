package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.*;
import com.tamabee.api_hr.dto.result.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface cho việc tính toán lương tổng hợp.
 * Bao gồm các phương thức tính: base salary, overtime, break deduction,
 * allowances, deductions, gross salary, net salary.
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

        /**
         * Tính lương cơ bản theo loại lương.
         * - MONTHLY: prorate theo số ngày làm việc thực tế / số ngày chuẩn
         * - DAILY: dailyRate × workingDays
         * - HOURLY: hourlyRate × workingHours
         * - SHIFT_BASED: shiftRate × numberOfShifts
         *
         * @param salaryInfo    Thông tin lương của nhân viên
         * @param attendance    Tổng hợp chấm công trong kỳ
         * @param payrollConfig Cấu hình tính lương
         * @return Lương cơ bản đã tính
         */
        BigDecimal calculateBaseSalary(
                        EmployeeSalaryInfo salaryInfo,
                        AttendanceSummary attendance,
                        PayrollConfig payrollConfig);

        /**
         * Tính tiền tăng ca dựa trên chi tiết tăng ca và cấu hình.
         * Áp dụng các hệ số: regular, night, holiday, weekend.
         *
         * @param dailyDetails   Chi tiết tăng ca theo ngày
         * @param overtimeConfig Cấu hình tăng ca
         * @param hourlyRate     Lương theo giờ
         * @return Kết quả tính toán tăng ca
         */
        OvertimeResult calculateOvertime(
                        List<DailyOvertimeDetail> dailyDetails,
                        OvertimeConfig overtimeConfig,
                        BigDecimal hourlyRate);

        /**
         * Tính khấu trừ giờ giải lao.
         * Chỉ áp dụng khi breakType = UNPAID.
         *
         * @param totalBreakMinutes Tổng số phút giải lao
         * @param hourlyRate        Lương theo giờ
         * @param breakConfig       Cấu hình giờ giải lao
         * @return Số tiền khấu trừ
         */
        BigDecimal calculateBreakDeduction(
                        Integer totalBreakMinutes,
                        BigDecimal hourlyRate,
                        BreakConfig breakConfig);

        /**
         * Tính tổng phụ cấp dựa trên cấu hình và chấm công.
         *
         * @param allowanceConfig Cấu hình phụ cấp
         * @param attendance      Tổng hợp chấm công
         * @return Kết quả tính toán phụ cấp
         */
        AllowanceResult calculateAllowances(
                        AllowanceConfig allowanceConfig,
                        AttendanceSummary attendance);

        /**
         * Tính tổng khấu trừ dựa trên cấu hình, chấm công và lương gộp.
         * Bao gồm: phạt đi muộn, phạt về sớm, các khấu trừ cố định/phần trăm.
         *
         * @param deductionConfig Cấu hình khấu trừ
         * @param attendance      Tổng hợp chấm công
         * @param grossSalary     Lương gộp (để tính khấu trừ theo phần trăm)
         * @return Kết quả tính toán khấu trừ
         */
        DeductionResult calculateDeductions(
                        DeductionConfig deductionConfig,
                        AttendanceSummary attendance,
                        BigDecimal grossSalary);

        /**
         * Tính lương gộp = baseSalary + overtimePay + allowances
         *
         * @param baseSalary      Lương cơ bản
         * @param overtimePay     Tiền tăng ca
         * @param totalAllowances Tổng phụ cấp
         * @return Lương gộp
         */
        BigDecimal calculateGrossSalary(
                        BigDecimal baseSalary,
                        BigDecimal overtimePay,
                        BigDecimal totalAllowances);

        /**
         * Tính lương thực nhận = grossSalary - totalDeductions
         *
         * @param grossSalary     Lương gộp
         * @param totalDeductions Tổng khấu trừ
         * @param payrollConfig   Cấu hình tính lương (để làm tròn)
         * @return Lương thực nhận
         */
        BigDecimal calculateNetSalary(
                        BigDecimal grossSalary,
                        BigDecimal totalDeductions,
                        PayrollConfig payrollConfig);

        /**
         * Tính lương theo giờ từ thông tin lương.
         * Dùng để tính overtime và break deduction.
         *
         * @param salaryInfo    Thông tin lương
         * @param payrollConfig Cấu hình tính lương
         * @return Lương theo giờ
         */
        BigDecimal calculateHourlyRate(
                        EmployeeSalaryInfo salaryInfo,
                        PayrollConfig payrollConfig);

        /**
         * Tính phạt đi muộn/về sớm.
         *
         * @param deductionConfig Cấu hình khấu trừ
         * @param attendance      Tổng hợp chấm công
         * @return Tổng tiền phạt
         */
        BigDecimal calculatePenalties(
                        DeductionConfig deductionConfig,
                        AttendanceSummary attendance);
}
