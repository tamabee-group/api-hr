package com.tamabee.api_hr.mapper.company;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.request.department.CreateDepartmentRequest;
import com.tamabee.api_hr.dto.request.department.UpdateDepartmentRequest;
import com.tamabee.api_hr.dto.response.department.DefaultApproverResponse;
import com.tamabee.api_hr.dto.response.department.DepartmentResponse;
import com.tamabee.api_hr.dto.response.department.DepartmentSummary;
import com.tamabee.api_hr.dto.response.department.DepartmentTreeNode;
import com.tamabee.api_hr.dto.response.department.ManagerSummary;
import com.tamabee.api_hr.entity.company.DepartmentEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;

@Component
public class DepartmentMapper {

    public DepartmentEntity toEntity(CreateDepartmentRequest request, String generatedCode) {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setName(request.getName().trim());
        entity.setCode(generatedCode);
        entity.setDescription(request.getDescription());
        return entity;
    }

    public void updateEntity(DepartmentEntity entity, UpdateDepartmentRequest request) {
        if (request.getName() != null) {
            entity.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }

    public DepartmentResponse toResponse(DepartmentEntity entity, int employeeCount) {
        return DepartmentResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .parent(toParentSummary(entity.getParent()))
                .manager(toManagerSummary(entity.getManager()))
                .employeeCount(employeeCount)
                .build();
    }

    public DepartmentTreeNode toTreeNode(DepartmentEntity entity, int employeeCount, List<DepartmentTreeNode> children) {
        return DepartmentTreeNode.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .manager(toManagerSummary(entity.getManager()))
                .employeeCount(employeeCount)
                .children(children != null ? children : new ArrayList<>())
                .build();
    }

    public DepartmentSummary toSummary(DepartmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return DepartmentSummary.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    private DepartmentSummary toParentSummary(DepartmentEntity parent) {
        if (parent == null) {
            return null;
        }
        return DepartmentSummary.builder()
                .id(parent.getId())
                .name(parent.getName())
                .build();
    }

    public ManagerSummary toManagerSummary(UserEntity manager) {
        if (manager == null) {
            return null;
        }
        UserProfileEntity profile = manager.getProfile();
        return ManagerSummary.builder()
                .id(manager.getId())
                .name(profile != null ? profile.getName() : null)
                .avatar(profile != null ? profile.getAvatar() : null)
                .build();
    }

    public DefaultApproverResponse toDefaultApproverResponse(UserEntity manager, String departmentName) {
        if (manager == null) {
            return null;
        }
        UserProfileEntity profile = manager.getProfile();
        return DefaultApproverResponse.builder()
                .id(manager.getId())
                .name(profile != null ? profile.getName() : null)
                .avatar(profile != null ? profile.getAvatar() : null)
                .departmentName(departmentName)
                .build();
    }
}
