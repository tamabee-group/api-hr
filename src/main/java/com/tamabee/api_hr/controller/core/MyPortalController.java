package com.tamabee.api_hr.controller.core;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.request.user.UpdateMyProfileRequest;
import com.tamabee.api_hr.dto.response.payroll.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.portal.ContractResponse;
import com.tamabee.api_hr.dto.response.portal.DocumentResponse;
import com.tamabee.api_hr.dto.response.portal.MyProfileResponse;
import com.tamabee.api_hr.service.core.interfaces.IAuthService;
import com.tamabee.api_hr.service.core.interfaces.IMyPortalService;
import com.tamabee.api_hr.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller cho Employee Portal - xử lý các API self-service của nhân viên
 * Tất cả endpoint yêu cầu authentication
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyPortalController {

    private final IMyPortalService myPortalService;
    private final SecurityUtil securityUtil;
    private final IAuthService authService;

    /**
     * Lấy ID của user hiện tại từ SecurityContext
     */
    private Long getCurrentEmployeeId() {
        return securityUtil.getCurrentUserId();
    }

    // ==================== Payslip Endpoints ====================

    /**
     * Lấy danh sách phiếu lương của nhân viên với phân trang và filter
     */
    @GetMapping("/payslips")
    public ResponseEntity<BaseResponse<Page<PayrollItemResponse>>> getMyPayslips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status) {
        Long employeeId = getCurrentEmployeeId();
        Pageable pageable = PageRequest.of(page, size);
        Page<PayrollItemResponse> payslips = myPortalService.getMyPayslips(employeeId, year, status, pageable);
        return ResponseEntity.ok(BaseResponse.success(payslips, "Lấy danh sách phiếu lương thành công"));
    }

    /**
     * Lấy chi tiết phiếu lương theo ID
     */
    @GetMapping("/payslips/{itemId}")
    public ResponseEntity<BaseResponse<PayrollItemResponse>> getPayslipDetail(
            @PathVariable Long itemId) {
        Long employeeId = getCurrentEmployeeId();
        PayrollItemResponse detail = myPortalService.getPayslipDetail(employeeId, itemId);
        return ResponseEntity.ok(BaseResponse.success(detail, "Lấy chi tiết phiếu lương thành công"));
    }

    /**
     * Tải phiếu lương dạng PDF
     */
    @GetMapping("/payslips/{itemId}/pdf")
    public ResponseEntity<byte[]> downloadPayslipPdf(@PathVariable Long itemId) {
        Long employeeId = getCurrentEmployeeId();
        byte[] pdfContent = myPortalService.downloadPayslipPdf(employeeId, itemId);
        
        // Lấy thông tin để tạo filename
        String employeeCode = securityUtil.getCurrentUserEmployeeCode();
        String language = securityUtil.getCurrentUserLanguage();
        String filename = generatePayslipFilename(employeeCode, language);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    // ==================== Profile Endpoints ====================

    /**
     * Lấy thông tin profile của nhân viên
     */
    @GetMapping("/profile")
    public ResponseEntity<BaseResponse<MyProfileResponse>> getMyProfile() {
        Long employeeId = getCurrentEmployeeId();
        MyProfileResponse profile = myPortalService.getMyProfile(employeeId);
        return ResponseEntity.ok(BaseResponse.success(profile, "Lấy thông tin profile thành công"));
    }

    /**
     * Cập nhật thông tin profile của nhân viên
     */
    @PutMapping("/profile")
    public ResponseEntity<BaseResponse<MyProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateMyProfileRequest request) {
        Long employeeId = getCurrentEmployeeId();
        MyProfileResponse updatedProfile = myPortalService.updateMyProfile(employeeId, request);
        return ResponseEntity.ok(BaseResponse.success(updatedProfile, "Cập nhật profile thành công"));
    }

    /**
     * Upload avatar cho nhân viên
     */
    @PostMapping("/profile/avatar")
    public ResponseEntity<BaseResponse<Map<String, String>>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        Long employeeId = getCurrentEmployeeId();
        String avatarUrl = myPortalService.uploadAvatar(employeeId, file);
        return ResponseEntity.ok(BaseResponse.success(
                Map.of("avatarUrl", avatarUrl), 
                "Upload avatar thành công"));
    }

    // ==================== Contract Endpoints ====================

    /**
     * Đổi mật khẩu cho user đang đăng nhập
     * PUT /api/users/me/password
     */
    @PutMapping("/password")
    public ResponseEntity<BaseResponse<Void>> changePassword(
            @Valid @RequestBody com.tamabee.api_hr.dto.auth.ChangePasswordRequest request) {
        String email = securityUtil.getCurrentUserEmail();
        authService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(BaseResponse.success(null, "Đổi mật khẩu thành công"));
    }

    // ==================== Contract Endpoints ====================

    /**
     * Lấy hợp đồng hiện tại đang active của nhân viên
     */
    @GetMapping("/contracts/current")
    public ResponseEntity<BaseResponse<ContractResponse>> getCurrentContract() {
        Long employeeId = getCurrentEmployeeId();
        ContractResponse contract = myPortalService.getCurrentContract(employeeId);
        return ResponseEntity.ok(BaseResponse.success(contract, "Lấy hợp đồng hiện tại thành công"));
    }

    /**
     * Lấy lịch sử tất cả hợp đồng của nhân viên
     */
    @GetMapping("/contracts/history")
    public ResponseEntity<BaseResponse<List<ContractResponse>>> getContractHistory() {
        Long employeeId = getCurrentEmployeeId();
        List<ContractResponse> contracts = myPortalService.getContractHistory(employeeId);
        return ResponseEntity.ok(BaseResponse.success(contracts, "Lấy lịch sử hợp đồng thành công"));
    }

    /**
     * Lấy chi tiết hợp đồng theo ID
     */
    @GetMapping("/contracts/{contractId}")
    public ResponseEntity<BaseResponse<ContractResponse>> getContractDetail(
            @PathVariable Long contractId) {
        Long employeeId = getCurrentEmployeeId();
        ContractResponse contract = myPortalService.getContractDetail(employeeId, contractId);
        return ResponseEntity.ok(BaseResponse.success(contract, "Lấy chi tiết hợp đồng thành công"));
    }

    // ==================== Document Endpoints ====================

    /**
     * Lấy danh sách tài liệu của nhân viên với phân trang
     */
    @GetMapping("/documents")
    public ResponseEntity<BaseResponse<Page<DocumentResponse>>> getMyDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long employeeId = getCurrentEmployeeId();
        Pageable pageable = PageRequest.of(page, size);
        Page<DocumentResponse> documents = myPortalService.getMyDocuments(employeeId, pageable);
        return ResponseEntity.ok(BaseResponse.success(documents, "Lấy danh sách tài liệu thành công"));
    }

    /**
     * Upload tài liệu mới cho nhân viên
     */
    @PostMapping("/documents")
    public ResponseEntity<BaseResponse<DocumentResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) {
        Long employeeId = getCurrentEmployeeId();
        DocumentResponse document = myPortalService.uploadDocument(employeeId, file, documentType);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(document, "Upload tài liệu thành công"));
    }

    /**
     * Lấy chi tiết tài liệu theo ID
     */
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<BaseResponse<DocumentResponse>> getDocumentDetail(
            @PathVariable Long documentId) {
        Long employeeId = getCurrentEmployeeId();
        DocumentResponse document = myPortalService.getDocumentDetail(employeeId, documentId);
        return ResponseEntity.ok(BaseResponse.success(document, "Lấy chi tiết tài liệu thành công"));
    }

    /**
     * Xóa tài liệu của nhân viên
     */
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<BaseResponse<Void>> deleteDocument(@PathVariable Long documentId) {
        Long employeeId = getCurrentEmployeeId();
        myPortalService.deleteDocument(employeeId, documentId);
        return ResponseEntity.ok(BaseResponse.success(null, "Xóa tài liệu thành công"));
    }

    /**
     * Tải tài liệu về
     */
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        Long employeeId = getCurrentEmployeeId();
        byte[] fileContent = myPortalService.downloadDocument(employeeId, documentId);
        
        // Lấy thông tin document để set headers
        DocumentResponse document = myPortalService.getDocumentDetail(employeeId, documentId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.getMimeType()));
        headers.setContentDispositionFormData("attachment", document.getFileName());
        
        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }

    // ==================== Helper Methods ====================

    /**
     * Tạo tên file PDF phiếu lương theo ngôn ngữ của user
     */
    private String generatePayslipFilename(String employeeCode, String language) {
        String prefix;
        if ("ja".equals(language)) {
            prefix = "給与明細";
        } else if ("en".equals(language)) {
            prefix = "payslip";
        } else {
            prefix = "phieu_luong";
        }
        return prefix + "_" + employeeCode + ".pdf";
    }
}
