package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.DepositStatus;
import lombok.Data;

/**
 * Request DTO để filter danh sách yêu cầu nạp tiền
 */
@Data
public class DepositFilterRequest {

    // Filter theo trạng thái
    private DepositStatus status;

    // Filter theo company (chỉ dành cho admin)
    private Long companyId;
}
