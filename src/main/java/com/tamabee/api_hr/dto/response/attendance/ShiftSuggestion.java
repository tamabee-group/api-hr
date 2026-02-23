package com.tamabee.api_hr.dto.response.attendance;

import lombok.Data;

import java.util.List;

/**
 * DTO cho gợi ý phân ca của một shift template cụ thể.
 */
@Data
public class ShiftSuggestion {

    private ShiftTemplateResponse shiftTemplate;
    private List<EmployeePreferenceSummary> preferredEmployees;
    private List<EmployeePreferenceSummary> flexibleEmployees; // không có nguyện vọng
    private Integer historicalCount; // số người thường làm ca này vào thứ này
}
