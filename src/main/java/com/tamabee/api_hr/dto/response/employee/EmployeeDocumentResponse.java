package com.tamabee.api_hr.dto.response.employee;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho tài liệu nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDocumentResponse {

    private Long id;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String documentType;
    private LocalDateTime createdAt;
}
