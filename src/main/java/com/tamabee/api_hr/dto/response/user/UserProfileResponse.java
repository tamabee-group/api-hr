package com.tamabee.api_hr.dto.response.user;

import lombok.Data;

@Data
public class UserProfileResponse {
    private String name;
    private String phone;
    private String address;
    private String zipCode;
    private String dateOfBirth;
    private String gender;
    private String avatar;
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
