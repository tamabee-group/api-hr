package com.tamabee.api_hr.entity.company;

import com.tamabee.api_hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "companies", indexes = {
    @Index(name = "idx_email", columnList = "email", unique = true)
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
    
    private String referredByEmployeeCode;
}
