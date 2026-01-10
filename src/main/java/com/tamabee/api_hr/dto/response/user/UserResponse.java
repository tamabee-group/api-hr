package com.tamabee.api_hr.dto.response.user;

import java.time.LocalDateTime;

import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String employeeCode;
    private String email;
    private UserRole role;
    private UserStatus status;
    private String locale;
    private String language;
    private Long companyId;
    private String companyName;
    private String companyLogo;
    private String tenantDomain; // Tenant domain cho multi-tenant ("tamabee" cho Tamabee users)
    private Long planId; // Plan ID của company (null cho Tamabee users)
    private Integer profileCompleteness;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserProfileResponse profile;
}
