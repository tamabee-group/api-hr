package com.tamabee.api_hr.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO cho mẫu ca làm việc.
 */
@Data
public class ShiftTemplateResponse {

    private Long id;
    private Long companyId;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakMinutes;
    private BigDecimal multiplier;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
