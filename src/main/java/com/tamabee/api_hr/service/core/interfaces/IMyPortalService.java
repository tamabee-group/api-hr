package com.tamabee.api_hr.service.core.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.request.user.UpdateMyProfileRequest;
import com.tamabee.api_hr.dto.response.payroll.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.portal.ContractResponse;
import com.tamabee.api_hr.dto.response.portal.DocumentResponse;
import com.tamabee.api_hr.dto.response.portal.MyProfileResponse;

/**
 * Service interface cho Employee Portal - xử lý các thao tác self-service của nhân viên
 */
public interface IMyPortalService {

    // ==================== Payslip Operations ====================

    /**
     * Lấy danh sách phiếu lương của nhân viên với phân trang và filter
     * @param employeeId ID của nhân viên
     * @param year Năm cần lọc (optional)
     * @param status Trạng thái phiếu lương (optional)
     * @param pageable Thông tin phân trang
     * @return Danh sách phiếu lương phân trang
     */
    Page<PayrollItemResponse> getMyPayslips(Long employeeId, Integer year, String status, Pageable pageable);

    /**
     * Lấy chi tiết phiếu lương theo ID
     * @param employeeId ID của nhân viên
     * @param itemId ID của payroll item
     * @return Chi tiết phiếu lương
     */
    PayrollItemResponse getPayslipDetail(Long employeeId, Long itemId);

    /**
     * Tải phiếu lương dạng PDF
     * @param employeeId ID của nhân viên
     * @param itemId ID của payroll item
     * @return Nội dung file PDF dạng byte array
     */
    byte[] downloadPayslipPdf(Long employeeId, Long itemId);

    // ==================== Profile Operations ====================

    /**
     * Lấy thông tin profile của nhân viên
     * @param employeeId ID của nhân viên
     * @return Thông tin profile đầy đủ
     */
    MyProfileResponse getMyProfile(Long employeeId);

    /**
     * Cập nhật thông tin profile của nhân viên
     * @param employeeId ID của nhân viên
     * @param request Dữ liệu cập nhật
     * @return Profile sau khi cập nhật
     */
    MyProfileResponse updateMyProfile(Long employeeId, UpdateMyProfileRequest request);

    /**
     * Upload avatar cho nhân viên
     * @param employeeId ID của nhân viên
     * @param file File ảnh avatar
     * @return URL của avatar mới
     */
    String uploadAvatar(Long employeeId, MultipartFile file);

    // ==================== Contract Operations ====================

    /**
     * Lấy hợp đồng hiện tại đang active của nhân viên
     * @param employeeId ID của nhân viên
     * @return Hợp đồng hiện tại hoặc null nếu không có
     */
    ContractResponse getCurrentContract(Long employeeId);

    /**
     * Lấy lịch sử tất cả hợp đồng của nhân viên
     * @param employeeId ID của nhân viên
     * @return Danh sách hợp đồng sắp xếp theo ngày bắt đầu giảm dần
     */
    List<ContractResponse> getContractHistory(Long employeeId);

    /**
     * Lấy chi tiết hợp đồng theo ID
     * @param employeeId ID của nhân viên
     * @param contractId ID của hợp đồng
     * @return Chi tiết hợp đồng
     */
    ContractResponse getContractDetail(Long employeeId, Long contractId);

    // ==================== Document Operations ====================

    /**
     * Lấy danh sách tài liệu của nhân viên với phân trang
     * @param employeeId ID của nhân viên
     * @param pageable Thông tin phân trang
     * @return Danh sách tài liệu phân trang
     */
    Page<DocumentResponse> getMyDocuments(Long employeeId, Pageable pageable);

    /**
     * Upload tài liệu mới cho nhân viên
     * @param employeeId ID của nhân viên
     * @param file File tài liệu
     * @param documentType Loại tài liệu
     * @return Thông tin tài liệu đã upload
     */
    DocumentResponse uploadDocument(Long employeeId, MultipartFile file, String documentType);

    /**
     * Lấy chi tiết tài liệu theo ID
     * @param employeeId ID của nhân viên
     * @param documentId ID của tài liệu
     * @return Chi tiết tài liệu
     */
    DocumentResponse getDocumentDetail(Long employeeId, Long documentId);

    /**
     * Xóa tài liệu của nhân viên
     * @param employeeId ID của nhân viên
     * @param documentId ID của tài liệu
     */
    void deleteDocument(Long employeeId, Long documentId);

    /**
     * Tải tài liệu về
     * @param employeeId ID của nhân viên
     * @param documentId ID của tài liệu
     * @return Nội dung file dạng byte array
     */
    byte[] downloadDocument(Long employeeId, Long documentId);
}
