package com.tamabee.api_hr.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.tamabee.api_hr.entity.user.EmployeeDocumentEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.user.EmployeeDocumentRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.impl.EmployeeDocumentServiceImpl;
import com.tamabee.api_hr.service.core.interfaces.IUploadService;

/**
 * Unit tests cho Document Validation trong EmployeeDocumentService.
 * Validates: Requirements 10.4, 10.5
 */
@ExtendWith(MockitoExtension.class)
class EmployeeDocumentServiceValidationTest {

    @Mock
    private EmployeeDocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IUploadService uploadService;

    private EmployeeDocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentService = new EmployeeDocumentServiceImpl(
                documentRepository,
                userRepository,
                uploadService);
    }

    // ==================== File Type Validation Tests ====================

    @Test
    @DisplayName("uploadDocument - PDF file type is allowed")
    void uploadDocument_pdfFileType_isAllowed() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));
        when(uploadService.uploadFile(any(), anyString(), anyString())).thenReturn("http://example.com/file.pdf");
        when(documentRepository.save(any())).thenReturn(createTestDocument(1L, "document.pdf"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", new byte[1024]);

        // When/Then: Should not throw exception
        documentService.uploadDocument(employeeId, file, "CONTRACT");
    }

    @Test
    @DisplayName("uploadDocument - DOC file type is allowed")
    void uploadDocument_docFileType_isAllowed() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));
        when(uploadService.uploadFile(any(), anyString(), anyString())).thenReturn("http://example.com/file.doc");
        when(documentRepository.save(any())).thenReturn(createTestDocument(1L, "document.doc"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "document.doc", "application/msword", new byte[1024]);

        // When/Then: Should not throw exception
        documentService.uploadDocument(employeeId, file, "CONTRACT");
    }

    @Test
    @DisplayName("uploadDocument - JPEG image is allowed")
    void uploadDocument_jpegImage_isAllowed() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));
        when(uploadService.uploadFile(any(), anyString(), anyString())).thenReturn("http://example.com/file.jpg");
        when(documentRepository.save(any())).thenReturn(createTestDocument(1L, "photo.jpg"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[1024]);

        // When/Then: Should not throw exception
        documentService.uploadDocument(employeeId, file, "ID_CARD");
    }

    @Test
    @DisplayName("uploadDocument - PNG image is allowed")
    void uploadDocument_pngImage_isAllowed() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));
        when(uploadService.uploadFile(any(), anyString(), anyString())).thenReturn("http://example.com/file.png");
        when(documentRepository.save(any())).thenReturn(createTestDocument(1L, "photo.png"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", new byte[1024]);

        // When/Then: Should not throw exception
        documentService.uploadDocument(employeeId, file, "ID_CARD");
    }

    @Test
    @DisplayName("uploadDocument - Excel file is allowed")
    void uploadDocument_excelFile_isAllowed() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));
        when(uploadService.uploadFile(any(), anyString(), anyString())).thenReturn("http://example.com/file.xlsx");
        when(documentRepository.save(any())).thenReturn(createTestDocument(1L, "data.xlsx"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[1024]);

        // When/Then: Should not throw exception
        documentService.uploadDocument(employeeId, file, "CERTIFICATE");
    }

    @Test
    @DisplayName("uploadDocument - Executable file type is rejected")
    void uploadDocument_executableFileType_isRejected() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));

        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", new byte[1024]);

        // When/Then: Should throw BadRequestException
        assertThatThrownBy(() -> documentService.uploadDocument(employeeId, file, "CONTRACT"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("uploadDocument - ZIP file type is rejected")
    void uploadDocument_zipFileType_isRejected() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));

        MockMultipartFile file = new MockMultipartFile(
                "file", "archive.zip", "application/zip", new byte[1024]);

        // When/Then: Should throw BadRequestException
        assertThatThrownBy(() -> documentService.uploadDocument(employeeId, file, "CONTRACT"))
                .isInstanceOf(BadRequestException.class);
    }

    // ==================== File Size Validation Tests ====================

    @Test
    @DisplayName("uploadDocument - File under 10MB is allowed")
    void uploadDocument_fileUnder10MB_isAllowed() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));
        when(uploadService.uploadFile(any(), anyString(), anyString())).thenReturn("http://example.com/file.pdf");
        when(documentRepository.save(any())).thenReturn(createTestDocument(1L, "document.pdf"));

        // 5MB file
        byte[] content = new byte[5 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", content);

        // When/Then: Should not throw exception
        documentService.uploadDocument(employeeId, file, "CONTRACT");
    }

    @Test
    @DisplayName("uploadDocument - File over 10MB is rejected")
    void uploadDocument_fileOver10MB_isRejected() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));

        // 11MB file
        byte[] content = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "large-document.pdf", "application/pdf", content);

        // When/Then: Should throw BadRequestException
        assertThatThrownBy(() -> documentService.uploadDocument(employeeId, file, "CONTRACT"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("uploadDocument - File exactly 10MB is allowed")
    void uploadDocument_fileExactly10MB_isAllowed() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));
        when(uploadService.uploadFile(any(), anyString(), anyString())).thenReturn("http://example.com/file.pdf");
        when(documentRepository.save(any())).thenReturn(createTestDocument(1L, "document.pdf"));

        // Exactly 10MB file
        byte[] content = new byte[10 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", content);

        // When/Then: Should not throw exception
        documentService.uploadDocument(employeeId, file, "CONTRACT");
    }

    // ==================== Empty File Validation Tests ====================

    @Test
    @DisplayName("uploadDocument - Empty file is rejected")
    void uploadDocument_emptyFile_isRejected() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));

        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        // When/Then: Should throw BadRequestException
        assertThatThrownBy(() -> documentService.uploadDocument(employeeId, file, "CONTRACT"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("uploadDocument - Null file is rejected")
    void uploadDocument_nullFile_isRejected() {
        // Given
        Long employeeId = 1L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.of(createTestEmployee(employeeId)));

        // When/Then: Should throw BadRequestException
        assertThatThrownBy(() -> documentService.uploadDocument(employeeId, null, "CONTRACT"))
                .isInstanceOf(BadRequestException.class);
    }

    // ==================== Employee Validation Tests ====================

    @Test
    @DisplayName("uploadDocument - Non-existent employee returns 404")
    void uploadDocument_nonExistentEmployee_returns404() {
        // Given
        Long employeeId = 999L;
        when(userRepository.findByIdAndDeletedFalse(employeeId)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", new byte[1024]);

        // When/Then: Should throw NotFoundException
        assertThatThrownBy(() -> documentService.uploadDocument(employeeId, file, "CONTRACT"))
                .isInstanceOf(NotFoundException.class);
    }

    /**
     * Helper method tạo test employee
     */
    private UserEntity createTestEmployee(Long id) {
        UserEntity employee = new UserEntity();
        employee.setId(id);
        employee.setEmail("test@example.com");
        employee.setEmployeeCode("EMP001");
        employee.setRole(UserRole.EMPLOYEE_COMPANY);
        employee.setStatus(UserStatus.ACTIVE);
        employee.setLanguage("vi");
        employee.setLocale("vi");
        employee.setDeleted(false);
        return employee;
    }

    /**
     * Helper method tạo test document
     */
    private EmployeeDocumentEntity createTestDocument(Long id, String fileName) {
        EmployeeDocumentEntity document = new EmployeeDocumentEntity();
        document.setId(id);
        document.setEmployeeId(1L);
        document.setFileName(fileName);
        document.setFileUrl("http://example.com/" + fileName);
        document.setFileType(fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase());
        document.setFileSize(1024L);
        document.setDocumentType("CONTRACT");
        return document;
    }
}
