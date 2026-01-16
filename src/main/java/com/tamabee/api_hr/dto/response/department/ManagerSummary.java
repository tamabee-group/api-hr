package com.tamabee.api_hr.dto.response.department;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManagerSummary {
    private Long id;
    private String name;
    private String avatar;
}
