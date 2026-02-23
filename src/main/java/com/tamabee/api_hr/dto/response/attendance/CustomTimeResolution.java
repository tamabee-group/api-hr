package com.tamabee.api_hr.dto.response.attendance;

import lombok.Data;

import java.util.List;

/**
 * DTO cho kết quả phân giải custom time với các shift template phù hợp.
 */
@Data
public class CustomTimeResolution {

    private List<TemplateMatch> matchingTemplates;
    private List<TimeGap> uncoveredGaps;
    private boolean fullyCovered;
}
