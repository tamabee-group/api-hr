package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.response.employee.EmployeeDocumentResponse;
import com.tamabee.api_hr.entity.user.EmployeeDocumentEntity;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.user.EmployeeDocumentRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IEmployeeDocumentService;
import com.tamabee.api_hr.service.core.interfaces.IUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Service implementation quản lý tài liệu nhân viên
 */
@Service
@RequiredArgsConstructor
public class EmployeeDocumentServiceImpl implements IEmployeeDocumentService {

    // Allowed file types
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    // Allowed file extensions
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "jpg", "jpeg", "png", "xls", "xlsx");

    // Max file size: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final EmployeeDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final IUploadService uploadService;

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDocumentResponse> getEmployeeDocuments(Long employeeId, Pageable pageable) {
        // Validate employee exists
        validateEmployeeExists(employeeId);

        Page<EmployeeDocumentEntity> documents = documentRepository.findByEmployeeId(employeeId, pageable);
        return documents.map(this::toResponse);
    }

    @Override
    @Transactional
    public EmployeeDocumentResponse uploadDocument(Long employeeId, MultipartFile file, String documentType) {
        // Validate employee exists
        validateEmployeeExists(employeeId);

        // Validate file
        validateFile(file);

        // Upload file
        String fileUrl = uploadService.uploadFile(file, "documents", employeeId.toString());

        // Get file extension
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);

        // Create document entity
        EmployeeDocumentEntity document = new EmployeeDocumentEntity();
        document.setEmployeeId(employeeId);
        document.setFileName(originalFilename);
        document.setFileUrl(fileUrl);
        document.setFileType(fileExtension.toUpperCase());
        document.setFileSize(file.getSize());
        document.setDocumentType(documentType);

        EmployeeDocumentEntity savedDocument = documentRepository.save(document);
        return toResponse(savedDocument);
    }

    @Override
    @Transactional
    public void deleteDocument(Long employeeId, Long documentId) {
        // Validate employee exists
        validateEmployeeExists(employeeId);

        // Find document
        EmployeeDocumentEntity document = documentRepository.findByIdAndEmployeeId(documentId, employeeId)
                .orElseThrow(() -> NotFoundException.document(documentId));

        // Delete physical file
        if (document.getFileUrl() != null) {
            uploadService.deleteFile(document.getFileUrl());
        }

        // Delete database record
        documentRepository.delete(document);
    }

    // ==================== Private helper methods ====================

    /**
     * Validate employee exists trong tenant hiện tại
     */
    private void validateEmployeeExists(Long employeeId) {
        if (!userRepository.findByIdAndDeletedFalse(employeeId).isPresent()) {
            throw NotFoundException.user(employeeId);
        }
    }

    /**
     * Validate file type và size
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BadRequestException.invalidFile("File is empty");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BadRequestException.fileTooLarge(MAX_FILE_SIZE);
        }

        // Validate file type by content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType)) {
            throw BadRequestException.invalidFileType(ALLOWED_EXTENSIONS.toString());
        }

        // Validate file extension
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw BadRequestException.invalidFile("Filename is required");
        }

        String extension = getFileExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw BadRequestException.invalidFileType(ALLOWED_EXTENSIONS.toString());
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Convert entity to response
     */
    private EmployeeDocumentResponse toResponse(EmployeeDocumentEntity entity) {
        return EmployeeDocumentResponse.builder()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .fileUrl(entity.getFileUrl())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .documentType(entity.getDocumentType())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
