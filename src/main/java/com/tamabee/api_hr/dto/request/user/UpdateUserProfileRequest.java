package com.tamabee.api_hr.dto.request.user;

import java.time.LocalDate;

import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.validation.MinAge;

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

    // Basic info
    @MinAge(value = 15, message = "Nhân viên phải từ 15 tuổi trở lên theo luật lao động")
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String maritalStatus;
    private String nationalId;

    // Work info
    private String jobTitle;
    private Long departmentId;
    private String employmentType;
    private LocalDate joiningDate;
    private String workLocation;

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
