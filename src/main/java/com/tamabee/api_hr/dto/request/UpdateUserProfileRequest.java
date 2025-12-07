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
    private String bankName;
    private String bankAccount;
    private String bankAccountName;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String emergencyContactAddress;
}
