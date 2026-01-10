package com.tamabee.api_hr.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Industry is required")
    private String industry;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Locale is required")
    private String locale;

    @NotBlank(message = "Language is required")
    private String language;

    @NotBlank(message = "Tenant domain is required")
    @Size(min = 3, max = 30, message = "Tenant domain must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-z0-9]([a-z0-9-]{1,28}[a-z0-9])?$|^[a-z0-9]{3}$", message = "Tenant domain must contain only lowercase letters, numbers, and hyphens, and cannot start or end with a hyphen")
    private String tenantDomain;

    private String zipcode;

    private String referralCode;
}
