package com.tamabee.api_hr.dto.response.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho Personal Info tab.
 * Tổ chức thông tin cá nhân theo sections.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePersonalInfoResponse {

    private BasicInfoSection basicInfo;
    private WorkInfoSection workInfo;
    private ContactInfoSection contactInfo;
    private BankDetailsSection bankDetails;
    private EmergencyContactSection emergencyContact;

    /**
     * Thông tin cơ bản: avatar, name, date of birth, gender, nationality, marital status, national ID
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicInfoSection {
        private String avatar;
        private String name;
        private String dateOfBirth;
        private String gender;
        private String nationality;
        private String maritalStatus;
        private String nationalId;
    }

    /**
     * Thông tin công việc: job title, department, direct manager, employment type, joining date, work location
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkInfoSection {
        private String jobTitle;
        private String department;
        private Long departmentId;
        private UserSummaryResponse directManager;
        private String employmentType;
        private String joiningDate;
        private String workLocation;
    }

    /**
     * Thông tin liên hệ: phone, email, address
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfoSection {
        private String phone;
        private String email;
        private String address;
        private String zipCode;
    }

    /**
     * Thông tin ngân hàng
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankDetailsSection {
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
    }

    /**
     * Liên hệ khẩn cấp
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyContactSection {
        private String name;
        private String phone;
        private String relation;
        private String address;
    }
}
