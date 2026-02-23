package com.tamabee.api_hr.mapper.core;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.request.user.UpdateMyProfileRequest;
import com.tamabee.api_hr.dto.response.portal.ContractResponse;
import com.tamabee.api_hr.dto.response.portal.DocumentResponse;
import com.tamabee.api_hr.dto.response.portal.MyProfileResponse;
import com.tamabee.api_hr.entity.contract.EmploymentContractEntity;
import com.tamabee.api_hr.entity.user.EmployeeDocumentEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;

/**
 * Mapper cho Employee Portal - chuyển đổi giữa entities và DTOs
 */
@Component
public class MyPortalMapper {

    // ==================== PROFILE MAPPING ====================

    /**
     * Chuyển đổi UserEntity sang MyProfileResponse
     */
    public MyProfileResponse toMyProfileResponse(UserEntity user) {
        return toMyProfileResponse(user, null);
    }

    /**
     * Chuyển đổi UserEntity sang MyProfileResponse với tên manager
     */
    public MyProfileResponse toMyProfileResponse(UserEntity user, String managerName) {
        if (user == null) {
            return null;
        }

        UserProfileEntity profile = user.getProfile();

        MyProfileResponse.MyProfileResponseBuilder builder = MyProfileResponse.builder()
                .id(user.getId())
                .employeeCode(user.getEmployeeCode())
                .email(user.getEmail())
                .profileCompletionPercentage(user.getProfileCompleteness());

        if (profile != null) {
            // Thông tin cơ bản
            builder.name(profile.getName())
                    .phone(profile.getPhone())
                    .dateOfBirth(profile.getDateOfBirth())
                    .gender(profile.getGender())
                    .address(profile.getAddress())
                    .zipCode(profile.getZipCode())
                    .avatar(profile.getAvatar());

            // Thông tin công việc (readonly)
            builder.jobTitle(profile.getJobTitle())
                    .joiningDate(profile.getJoiningDate())
                    .contractType(profile.getEmploymentType())
                    .managerName(managerName);

            // Department
            if (profile.getDepartmentEntity() != null) {
                builder.department(profile.getDepartmentEntity().getName());
            }

            // Thông tin ngân hàng
            builder.bankAccountType(profile.getBankAccountType())
                    .japanBankType(profile.getJapanBankType())
                    .bankName(profile.getBankName())
                    .bankAccount(profile.getBankAccount())
                    .bankAccountName(profile.getBankAccountName())
                    .bankCode(profile.getBankCode())
                    .bankBranchCode(profile.getBankBranchCode())
                    .bankBranchName(profile.getBankBranchName())
                    .bankAccountCategory(profile.getBankAccountCategory())
                    .bankSymbol(profile.getBankSymbol())
                    .bankNumber(profile.getBankNumber());

            // Liên hệ khẩn cấp
            builder.emergencyContactName(profile.getEmergencyContactName())
                    .emergencyContactPhone(profile.getEmergencyContactPhone())
                    .emergencyContactRelation(profile.getEmergencyContactRelation())
                    .emergencyContactAddress(profile.getEmergencyContactAddress());
        }

        return builder.build();
    }

    /**
     * Cập nhật UserProfileEntity từ UpdateMyProfileRequest
     * Chỉ cập nhật các trường được phép chỉnh sửa
     */
    public void updateUserProfile(UserProfileEntity profile, UpdateMyProfileRequest request) {
        if (profile == null || request == null) {
            return;
        }

        // Thông tin cơ bản (có thể chỉnh sửa)
        if (request.getName() != null) {
            profile.setName(request.getName());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }
        if (request.getZipCode() != null) {
            profile.setZipCode(request.getZipCode());
        }

        // Thông tin ngân hàng
        if (request.getBankAccountType() != null) {
            profile.setBankAccountType(request.getBankAccountType());
        }
        if (request.getJapanBankType() != null) {
            profile.setJapanBankType(request.getJapanBankType());
        }
        if (request.getBankName() != null) {
            profile.setBankName(request.getBankName());
        }
        if (request.getBankAccount() != null) {
            profile.setBankAccount(request.getBankAccount());
        }
        if (request.getBankAccountName() != null) {
            profile.setBankAccountName(request.getBankAccountName());
        }
        if (request.getBankCode() != null) {
            profile.setBankCode(request.getBankCode());
        }
        if (request.getBankBranchCode() != null) {
            profile.setBankBranchCode(request.getBankBranchCode());
        }
        if (request.getBankBranchName() != null) {
            profile.setBankBranchName(request.getBankBranchName());
        }
        if (request.getBankAccountCategory() != null) {
            profile.setBankAccountCategory(request.getBankAccountCategory());
        }
        if (request.getBankSymbol() != null) {
            profile.setBankSymbol(request.getBankSymbol());
        }
        if (request.getBankNumber() != null) {
            profile.setBankNumber(request.getBankNumber());
        }

        // Liên hệ khẩn cấp
        if (request.getEmergencyContactName() != null) {
            profile.setEmergencyContactName(request.getEmergencyContactName());
        }
        if (request.getEmergencyContactPhone() != null) {
            profile.setEmergencyContactPhone(request.getEmergencyContactPhone());
        }
        if (request.getEmergencyContactRelation() != null) {
            profile.setEmergencyContactRelation(request.getEmergencyContactRelation());
        }
        if (request.getEmergencyContactAddress() != null) {
            profile.setEmergencyContactAddress(request.getEmergencyContactAddress());
        }
    }

    // ==================== CONTRACT MAPPING ====================

    /**
     * Chuyển đổi EmploymentContractEntity sang ContractResponse
     */
    public ContractResponse toContractResponse(EmploymentContractEntity entity) {
        if (entity == null) {
            return null;
        }

        // Tính số ngày còn lại đến khi hết hạn
        Integer daysUntilExpiry = null;
        if (entity.getEndDate() != null) {
            LocalDate today = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
            if (entity.getEndDate().isBefore(today)) {
                daysUntilExpiry = 0;
            } else {
                daysUntilExpiry = (int) ChronoUnit.DAYS.between(today, entity.getEndDate());
            }
        }

        return ContractResponse.builder()
                .id(entity.getId())
                .contractNumber(entity.getContractNumber())
                .contractType(entity.getContractType() != null ? entity.getContractType().name() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .notes(entity.getNotes())
                .terminationDate(entity.getTerminatedAt())
                .terminationReason(entity.getTerminationReason())
                .daysUntilExpiry(daysUntilExpiry)
                .build();
    }

    // ==================== DOCUMENT MAPPING ====================

    /**
     * Chuyển đổi EmployeeDocumentEntity sang DocumentResponse
     */
    public DocumentResponse toDocumentResponse(EmployeeDocumentEntity entity) {
        if (entity == null) {
            return null;
        }

        return DocumentResponse.builder()
                .id(entity.getId())
                .documentType(entity.getDocumentType())
                .fileName(entity.getFileName())
                .fileUrl(entity.getFileUrl())
                .fileSize(entity.getFileSize())
                .mimeType(entity.getFileType())
                .uploadedAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Tạo EmployeeDocumentEntity từ thông tin upload
     */
    public EmployeeDocumentEntity toDocumentEntity(Long employeeId, MultipartFile file,
            String documentType, String fileUrl) {
        if (file == null || fileUrl == null) {
            return null;
        }

        EmployeeDocumentEntity entity = new EmployeeDocumentEntity();
        entity.setEmployeeId(employeeId);
        entity.setFileName(file.getOriginalFilename());
        entity.setFileUrl(fileUrl);
        entity.setFileType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setDocumentType(documentType);

        return entity;
    }
}
