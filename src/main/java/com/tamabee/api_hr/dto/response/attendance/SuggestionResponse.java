package com.tamabee.api_hr.dto.response.attendance;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO cho gợi ý phân ca theo ngày.
 */
@Data
public class SuggestionResponse {

    private Integer dayOfWeek;
    private LocalDate date;
    private List<ShiftSuggestion> suggestions;
}
