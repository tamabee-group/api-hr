package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.BreakType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response chứa báo cáo giờ giải lao hàng ngày của công ty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyBreakReportResponse {

    // Ngày báo cáo
    private LocalDate reportDate;

    // ID công ty
    private Long companyId;

    // Tổng số nhân viên có bản ghi giải lao
    private Integer totalEmployees;

    // Tổng số phút giải lao của tất cả nhân viên
    private Integer totalBreakMinutes;

    // Trung bình số phút giải lao mỗi nhân viên
    private Double averageBreakMinutes;

    // Số nhân viên tuân thủ minimum break
    private Integer compliantEmployees;

    // Số nhân viên không tuân thủ minimum break
    private Integer nonCompliantEmployees;

    // Tỷ lệ tuân thủ (%)
    private Double complianceRate;

    // Số nhân viên vượt quá maximum break
    private Integer exceededMaximumEmployees;

    // Loại giải lao áp dụng
    private BreakType breakType;

    // Chi tiết giờ giải lao của từng nhân viên
    private List<EmployeeBreakDetail> employeeDetails;

    /**
     * Chi tiết giờ giải lao của một nhân viên trong ngày
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeBreakDetail {

        private Long employeeId;

        private String employeeCode;

        private String employeeName;

        // Tổng số phút giải lao thực tế
        private Integer totalActualBreakMinutes;

        // Tổng số phút giải lao hiệu lực
        private Integer totalEffectiveBreakMinutes;

        // Số lần giải lao
        private Integer breakCount;

        // Có tuân thủ minimum break không
        private Boolean breakCompliant;

        // Số phút giải lao tối thiểu yêu cầu
        private Integer minimumBreakRequired;

        // Có vượt quá maximum break không
        private Boolean exceededMaximum;

        // Danh sách các break sessions của nhân viên trong ngày
        private List<BreakSessionInfo> breakSessions;
    }

    /**
     * Thông tin chi tiết của một break session
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakSessionInfo {

        private Long breakRecordId;

        private Integer breakNumber;

        private LocalDateTime breakStart;

        private LocalDateTime breakEnd;

        private Integer durationMinutes;
    }
}
