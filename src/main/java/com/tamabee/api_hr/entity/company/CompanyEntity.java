package com.tamabee.api_hr.entity.company;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.CompanyStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "companies", indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_companies_plan_id", columnList = "planId"),
        @Index(name = "idx_companies_deleted", columnList = "deleted"),
        @Index(name = "idx_companies_status", columnList = "status")
})
public class CompanyEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ownerName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String industry;

    private String zipcode;

    @Column(nullable = false)
    private String locale;

    @Column(nullable = false)
    private String language;

    // Gói dịch vụ subscription
    @Column(name = "plan_id")
    private Long planId;

    // Nhân viên tư vấn (giới thiệu công ty)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by_employee_id")
    private UserEntity referredByEmployee;

    // Logo công ty
    private String logo;

    // Chủ sở hữu công ty (user đăng ký công ty)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;

    // Trạng thái công ty (ACTIVE, INACTIVE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status = CompanyStatus.ACTIVE;
}
