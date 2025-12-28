package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.UserStatus;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String name;

    @Email
    private String email;

    private String phone;
    private String language;
    private UserStatus status;
    private String zipCode;
    private String address;

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

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String emergencyContactAddress;
}
