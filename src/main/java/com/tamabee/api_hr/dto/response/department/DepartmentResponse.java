package com.tamabee.api_hr.dto.response.department;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartmentResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private DepartmentSummary parent;
    private ManagerSummary manager;
    private int employeeCount;
}
