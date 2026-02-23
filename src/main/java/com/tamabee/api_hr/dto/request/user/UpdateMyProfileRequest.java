package com.tamabee.api_hr.dto.request.user;

import java.time.LocalDate;

import com.tamabee.api_hr.validation.MinAge;

import lombok.Data;

/**
 * DTO cho employee tự cập nhật thông tin cá nhân qua Employee Portal
 * Chỉ bao gồm các trường mà employee được phép chỉnh sửa
 */
@Data
public class UpdateMyProfileRequest {
    // Thông tin cơ bản (có thể chỉnh sửa)
    private String name;
    private String phone;

    @MinAge(value = 15, message = "Nhân viên phải từ 15 tuổi trở lên theo luật lao động")
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String zipCode;

    // Thông tin ngân hàng
    private String bankAccountType;      // VN hoặc JP
    private String japanBankType;        // normal (ngân hàng thông thường) hoặc yucho (ゆうちょ銀行)
    private String bankName;
    private String bankAccount;
    private String bankAccountName;
    private String bankCode;             // Mã ngân hàng - Japan normal bank
    private String bankBranchCode;       // Mã chi nhánh - Japan normal bank
    private String bankBranchName;       // Tên chi nhánh - Japan normal bank
    private String bankAccountCategory;  // 普通 (futsu) hoặc 当座 (toza)
    private String bankSymbol;           // 記号 - dùng cho ゆうちょ銀行 (Japan Post Bank)
    private String bankNumber;           // 番号 - dùng cho ゆうちょ銀行 (Japan Post Bank)

    // Thông tin liên hệ khẩn cấp
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String emergencyContactAddress;
}
