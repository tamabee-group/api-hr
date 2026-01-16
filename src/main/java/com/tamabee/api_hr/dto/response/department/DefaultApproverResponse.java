package com.tamabee.api_hr.dto.response.department;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DefaultApproverResponse {
    private Long id;
    private String name;
    private String avatar;
    private String departmentName;
}
