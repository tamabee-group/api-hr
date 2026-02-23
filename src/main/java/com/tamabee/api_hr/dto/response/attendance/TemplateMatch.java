package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalTime;

import lombok.Data;

/**
 * DTO cho kết quả khớp shift template với custom time.
 */
@Data
public class TemplateMatch {

    private ShiftTemplateResponse template;
    private LocalTime coverStart;
    private LocalTime coverEnd;
    private double overlapPercentage;
}
