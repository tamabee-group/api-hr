package com.tamabee.api_hr.dto.response;

import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

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
    private Integer profileCompleteness;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserProfileResponse profile;
}
