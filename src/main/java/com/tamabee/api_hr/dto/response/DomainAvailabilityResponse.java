package com.tamabee.api_hr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho API check-domain availability
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainAvailabilityResponse {

    /**
     * Domain được kiểm tra
     */
    private String domain;

    /**
     * Domain có khả dụng không
     */
    private boolean available;

    /**
     * Lý do không khả dụng (nếu có)
     * Possible values: INVALID_FORMAT, RESERVED, ALREADY_EXISTS
     */
    private String reason;
}
