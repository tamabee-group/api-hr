package com.tamabee.api_hr.entity.user;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.entity.company.DepartmentEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "user_profiles")
@EqualsAndHashCode(callSuper = true, exclude = {"departmentEntity"})
public class UserProfileEntity extends BaseEntity {

    // Soft delete flag
    @Column(nullable = false)
    private Boolean deleted = false;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // Basic info
    private String name;
    private String phone;
    private String address;
    private String zipCode;
    private java.time.LocalDate dateOfBirth;
    private String gender;
    private String avatar;
    private String nationality;
    private String maritalStatus;
    private String nationalId;

    // Work info
    private String jobTitle;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentEntity departmentEntity;
    
    private String employmentType;
    private java.time.LocalDate joiningDate;
    private String workLocation;

    @Column(unique = true, length = 8)
    private String referralCode;

    // Bank info - Common
    private String bankAccountType; // VN hoặc JP
    private String japanBankType; // normal (ngân hàng thông thường) hoặc yucho (ゆうちょ銀行)
    private String bankName;
    private String bankAccount;
    private String bankAccountName;

    // Bank info - Japan specific
    private String bankCode;
    private String bankBranchCode;
    private String bankBranchName;
    private String bankAccountCategory; // 普通 (futsu) hoặc 当座 (toza)
    private String bankSymbol; // 記号 - dùng cho ゆうちょ銀行 (Japan Post Bank)
    private String bankNumber; // 番号 - dùng cho ゆうちょ銀行 (Japan Post Bank)

    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String emergencyContactAddress;
}
