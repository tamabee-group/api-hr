package com.tamabee.api_hr.entity.user;

import com.tamabee.api_hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "user_profiles")
@EqualsAndHashCode(callSuper = true)
public class UserProfileEntity extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private String name;
    private String phone;
    private String address;
    private String zipCode;
    private String dateOfBirth;
    private String gender;
    private String avatar;

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
