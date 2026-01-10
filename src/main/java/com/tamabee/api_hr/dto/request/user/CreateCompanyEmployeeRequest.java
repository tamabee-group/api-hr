package com.tamabee.api_hr.dto.request.user;

import com.tamabee.api_hr.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO để tạo nhân viên công ty mới
 */
@Data
public class CreateCompanyEmployeeRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Tên không được để trống")
    private String name;

    private String phone;

    @NotNull(message = "Vai trò không được để trống")
    private UserRole role;

    private String address;

    private String zipCode;

    private String dateOfBirth;

    private String gender;

    @NotBlank(message = "Ngôn ngữ không được để trống")
    private String language;
}
