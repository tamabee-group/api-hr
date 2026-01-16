package com.tamabee.api_hr.dto.request.department;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDepartmentRequest {

    @Size(max = 100, message = "DEPARTMENT_NAME_TOO_LONG")
    private String name;

    private String description;

    private Long parentId;

    private Long managerId;
}
