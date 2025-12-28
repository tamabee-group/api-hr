package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.CommissionStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO để filter danh sách hoa hồng
 */
@Data
public class CommissionFilterRequest {

    // Filter theo employee code
    private String employeeCode;

    // Filter theo trạng thái
    private CommissionStatus status;

    // Filter theo khoảng thời gian
    private LocalDateTime fromDate;

    private LocalDateTime toDate;
}
