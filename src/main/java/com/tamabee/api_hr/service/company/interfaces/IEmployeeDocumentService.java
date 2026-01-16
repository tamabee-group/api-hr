package com.tamabee.api_hr.service.company.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.response.employee.EmployeeDocumentResponse;

/**
 * Service quản lý tài liệu nhân viên.
 * Hỗ trợ upload, download, delete documents.
 */
public interface IEmployeeDocumentService {

    /**
     * Lấy danh sách documents của nhân viên (phân trang)
     *
     * @param employeeId ID nhân viên
     * @param pageable   thông tin phân trang
     * @return danh sách documents
     */
    Page<EmployeeDocumentResponse> getEmployeeDocuments(Long employeeId, Pageable pageable);

    /**
     * Upload document mới cho nhân viên
     *
     * @param employeeId   ID nhân viên
     * @param file         file upload
     * @param documentType loại document (CONTRACT, ID_CARD, CERTIFICATE, etc.)
     * @return document đã upload
     */
    EmployeeDocumentResponse uploadDocument(Long employeeId, MultipartFile file, String documentType);

    /**
     * Xóa document của nhân viên
     *
     * @param employeeId ID nhân viên
     * @param documentId ID document
     */
    void deleteDocument(Long employeeId, Long documentId);
}
