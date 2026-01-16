package com.tamabee.api_hr.dto.response.department;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartmentTreeNode {
    private Long id;
    private String name;
    private String code;
    private String description;
    private ManagerSummary manager;
    private int employeeCount;
    private List<DepartmentTreeNode> children;
}
