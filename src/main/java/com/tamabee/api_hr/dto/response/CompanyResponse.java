package com.tamabee.api_hr.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CompanyResponse {
    private Long id;
    private String name;
    private String ownerName;
    private String email;
    private String phone;
    private String address;
    private String industry;
    private String zipcode;
    private String locale;
    private String language;
    private String referredByEmployeeCode;
    private String referredByEmployeeName;
    private String logo;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
