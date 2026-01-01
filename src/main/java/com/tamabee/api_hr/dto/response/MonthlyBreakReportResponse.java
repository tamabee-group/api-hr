package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.BreakType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;
import java.util.List;

/**
 * Response chứa báo cáo giờ giải lao hàng tháng của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyBreakReportResponse {

    // Tháng báo cáo
    private YearMonth reportMonth;

    // ID công ty
    private Long companyId;

    // Tổng số nhân viên có bản ghi giải lao trong tháng
    private Integer totalEmployees;

    // Tổng số ngày làm việc có bản ghi giải lao
    private Integer totalWorkingDays;

    // Tổng số phút giải lao của tất cả nhân viên trong tháng
    private Long totalBreakMinutes;

    // Trung bình số phút giải lao mỗi nhân viên mỗi ngày
    private Double averageBreakMinutesPerDay;

    // Tổng số lần không tuân thủ minimum break
    private Integer totalNonCompliantCount;

    // Tổng số lần vượt quá maximum break
    private Integer totalExceededMaximumCount;

    // Tỷ lệ tuân thủ trung bình (%)
    private Double averageComplianceRate;

    // Loại giải lao áp dụng
    private BreakType breakType;

    // Chi tiết giờ giải lao của từng nhân viên trong tháng
    private List<EmployeeMonthlyBreakDetail> employeeDetails;

    /**
     * Chi tiết giờ giải lao của một nhân viên trong tháng
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeMonthlyBreakDetail {

        private Long employeeId;

        private String employeeCode;

        private String employeeName;

        // Số ngày có bản ghi giải lao
        private Integer workingDays;

        // Tổng số phút giải lao thực tế trong tháng
        private Long totalActualBreakMinutes;

        // Tổng số phút giải lao hiệu lực trong tháng
        private Long totalEffectiveBreakMinutes;

        // Trung bình số phút giải lao mỗi ngày
        private Double averageBreakMinutesPerDay;

        // Số lần không tuân thủ minimum break
        private Integer nonCompliantCount;

        // Số lần vượt quá maximum break
        private Integer exceededMaximumCount;

        // Tỷ lệ tuân thủ (%)
        private Double complianceRate;

        // Có thường xuyên không tuân thủ không (>= 3 lần)
        private Boolean frequentlyNonCompliant;

        // Có thường xuyên vượt quá maximum không (>= 3 lần)
        private Boolean frequentlyExceededMaximum;
    }
}
