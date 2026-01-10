package com.tamabee.api_hr.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO để cập nhật thông tin công ty
 */
@Data
public class UpdateCompanyProfileRequest {
    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 255, message = "Tên công ty không được quá 255 ký tự")
    private String name;

    @NotBlank(message = "Tên chủ sở hữu không được để trống")
    @Size(max = 255, message = "Tên chủ sở hữu không được quá 255 ký tự")
    private String ownerName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 20, message = "Số điện thoại không được quá 20 ký tự")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ không được quá 500 ký tự")
    private String address;

    @NotBlank(message = "Ngành nghề không được để trống")
    private String industry;

    @Size(max = 10, message = "Mã bưu điện không được quá 10 ký tự")
    private String zipcode;

    private String logo;
}
