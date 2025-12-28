package com.tamabee.api_hr.entity.user;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "users")
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {

    @Column(unique = true, length = 10)
    private String employeeCode;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private String locale;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private Long companyId = 0L;

    // Phần trăm hoàn thiện thông tin profile (0-100)
    @Column(nullable = false)
    private Integer profileCompleteness = 0;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private UserProfileEntity profile;

    /**
     * Tính toán và cập nhật phần trăm hoàn thiện profile
     * Các field được tính: name, phone, address, zipCode, dateOfBirth, gender,
     * avatar,
     * bankName, bankAccount, bankAccountName, emergencyContactName,
     * emergencyContactPhone
     */
    public void calculateProfileCompleteness() {
        if (profile == null) {
            this.profileCompleteness = 0;
            return;
        }

        int totalFields = 12;
        int filledFields = 0;

        // Thông tin cơ bản (6 fields)
        if (hasValue(profile.getName()))
            filledFields++;
        if (hasValue(profile.getPhone()))
            filledFields++;
        if (hasValue(profile.getAddress()))
            filledFields++;
        if (hasValue(profile.getZipCode()))
            filledFields++;
        if (hasValue(profile.getDateOfBirth()))
            filledFields++;
        if (hasValue(profile.getGender()))
            filledFields++;

        // Thông tin ngân hàng (3 fields)
        if (hasValue(profile.getBankName()))
            filledFields++;
        if (hasValue(profile.getBankAccount()))
            filledFields++;
        if (hasValue(profile.getBankAccountName()))
            filledFields++;

        // Liên hệ khẩn cấp (3 fields - chỉ tính name, phone, relation)
        if (hasValue(profile.getEmergencyContactName()))
            filledFields++;
        if (hasValue(profile.getEmergencyContactPhone()))
            filledFields++;
        if (hasValue(profile.getEmergencyContactRelation()))
            filledFields++;

        this.profileCompleteness = (filledFields * 100) / totalFields;
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
