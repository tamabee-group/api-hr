package com.tamabee.api_hr.dto.response.employee;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho cấu trúc team (manager và direct reports)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamHierarchyResponse {

    private UserSummaryResponse manager;
    private List<UserSummaryResponse> directReports;
}
