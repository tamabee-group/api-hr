package com.tamabee.api_hr.dto.response.portal;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho thông tin tài liệu của nhân viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String documentType;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime uploadedAt;
}
