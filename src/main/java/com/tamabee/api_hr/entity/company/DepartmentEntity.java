package com.tamabee.api_hr.entity.company;

import java.util.ArrayList;
import java.util.List;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.entity.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity đại diện cho phòng ban trong công ty.
 * Hỗ trợ cấu trúc phân cấp (parent-child) và soft delete.
 */
@Data
@Entity
@Table(name = "departments")
@EqualsAndHashCode(callSuper = true, exclude = {"parent", "children", "manager"})
public class DepartmentEntity extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Self-reference cho cấu trúc phân cấp
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DepartmentEntity parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<DepartmentEntity> children = new ArrayList<>();

    // Người quản lý phòng ban
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private UserEntity manager;

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    private String createdBy;
    private String updatedBy;
}
