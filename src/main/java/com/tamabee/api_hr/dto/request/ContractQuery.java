package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.ContractStatus;
import com.tamabee.api_hr.enums.ContractType;
import lombok.Data;

import java.time.LocalDate;

/**
 * Query DTO cho việc lọc danh sách hợp đồng
 */
@Data
public class ContractQuery {

    // Lọc theo trạng thái
    private ContractStatus status;

    // Lọc theo loại hợp đồng
    private ContractType contractType;

    // Lọc theo khoảng thời gian
    private LocalDate startDateFrom;
    private LocalDate startDateTo;

    // Lọc theo nhân viên
    private Long employeeId;
}
