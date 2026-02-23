package com.tamabee.api_hr.dto.response.portal;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho thông tin hợp đồng lao động
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponse {

    private Long id;
    private String contractNumber;
    private String contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String notes;
    private LocalDate terminationDate;
    private String terminationReason;
    private Integer daysUntilExpiry;
}
