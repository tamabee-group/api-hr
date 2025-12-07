package com.tamabee.api_hr.dto.response;

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
    
    // Bank info
    private String bankName;
    private String bankAccount;
    private String bankAccountName;
    
    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String emergencyContactAddress;
}
