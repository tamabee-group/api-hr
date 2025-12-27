package com.tamabee.api_hr.dto.request;

import lombok.Data;

/**
 * Request DTO để cập nhật thông tin công ty
 */
@Data
public class UpdateCompanyRequest {
    private String name;
    private String ownerName;
    private String email;
    private String phone;
    private String industry;
    private String locale;
    private String language;
    private String zipcode;
    private String address;
}
