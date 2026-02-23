package com.tamabee.api_hr.dto.response.portal;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho thông tin profile của nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyProfileResponse {

    // Thông tin cơ bản
    private Long id;
    private String employeeCode;
    private String email;
    private String name;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String zipCode;
    private String avatar;

    // Thông tin công việc (readonly)
    private String department;
    private String jobTitle;
    private LocalDate joiningDate;
    private String contractType;
    private String managerName;

    // Thông tin ngân hàng
    private String bankAccountType;
    private String japanBankType;
    private String bankName;
    private String bankAccount;
    private String bankAccountName;
    private String bankCode;
    private String bankBranchCode;
    private String bankBranchName;
    private String bankAccountCategory;
    private String bankSymbol;
    private String bankNumber;

    // Liên hệ khẩn cấp
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String emergencyContactAddress;

    // Tính toán
    private Integer profileCompletionPercentage;
}
