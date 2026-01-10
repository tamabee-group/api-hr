package com.tamabee.api_hr.dto.request.attendance;

import com.tamabee.api_hr.enums.SwapRequestStatus;
import lombok.Data;

import java.time.LocalDate;

/**
 * Query DTO để lọc danh sách yêu cầu đổi ca.
 */
@Data
public class SwapRequestQuery {

    private Long requesterId;
    private Long targetEmployeeId;
    private SwapRequestStatus status;
    private LocalDate createdFrom;
    private LocalDate createdTo;
}
