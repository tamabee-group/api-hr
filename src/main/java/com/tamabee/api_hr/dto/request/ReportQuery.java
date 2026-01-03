package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.ContractType;
import com.tamabee.api_hr.enums.SalaryType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Query parameters cho các báo cáo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportQuery {

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    // Lọc theo danh sách nhân viên (optional)
    private List<Long> employeeIds;

    // Lọc theo phòng ban (optional)
    private List<Long> departmentIds;

    // Lọc theo loại hợp đồng (optional)
    private List<ContractType> contractTypes;

    // Lọc theo loại lương (optional)
    private List<SalaryType> salaryTypes;

    // Lọc theo ca làm việc (optional)
    private List<Long> shiftTemplateIds;
}
