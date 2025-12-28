package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.TransactionType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO để filter lịch sử giao dịch ví
 */
@Data
public class TransactionFilterRequest {

    // Filter theo loại giao dịch
    private TransactionType transactionType;

    // Filter theo khoảng thời gian
    private LocalDateTime fromDate;

    private LocalDateTime toDate;
}
